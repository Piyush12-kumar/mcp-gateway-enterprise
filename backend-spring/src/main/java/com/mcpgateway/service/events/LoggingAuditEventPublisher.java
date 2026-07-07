package com.mcpgateway.service.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default audit event publisher that logs events locally.
 * Used when Kafka is not enabled.
 */
@Service
@ConditionalOnProperty(name = "features.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingAuditEventPublisher implements AuditEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAuditEventPublisher.class);

    @Override
    public void publish(Long userId, Long orgId, String toolName, String status, long executionTimeMs) {
        logger.info("AUDIT_EVENT: userId={}, orgId={}, tool={}, status={}, time={}ms",
                userId, orgId, toolName, status, executionTimeMs);
    }
}

