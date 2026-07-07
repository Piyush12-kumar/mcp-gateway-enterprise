package com.mcpgateway.service.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding window rate limiter.
 * Uses a Deque per user to track call timestamps.
 * Good for single-instance deployment; for multi-node use Redis.
 */
@Service
@ConditionalOnProperty(name = "features.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiterService implements RateLimiterService {

    private final Map<Long, Deque<Long>> calls = new ConcurrentHashMap<>();
    private final int maxCalls = 100;
    private final int windowSec = 60;

    @Override
    public boolean isAllowed(Long userId) {
        long now = Instant.now().getEpochSecond();
        Deque<Long> dq = calls.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());
        synchronized (dq) {
            // Remove expired timestamps from the head
            while (!dq.isEmpty() && dq.peekFirst() <= now - windowSec) {
                dq.pollFirst();
            }
            if (dq.size() >= maxCalls) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }

    @Override
    public int getRemainingCalls(Long userId) {
        long now = Instant.now().getEpochSecond();
        Deque<Long> dq = calls.get(userId);
        if (dq == null) return maxCalls;
        synchronized (dq) {
            while (!dq.isEmpty() && dq.peekFirst() <= now - windowSec) {
                dq.pollFirst();
            }
            return Math.max(0, maxCalls - dq.size());
        }
    }
}

