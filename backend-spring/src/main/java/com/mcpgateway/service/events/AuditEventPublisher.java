package com.mcpgateway.service.events;

/**
 * Abstraction for publishing audit events.
 * Default implementation logs locally; Kafka impl streams to a topic.
 */
public interface AuditEventPublisher {
    
    /**
     * Publish an audit event.
     * @param userId the user who made the call
     * @param orgId the organization ID
     * @param toolName the tool that was called
     * @param status SUCCESS, FAILED, or RATE_LIMITED
     * @param executionTimeMs how long the call took
     */
    void publish(Long userId, Long orgId, String toolName, String status, long executionTimeMs);
}

