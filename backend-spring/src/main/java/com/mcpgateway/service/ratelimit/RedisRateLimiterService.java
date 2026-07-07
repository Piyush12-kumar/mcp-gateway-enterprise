package com.mcpgateway.service.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed distributed sliding window rate limiter.
 * Uses Redis Sorted Sets with timestamps as scores.
 * Suitable for multi-node deployments where state must be shared.
 */
@Service
@ConditionalOnProperty(name = "features.redis.enabled", havingValue = "true")
public class RedisRateLimiterService implements RateLimiterService {

    private static final String KEY_PREFIX = "ratelimit:user:";
    private final int maxCalls = 100;
    private final int windowSec = 60;
    private final StringRedisTemplate redis;

    public RedisRateLimiterService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isAllowed(Long userId) {
        String key = KEY_PREFIX + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSec * 1000L);

        // Remove old entries outside the window
        redis.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // Count current calls in window
        Long count = redis.opsForZSet().zCard(key);
        if (count != null && count >= maxCalls) {
            return false;
        }
        
        // Add new call with current timestamp as score
        redis.opsForZSet().add(key, String.valueOf(now), now);
        redis.expire(key, windowSec + 10, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public int getRemainingCalls(Long userId) {
        String key = KEY_PREFIX + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (windowSec * 1000L);
        
        redis.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = redis.opsForZSet().zCard(key);
        return Math.max(0, maxCalls - (count == null ? 0 : count.intValue()));
    }
}

