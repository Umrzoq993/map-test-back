// src/main/java/com/agri/mapapp/auth/SessionService.java
package com.agri.mapapp.auth;

import com.agri.mapapp.audit.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final AuthenticationManager authManager;
    private final AppUserRepository userRepo;
    private final RefreshTokenRepository rtRepo;
    private final JwtService jwtService;
    private final OnlineUserTracker online;
    private final AuditService audit;

    @Value("${app.jwt.refresh-exp-seconds:86400}")
    private long refreshExpSeconds;

    @Value("${app.security.refresh.rotate:true}")
    private boolean rotateOnRefresh;

    @Value("${app.security.refresh.ip-binding.enabled:true}")
    private boolean ipBindingEnabled;

    @Value("${app.security.refresh.ua-binding.enabled:false}")
    private boolean uaBindingEnabled;

    @Value("${app.security.refresh.ua-binding.mode:lenient}")
    private String uaBindingMode;

    @Value("${app.security.sessions.single-device.enabled:false}")
    private boolean singleDeviceEnabled;

    @Value("${app.security.sessions.single-device.policy:REVOKE_OLD}")
    private String singleDevicePolicy;

    @Transactional
    public TokenPair loginIssueTokens(String username, String password,
                                      String deviceId, String userAgent, String ip) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        AppUser user = userRepo.findByUsername(up.getUsername()).orElseThrow();

        // Status tekshiruvi
        if (user.getStatus() != UserStatus.ACTIVE) {
            audit.log("SESSION_REJECTED", user, deviceId, ip, userAgent);
            throw new BadCredentialsException("User is not active");
        }

        // Single-device siyosati
        if (singleDeviceEnabled) {
            List<RefreshToken> actives = rtRepo.findAllByUser_IdAndRevokedFalse(user.getId()).stream()
                    .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()))
                    .toList();
            if (!actives.isEmpty()) {
                if ("REJECT_NEW".equalsIgnoreCase(singleDevicePolicy)) {
                    audit.log("SESSION_REJECTED", user, deviceId, ip, userAgent);
                    throw new BadCredentialsException("Active session exists (single-device policy)");
                } else {
                    rtRepo.revokeAllByUserId(user.getId());
                }
            }
        }

        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .ip(ip)
                .expiresAt(Instant.now().plus(refreshExpSeconds, ChronoUnit.SECONDS))
                .revoked(false)
                .build();
        rtRepo.save(rt);

        Long orgId = user.getOrgUnit() != null ? user.getOrgUnit().getId() : null;
        String access = jwtService.generateAccessToken(user.getUsername(), user.getRole(), orgId);

        online.ping(user.getId(), deviceId);
        rtRepo.touchLastSeen(user.getId(), deviceId, Instant.now());

        audit.log("LOGIN_SUCCESS", user, deviceId, ip, userAgent);

        return new TokenPair("Bearer", access, rt.getToken(),
                Instant.now().plusMillis(jwtService.getAccessExpMs()));
    }

    @Transactional
    public TokenPair rotate(String refreshToken, String deviceId, String userAgent, String ip) {
        RefreshToken old = rtRepo.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (old.getExpiresAt().isBefore(Instant.now())) {
            old.setRevoked(true);
            rtRepo.save(old);
            throw new BadCredentialsException("Refresh token expired");
        }

        // Device binding
        if (old.getDeviceId() != null && deviceId != null && !old.getDeviceId().equals(deviceId)) {
            old.setRevoked(true);
            rtRepo.save(old);
            audit.log("SESSION_REJECTED", old.getUser(), deviceId, ip, userAgent);
            throw new BadCredentialsException("Refresh token device mismatch");
        }

        // UA binding
        if (uaBindingEnabled && !uaMatches(old.getUserAgent(), userAgent)) {
            old.setRevoked(true);
            rtRepo.save(old);
            audit.log("SESSION_REJECTED", old.getUser(), deviceId, ip, userAgent);
            throw new BadCredentialsException("User-Agent mismatch for refresh token");
        }

        // IP binding
        if (ipBindingEnabled) {
            String savedIp = old.getIp();
            if (savedIp == null || !savedIp.equals(ip)) {
                old.setRevoked(true);
                rtRepo.save(old);
                audit.log("SESSION_REJECTED", old.getUser(), deviceId, ip, userAgent);
                throw new BadCredentialsException("IP mismatch for refresh token");
            }
        }

        AppUser user = old.getUser();
        Long orgId = user.getOrgUnit() != null ? user.getOrgUnit().getId() : null;
        String newAccess = jwtService.generateAccessToken(user.getUsername(), user.getRole(), orgId);

        if (rotateOnRefresh) {
            // Rotation
            old.setRevoked(true);
            String newRtStr = UUID.randomUUID().toString();
            old.setReplacedByToken(newRtStr);
            rtRepo.save(old);

            RefreshToken nu = RefreshToken.builder()
                    .token(newRtStr)
                    .user(user)
                    .deviceId(deviceId)
                    .userAgent(userAgent)
                    .ip(ip)
                    .expiresAt(Instant.now().plus(refreshExpSeconds, ChronoUnit.SECONDS))
                    .revoked(false)
                    .build();
            rtRepo.save(nu);

            online.ping(user.getId(), deviceId);
            rtRepo.touchLastSeen(user.getId(), deviceId, Instant.now());
            audit.log("REFRESH_ROTATE", user, deviceId, ip, userAgent);

            return new TokenPair("Bearer", newAccess, nu.getToken(),
                    Instant.now().plusMillis(jwtService.getAccessExpMs()));
        } else {
            // Reuse
            online.ping(user.getId(), deviceId);
            rtRepo.touchLastSeen(user.getId(), deviceId, Instant.now());
            audit.log("REFRESH_REUSE", user, deviceId, ip, userAgent);

            return new TokenPair("Bearer", newAccess, old.getToken(),
                    Instant.now().plusMillis(jwtService.getAccessExpMs()));
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        rtRepo.findByTokenAndRevokedFalse(refreshToken).ifPresent(t -> {
            t.setRevoked(true);
            rtRepo.save(t);
            audit.log("LOGOUT", t.getUser(), t.getDeviceId(), t.getIp(), t.getUserAgent());
        });
    }

    @Transactional
    public void revokeDevice(Long userId, String deviceId) {
        List<RefreshToken> tokens = rtRepo.findAllByUser_IdAndRevokedFalse(userId);
        for (RefreshToken t : tokens) {
            if (deviceId.equals(t.getDeviceId())) {
                t.setRevoked(true);
                rtRepo.save(t);
                audit.log("SESSION_REVOKE", t.getUser(), deviceId, t.getIp(), t.getUserAgent());
            }
        }
    }

    @Transactional
    public void revokeAllOtherDevices(Long userId, String keepDeviceId) {
        rtRepo.revokeAllOtherDevices(userId, keepDeviceId);
    }

    @Transactional
    public void heartbeat(Long userId, String deviceId) {
        online.ping(userId, deviceId);
        rtRepo.touchLastSeen(userId, deviceId, Instant.now());
    }

    /** ✅ DB darajasida pagination bilan sessiyalar */
    public Page<RefreshToken> listSessionsPage(Long userId, boolean includeRevoked, boolean includeExpired, Pageable pageable) {
        return rtRepo.findSessions(userId, includeRevoked, includeExpired, Instant.now(), pageable);
    }

    // ===== Legacy (list) — mavjud kodni saqlab turamiz =====
    public List<RefreshToken> listSessions(Long userId, boolean includeRevoked, boolean includeExpired) {
        List<RefreshToken> base = includeRevoked
                ? rtRepo.findAllByUser_Id(userId)
                : rtRepo.findAllByUser_IdAndRevokedFalse(userId);

        Instant now = Instant.now();
        if (!includeExpired) {
            base = base.stream()
                    .filter(t -> t.getExpiresAt() != null && t.getExpiresAt().isAfter(now))
                    .toList();
        }
        return base;
    }

    // ===== Helpers =====
    private boolean uaMatches(String stored, String incoming) {
        if (stored == null || incoming == null) return false;
        String mode = uaBindingMode == null ? "strict" : uaBindingMode.toLowerCase();
        return switch (mode) {
            case "strict" -> stored.equals(incoming);
            case "lenient" -> normalizeUa(stored).equalsIgnoreCase(normalizeUa(incoming));
            default -> stored.equals(incoming);
        };
    }

    private String normalizeUa(String ua) {
        int i = ua.indexOf('(');
        if (i > 0) ua = ua.substring(0, i);
        return ua.replaceAll("\\s+", " ").trim();
    }
}
