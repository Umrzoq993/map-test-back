package com.agri.mapapp.captcha;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryCaptchaStore implements CaptchaStore {

    private static class Entry {
        final String value;
        final long expiresAt;
        Entry(String value, long expiresAt) { this.value = value; this.expiresAt = expiresAt; }
    }

    private final Map<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public void save(String key, String value, Duration ttl) {
        Objects.requireNonNull(key);
        long exp = Instant.now().plus(ttl).toEpochMilli();
        map.put(key, new Entry(value, exp));
    }

    @Override
    public String get(String key) {
        Entry e = map.get(key);
        if (e == null) return null;
        if (e.expiresAt < System.currentTimeMillis()) {
            map.remove(key);
            return null;
        }
        return e.value;
    }

    @Override
    public void delete(String key) {
        map.remove(key);
    }
}
