package com.agri.mapapp.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.security.ratelimit.distributed.enabled",
        havingValue = "false", matchIfMissing = true)
public class RateLimitService implements RateLimiter {

    private static final class Counter {
        long windowStartEpochSec;
        int count;
    }

    private final ConcurrentHashMap<String, Counter> loginMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> refreshMap = new ConcurrentHashMap<>();

    @Value("${app.security.ratelimit.login.max-requests:10}")
    private int loginMax;
    @Value("${app.security.ratelimit.login.window-seconds:60}")
    private long loginWindowSec;

    @Value("${app.security.ratelimit.refresh.max-requests:30}")
    private int refreshMax;
    @Value("${app.security.ratelimit.refresh.window-seconds:60}")
    private long refreshWindowSec;

    @Override
    public boolean tryConsumeLogin(String key) {
        return tryConsume(loginMap, key, loginMax, loginWindowSec);
    }

    @Override
    public boolean tryConsumeRefresh(String key) {
        return tryConsume(refreshMap, key, refreshMax, refreshWindowSec);
    }

    private boolean tryConsume(ConcurrentHashMap<String, Counter> map,
                               String key, int max, long windowSec) {
        long now = Instant.now().getEpochSecond();
        Counter c = map.computeIfAbsent(key, k -> {
            Counter n = new Counter();
            n.windowStartEpochSec = now;
            n.count = 0;
            return n;
        });
        synchronized (c) {
            if (now - c.windowStartEpochSec >= windowSec) {
                c.windowStartEpochSec = now;
                c.count = 0;
            }
            c.count++;
            return c.count <= max;
        }
    }
}
