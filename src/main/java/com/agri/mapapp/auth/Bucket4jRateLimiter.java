package com.agri.mapapp.auth;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.ratelimit.bucket4j.enabled", havingValue = "true")
public class Bucket4jRateLimiter implements RateLimiter {

    private final io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager<String> proxyManager;
    private final Bucket4jRedisConfig.Bucket4jSpecs specs;

    @Override
    public boolean tryConsumeLogin(String key) {
        String redisKey = "b4j:login:" + key;
        BucketConfiguration cfg = specs.loginConfig();
        Bucket bucket = proxyManager.builder().build(redisKey, cfg);
        return bucket.tryConsume(1);
    }

    @Override
    public boolean tryConsumeRefresh(String key) {
        String redisKey = "b4j:refresh:" + key;
        BucketConfiguration cfg = specs.refreshConfig();
        Bucket bucket = proxyManager.builder().build(redisKey, cfg);
        return bucket.tryConsume(1);
    }
}
