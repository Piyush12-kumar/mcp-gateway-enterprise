package com.mcpgateway.service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka-based audit event publisher.
 * Streams events to a topic for downstream consumers (billing, analytics, compliance).
 * Events are keyed by orgId for partition ordering.
 */
@Service
@ConditionalOnProperty(name = "features.kafka.enabled", havingValue = "true")
public class KafkaAuditEventPublisher implements AuditEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaAuditEventPublisher.class);
    private static final String TOPIC = "audit-events";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaAuditEventPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(Long userId, Long orgId, String toolName, String status, long executionTimeMs) {
        try {
            Map<String, Object> event = Map.of(
                    "timestamp", Instant.now().toString(),
                    "userId", userId,
                    "orgId", orgId == null ? -1 : orgId,
                    "toolName", toolName,
                    "status", status,
                    "executionTimeMs", executionTimeMs
            );
            String json = mapper.writeValueAsString(event);
            String key = orgId == null ? "global" : orgId.toString();
            kafka.send(TOPIC, key, json);
            logger.debug("Published audit event to Kafka: {}", json);
        } catch (Exception e) {
            logger.error("Failed to publish audit event to Kafka", e);
        }
    }
}

