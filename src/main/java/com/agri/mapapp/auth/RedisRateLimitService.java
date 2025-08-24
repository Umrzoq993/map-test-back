package com.agri.mapapp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"app.security.ratelimit.distributed.enabled", "app.security.ratelimit.bucket4j.enabled"},
        havingValue = "true,false"
)
public class RedisRateLimitService implements RateLimiter {

    private final StringRedisTemplate redis;

    @Value("${app.security.ratelimit.login.max-requests:10}")
    private int loginMax;
    @Value("${app.security.ratelimit.login.window-seconds:60}")
    private long loginWinSec;

    @Value("${app.security.ratelimit.refresh.max-requests:30}")
    private int refreshMax;
    @Value("${app.security.ratelimit.refresh.window-seconds:60}")
    private long refreshWinSec;

    @Override
    public boolean tryConsumeLogin(String key) {
        return incrWithinWindow("rl:login:", key, loginMax, loginWinSec);
    }

    @Override
    public boolean tryConsumeRefresh(String key) {
        return incrWithinWindow("rl:refresh:", key, refreshMax, refreshWinSec);
    }

    private boolean incrWithinWindow(String prefix, String key, int max, long windowSec) {
        String k = prefix + key;
        Long val = redis.opsForValue().increment(k);
        if (val != null && val == 1L) {
            redis.expire(k, Duration.ofSeconds(windowSec));
        }
        return val != null && val <= max;
    }
}
