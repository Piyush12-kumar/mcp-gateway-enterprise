package com.mcpgateway.service.ratelimit;

/**
 * Rate limiter abstraction with pluggable backends.
 * In-memory for single instance, Redis for distributed deployment.
 */
public interface RateLimiterService {

    /**
     * Check if a user is allowed to make another call.
     * @param userId the user ID
     * @return true if allowed, false if rate limited
     */
    boolean isAllowed(Long userId);

    /**
     * Get the number of remaining calls for a user in the current window.
     * @param userId the user ID
     * @return number of remaining calls
     */
    int getRemainingCalls(Long userId);
}

