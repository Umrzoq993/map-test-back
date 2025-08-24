package com.agri.mapapp.auth;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.jwt.refresh-exp-seconds:86400}") // 1 kun default
    private long refreshExpSeconds;

    @Value("${app.security.refresh.ip-binding.enabled:true}")
    private boolean ipBindingEnabled;

    @Value("${app.security.refresh.ua-binding.enabled:false}")
    private boolean uaBindingEnabled;

    @Value("${app.security.refresh.ua-binding.mode:lenient}") // strict | lenient
    private String uaBindingMode;

    @Transactional
    public AuthController.TokenPair loginIssueTokens(String username, String password,
                                                     String deviceId, String userAgent, String ip) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        AppUser user = userRepo.findByUsername(up.getUsername()).orElseThrow();

        // 1-device policy: barcha eski sessiyalarni o'chiramiz
        rtRepo.revokeAllByUserId(user.getId());

        // yangi refresh token
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

        Long orgId = user.getOrg() != null ? user.getOrg().getId() : null;
        String access = jwtService.generateAccessToken(user.getUsername(), user.getRole(), orgId);

        online.ping(user.getId(), deviceId);
        rtRepo.touchLastSeen(user.getId(), deviceId, Instant.now());

        return new AuthController.TokenPair("Bearer", access, rt.getToken(),
                Instant.now().plus(120, ChronoUnit.MINUTES));
    }

    @Transactional
    public AuthController.TokenPair rotate(String refreshToken, String deviceId, String userAgent, String ip) {
        RefreshToken old = rtRepo.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Device tekshiruvi
        if (!old.getDeviceId().equals(deviceId)) {
            old.setRevoked(true);
            rtRepo.save(old);
            throw new BadCredentialsException("Refresh token device mismatch");
        }

        // UA binding
        if (uaBindingEnabled) {
            if (!uaMatches(old.getUserAgent(), userAgent)) {
                old.setRevoked(true);
                rtRepo.save(old);
                throw new BadCredentialsException("User-Agent mismatch for refresh token");
            }
        }

        // IP binding
        if (ipBindingEnabled) {
            String savedIp = old.getIp();
            if (savedIp == null || !savedIp.equals(ip)) {
                old.setRevoked(true);
                rtRepo.save(old);
                throw new BadCredentialsException("IP mismatch for refresh token");
            }
        }

        if (old.getExpiresAt().isBefore(Instant.now())) {
            old.setRevoked(true);
            rtRepo.save(old);
            throw new BadCredentialsException("Refresh token expired");
        }

        AppUser user = old.getUser();
        Long orgId = user.getOrg() != null ? user.getOrg().getId() : null;

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

        String newAccess = jwtService.generateAccessToken(user.getUsername(), user.getRole(), orgId);

        online.ping(user.getId(), deviceId);
        rtRepo.touchLastSeen(user.getId(), deviceId, Instant.now());

        return new AuthController.TokenPair("Bearer", newAccess, nu.getToken(),
                Instant.now().plus(120, ChronoUnit.MINUTES));
    }

    @Transactional
    public void logout(String refreshToken) {
        rtRepo.findByTokenAndRevokedFalse(refreshToken).ifPresent(t -> {
            t.setRevoked(true);
            rtRepo.save(t);
        });
    }

    @Transactional
    public void heartbeat(Long userId, String deviceId) {
        online.ping(userId, deviceId);
        rtRepo.touchLastSeen(userId, deviceId, Instant.now());
    }

    public int onlineCount() {
        return online.getOnlineCount();
    }

    public List<RefreshToken> listActiveSessions() {
        return rtRepo.findAllByRevokedFalse();
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        rtRepo.revokeAllByUserId(userId);
    }

    // ==== UA helperlar ====
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
        // '(' dan oldingi prefiksni olib, bo'shliqlarni siqamiz
        int i = ua.indexOf('(');
        if (i > 0) ua = ua.substring(0, i);
        return ua.replaceAll("\\s+", " ").trim();
    }
}
