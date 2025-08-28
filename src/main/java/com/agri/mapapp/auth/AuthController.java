// src/main/java/com/agri/mapapp/auth/AuthController.java
package com.agri.mapapp.auth;

import com.agri.mapapp.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SessionService sessions;
    private final OnlineUserTracker online;
    private final AppUserRepository userRepo;

    @Getter @Setter
    public static class LoginReq {
        @NotBlank private String username;
        @NotBlank private String password;
        private String deviceId;
    }

    @Getter @Setter
    public static class RefreshReq {
        @NotBlank private String refreshToken;
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
        private Long orgId;     // OrganizationUnit.id
        private String orgName; // OrganizationUnit.name
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

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@RequestBody LoginReq body, HttpServletRequest req) {
        String deviceId = headerDeviceId(req, body.getDeviceId());
        String ip = ipOf(req);
        String ua = uaOf(req);
        TokenPair pair = sessions.loginIssueTokens(body.getUsername(), body.getPassword(), deviceId, ua, ip);
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshReq body, HttpServletRequest req) {
        String deviceId = headerDeviceId(req, body.getDeviceId());
        String ip = ipOf(req);
        String ua = uaOf(req);
        TokenPair pair = sessions.rotate(body.getRefreshToken(), deviceId, ua, ip);
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody RefreshReq body) {
        sessions.logout(body.getRefreshToken());
        return ResponseEntity.ok(Map.of("ok", true));
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
                                                         @RequestBody HeartbeatReq body) {
        if (principal == null) return ResponseEntity.status(401).build();
        AppUser u = userRepo.findByUsername(principal.getUsername()).orElseThrow();
        sessions.heartbeat(u.getId(), body.getDeviceId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * ✅ Paged sessions API
     * GET /api/auth/sessions?includeRevoked=true&includeExpired=false&page=0&size=10&sort=lastSeenAt,desc
     */
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
                        tokenSuffix(rt.getToken())
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
        // format: "field,dir" yoki "field" (default: lastSeenAt desc)
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("lastSeenAt"));
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "desc";
        if (dir.startsWith("asc")) return Sort.by(Sort.Order.asc(field));
        return Sort.by(Sort.Order.desc(field));
    }
}
