package com.agri.mapapp.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory online foydalanuvchilar trekeri.
 * Heartbeat kelganda (ping), (userId:deviceId) kalit yangilanadi.
 * 60 soniya ichida ping bo'lsa => online hisoblanadi.
 */
@Component
public class OnlineUserTracker {

    // key: userId + "::" + deviceId
    private final Map<String, Instant> lastSeenMap = new ConcurrentHashMap<>();
    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    public void ping(Long userId, String deviceId) {
        if (userId == null || deviceId == null) return;
        lastSeenMap.put(key(userId, deviceId), Instant.now());
    }

    public int getOnlineCount() {
        Instant now = Instant.now();
        return (int) lastSeenMap.values().stream()
                .filter(ts -> Duration.between(ts, now).abs().compareTo(ONLINE_TTL) <= 0)
                .count();
    }

    private String key(Long userId, String deviceId) {
        return userId + "::" + deviceId;
    }
}
