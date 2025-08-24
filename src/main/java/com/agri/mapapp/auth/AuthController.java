package com.agri.mapapp.auth;

import com.agri.mapapp.org.OrganizationUnit;
import com.agri.mapapp.org.OrganizationUnitRepository;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final AppUserRepository userRepo;
    private final OrganizationUnitRepository orgRepo;
    private final PasswordEncoder passwordEncoder;

    private final SessionService sessionService;
    private final RateLimiter rateLimitService; // <-- interfeys (Redis yoki InMemory)

    // -------- Helpers --------
    private String resolveDeviceId(String bodyDeviceId, HttpServletRequest http) {
        if (bodyDeviceId != null && !bodyDeviceId.isBlank()) return bodyDeviceId;
        String h1 = http.getHeader("X-Device-Id");
        if (h1 != null && !h1.isBlank()) return h1;
        String h2 = http.getHeader("X-Device-ID");
        if (h2 != null && !h2.isBlank()) return h2;
        return null;
    }

    private String clientIp(HttpServletRequest http) {
        String xf = http.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            int comma = xf.indexOf(',');
            return comma > 0 ? xf.substring(0, comma).trim() : xf.trim();
        }
        String rip = http.getHeader("X-Real-IP");
        if (rip != null && !rip.isBlank()) return rip.trim();
        return http.getRemoteAddr();
    }

    // -------- AUTH --------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req, HttpServletRequest http) {
        String ip = clientIp(http);
        if (!rateLimitService.tryConsumeLogin(ip)) {
            return ResponseEntity.status(429).body(Map.of("message", "Too Many Requests"));
        }
        String ua = http.getHeader("User-Agent");
        String deviceId = resolveDeviceId(req.getDeviceId(), http);
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "deviceId is required"));
        }
        var pair = sessionService.loginIssueTokens(req.getUsername(), req.getPassword(), deviceId, ua, ip);
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshReq req, HttpServletRequest http) {
        String ip = clientIp(http);
        if (!rateLimitService.tryConsumeRefresh(ip)) {
            return ResponseEntity.status(429).body(Map.of("message", "Too Many Requests"));
        }
        String ua = http.getHeader("User-Agent");
        String deviceId = resolveDeviceId(req.getDeviceId(), http);
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "deviceId is required"));
        }
        var pair = sessionService.rotate(req.getRefreshToken(), deviceId, ua, ip);
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshReq req) {
        sessionService.logout(req.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String token = auth.substring(7);
        if (!jwtService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String username = jwtService.getUsername(token);
        var user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        Long orgId = user.getOrg() != null ? user.getOrg().getId() : null;
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "orgId", orgId
        ));
    }

    // -------- HEARTBEAT --------

    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(@RequestBody HeartbeatReq req, @RequestHeader("Authorization") String auth,
                                       HttpServletRequest http) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String token = auth.substring(7);
        if (!jwtService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String username = jwtService.getUsername(token);
        var user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String deviceId = resolveDeviceId(req.getDeviceId(), http);
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "deviceId is required"));
        }

        sessionService.heartbeat(user.getId(), deviceId);
        return ResponseEntity.ok(Map.of("ok", true, "ts", Instant.now().toString()));
    }

    @GetMapping("/online-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> onlineCount() {
        int n = sessionService.onlineCount();
        return ResponseEntity.ok(Map.of("online", n));
    }

    // -------- ADMIN --------

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserReq req) {
        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists"));
        }
        OrganizationUnit org = null;
        if (req.getOrgId() != null) {
            org = orgRepo.findById(req.getOrgId())
                    .orElseThrow(() -> new IllegalArgumentException("Org not found: " + req.getOrgId()));
        }
        AppUser user = AppUser.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .org(org)
                .build();
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "orgId", org != null ? org.getId() : null
        ));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SessionRes> listSessions() {
        return sessionService.listActiveSessions().stream()
                .map(rt -> new SessionRes(
                        rt.getId(),
                        rt.getUser().getId(),
                        rt.getUser().getUsername(),
                        rt.getDeviceId(),
                        rt.getUserAgent(),
                        rt.getIp(),
                        rt.getCreatedAt(),
                        rt.getLastSeenAt(),
                        rt.getExpiresAt()
                ))
                .toList();
    }

    @PostMapping("/sessions/revoke/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeAllForUser(@PathVariable Long userId) {
        sessionService.revokeAllForUser(userId);
        return ResponseEntity.ok(Map.of("message", "All sessions revoked for user " + userId));
    }

    // -------- DTOs --------

    @Getter @Setter
    public static class LoginReq {
        private String username;
        private String password;
        private String deviceId; // Body bo'sh bo'lsa headerdan olinadi
    }

    @Getter @AllArgsConstructor
    public static class TokenPair {
        private String tokenType;     // Bearer
        private String accessToken;
        private String refreshToken;
        private Instant accessTokenExpiresAt; // info uchun
    }

    @Getter @Setter
    public static class RefreshReq {
        private String refreshToken;
        private String deviceId; // Body bo'sh bo'lsa headerdan olinadi
    }

    @Getter @Setter
    public static class HeartbeatReq {
        private String deviceId; // Body bo'sh bo'lsa headerdan olinadi
    }

    @Getter
    @AllArgsConstructor
    public static class SessionRes {
        private Long id;
        private Long userId;
        private String username;
        private String deviceId;
        private String userAgent;
        private String ip;
        private Instant createdAt;
        private Instant lastSeenAt;
        private Instant expiresAt;
    }

    @Getter @Setter
    public static class CreateUserReq {
        private String username;
        private String password;
        private Role role;      // ADMIN yoki ORG_USER
        private Long orgId;     // ixtiyoriy (ORG_USER uchun)
    }
}
