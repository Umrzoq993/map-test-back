package com.agri.mapapp.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static java.time.Duration.ofSeconds;

@Configuration
@ConditionalOnProperty(name = "app.security.ratelimit.bucket4j.enabled", havingValue = "true")
public class Bucket4jRedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Lettuce client va connection (String, byte[]) — Bucket4j talabiga mos.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient() {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            builder.withPassword(redisPassword.toCharArray());
        }
        return RedisClient.create(builder.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        return bucket4jRedisClient.connect(codec);
    }

    /**
     * Redis ProxyManager — Bucket4j uchun markaziy kirish nuqtasi.
     * TTL: “bucket to'lib ketguncha” vaqtga asoslangan o'chirish strategiyasi.
     */
    @Bean
    public LettuceBasedProxyManager<String> lettuceProxyManager(StatefulRedisConnection<String, byte[]> conn,
                                                                @Value("${app.security.ratelimit.login.window-seconds:60}") long loginWinSec) {
        // expiry strategy: umumiy holda window ga yaqin.
        return LettuceBasedProxyManager.builderFor(conn)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(ofSeconds(Math.max(10, loginWinSec))))
                .build();
    }

    /**
     * Default konfiguratsiya yasash uchun helper.
     */
    @Bean
    public Bucket4jSpecs bucket4jSpecs(
            @Value("${app.security.ratelimit.login.max-requests:10}") int loginMax,
            @Value("${app.security.ratelimit.login.window-seconds:60}") long loginWinSec,
            @Value("${app.security.ratelimit.refresh.max-requests:30}") int refreshMax,
            @Value("${app.security.ratelimit.refresh.window-seconds:60}") long refreshWinSec) {

        return new Bucket4jSpecs(loginMax, loginWinSec, refreshMax, refreshWinSec);
    }

    public record Bucket4jSpecs(int loginMax, long loginWinSec, int refreshMax, long refreshWinSec) {
        public BucketConfiguration loginConfig() {
            Bandwidth bw = Bandwidth.classic(loginMax, Refill.greedy(loginMax, Duration.ofSeconds(loginWinSec)));
            return BucketConfiguration.builder().addLimit(bw).build();
        }
        public BucketConfiguration refreshConfig() {
            Bandwidth bw = Bandwidth.classic(refreshMax, Refill.greedy(refreshMax, Duration.ofSeconds(refreshWinSec)));
            return BucketConfiguration.builder().addLimit(bw).build();
        }
    }
}
