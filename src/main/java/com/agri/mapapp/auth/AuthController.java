package com.agri.mapapp.auth;

import com.agri.mapapp.common.PageResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final SessionService sessions;
    private final OnlineUserTracker online;
    private final AppUserRepository userRepo;

    /** Cookie nomi (default: refreshToken). Kerak bo‘lsa prod’da o‘zgartirasiz. */
    @Value("${app.security.cookie.name:refreshToken}")
    private String refreshCookieName;

    /** Cookie domain; bo‘sh bo‘lsa host-only */
    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    @Getter @Setter
    public static class LoginReq {
        @NotBlank private String username;
        @NotBlank private String password;
        private String deviceId;
    }

    @Getter @Setter
    public static class RefreshReq {
        private String refreshToken; // @NotBlank olib tashlandi: body optional
        private String deviceId;
    }

    @Getter @Setter
    public static class HeartbeatReq {
        @NotBlank private String deviceId;
    }

    @Getter @Setter @AllArgsConstructor
    public static class SessionView {
        private String deviceId;
        private String ip;
        private String userAgent;
        private Instant createdAt;
        private Instant lastSeenAt;
        private Instant expiresAt;
        private boolean revoked;
        private String tokenSuffix;
    }

    @Getter @Setter @AllArgsConstructor
    public static class MeRes {
        private Long id;
        private String username;
        private Role role;
        private Long orgId;
        private String orgName;
    }

    @Getter @Setter @AllArgsConstructor
    public static class AccessTokenResponse {
        private String tokenType; // "Bearer"
        private String accessToken;
        private Instant accessExpiresAt;
        private MeRes user;
    }

    private String headerDeviceId(HttpServletRequest req, String fallback) {
        String v = req.getHeader("X-Device-Id");
        return (v != null && !v.isBlank()) ? v : fallback;
    }
    private String ipOf(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        return (h != null && !h.isBlank()) ? h.split(",")[0].trim() : req.getRemoteAddr();
    }
    private String uaOf(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        return ua == null ? "" : ua;
    }
    private String tokenSuffix(String token) {
        if (token == null) return "";
        int n = token.length();
        return n <= 8 ? token : token.substring(n - 8);
    }

    /** Bir nechta nom bo‘yicha cookie qidiramiz (migratsiya/fallback uchun) */
    private String cookie(HttpServletRequest req, String... names) {
        Cookie[] cs = req.getCookies();
        if (cs == null) return null;
        for (String name : names) {
            for (Cookie c : cs) {
                if (name.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSeconds);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            b.domain(cookieDomain);
        }
        return b.build();
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@RequestBody LoginReq body, HttpServletRequest req) {
        String deviceId = headerDeviceId(req, body.getDeviceId());
        String ip = ipOf(req);
        String ua = uaOf(req);
        var tokens = sessions.loginIssueTokens(body.getUsername(), body.getPassword(), deviceId, ua, ip);

        // Build refresh cookie
        ResponseCookie rc = buildRefreshCookie(tokens.refreshToken(), sessions.getRefreshExpSeconds());

        AppUser u = tokens.user();
        Long orgId = (u.getOrgUnit() != null ? u.getOrgUnit().getId() : null);
        String orgName = (u.getOrgUnit() != null ? u.getOrgUnit().getName() : null);
        MeRes me = new MeRes(u.getId(), u.getUsername(), u.getRole(), orgId, orgName);
        AccessTokenResponse res = new AccessTokenResponse("Bearer", tokens.accessToken(), tokens.accessExpiresAt(), me);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rc.toString())
                .body(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest req,
                                                       @RequestBody(required = false) RefreshReq body,
                                                       @RequestParam(value = "refreshToken", required = false) String rtParam) {
        String deviceId = headerDeviceId(req, null);
        String ip = ipOf(req);
        String ua = uaOf(req);

        // 1) Cookie — hozirgi nom + muqobil nomlar
        String rt = cookie(req, refreshCookieName, "refresh", "rt", "refreshToken");

        // 2) Authorization: Bearer <token> yoki Refresh <token>
        if (rt == null || rt.isBlank()) {
            String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && !auth.isBlank()) {
                if (auth.startsWith("Bearer ")) {
                    rt = auth.substring(7).trim();
                } else if (auth.startsWith("Refresh ")) {
                    rt = auth.substring(8).trim();
                }
            }
        }

        // 3) Body
        if ((rt == null || rt.isBlank()) && body != null && body.getRefreshToken() != null) {
            rt = body.getRefreshToken().trim();
        }

        // 4) Query param
        if ((rt == null || rt.isBlank()) && rtParam != null) {
            rt = rtParam.trim();
        }

        if (rt == null || rt.isBlank()) {
            throw new AuthException("REFRESH_INVALID", "Refresh cookie missing");
        }

        var tokens = sessions.rotate(rt, deviceId, ua, ip);

        ResponseCookie rc = buildRefreshCookie(tokens.refreshToken(), sessions.getRefreshExpSeconds());

        AppUser u = tokens.user();
        Long orgId = (u.getOrgUnit() != null ? u.getOrgUnit().getId() : null);
        String orgName = (u.getOrgUnit() != null ? u.getOrgUnit().getName() : null);
        MeRes me = new MeRes(u.getId(), u.getUsername(), u.getRole(), orgId, orgName);
        AccessTokenResponse res = new AccessTokenResponse("Bearer", tokens.accessToken(), tokens.accessExpiresAt(), me);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rc.toString())
                .body(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest req) {
        String rt = cookie(req, refreshCookieName, "refresh", "rt", "refreshToken");
        if (rt != null && !rt.isBlank()) sessions.logout(rt);
        ResponseCookie rc = buildRefreshCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rc.toString())
                .body(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<MeRes> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        AppUser u = userRepo.findByUsername(principal.getUsername()).orElseThrow();
        Long orgId = (u.getOrgUnit() != null ? u.getOrgUnit().getId() : null);
        String orgName = (u.getOrgUnit() != null ? u.getOrgUnit().getName() : null);
        return ResponseEntity.ok(new MeRes(u.getId(), u.getUsername(), u.getRole(), orgId, orgName));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestBody Map<String, String> body) {
        if (principal == null) return ResponseEntity.status(401).build();
        String deviceId = body != null ? body.get("deviceId") : null;
        AppUser u = userRepo.findByUsername(principal.getUsername()).orElseThrow();
        sessions.heartbeat(u.getId(), deviceId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** ✅ Paged sessions API */
    @GetMapping("/sessions")
    public ResponseEntity<PageResponse<SessionView>> mySessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "includeRevoked", defaultValue = "false") boolean includeRevoked,
            @RequestParam(name = "includeExpired", defaultValue = "false") boolean includeExpired,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "lastSeenAt,desc") String sort
    ) {
        Long uid = userId;
        if (uid == null) {
            if (principal == null) return ResponseEntity.status(401).build();
            AppUser u = userRepo.findByUsername(principal.getUsername()).orElseThrow();
            uid = u.getId();
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<RefreshToken> pg = sessions.listSessionsPage(uid, includeRevoked, includeExpired, pageable);

        List<SessionView> content = pg.getContent().stream()
                .map(rt -> new SessionView(
                        rt.getDeviceId(),
                        rt.getIp(),
                        rt.getUserAgent(),
                        rt.getCreatedAt(),
                        rt.getLastSeenAt(),
                        rt.getExpiresAt(),
                        rt.isRevoked(),
                        tokenSuffix(rt.getTokenHash() != null ? rt.getTokenHash() : rt.getToken())
                ))
                .toList();

        PageResponse<SessionView> resp = new PageResponse<>(
                content,
                pg.getNumber(),
                pg.getSize(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.isLast()
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/sessions/revoke-others")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> revokeOthers(@RequestParam("userId") Long userId,
                                                            @RequestParam("keepDeviceId") String keepDeviceId) {
        sessions.revokeAllOtherDevices(userId, keepDeviceId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/sessions/revoke")
    public ResponseEntity<Map<String, Object>> revokeDevice(@RequestParam("userId") Long userId,
                                                            @RequestParam("deviceId") String deviceId) {
        sessions.revokeDevice(userId, deviceId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/online-count")
    public ResponseEntity<Map<String, Object>> onlineCount() {
        int n = online.getOnlineCount();
        return ResponseEntity.ok(Map.of("online", n));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("lastSeenAt"));
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";
        if (dir.startsWith("asc")) return Sort.by(Sort.Order.asc(field));
        return Sort.by(Sort.Order.desc(field));
    }
}
