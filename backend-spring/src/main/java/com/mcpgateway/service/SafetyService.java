package com.mcpgateway.service;

import com.mcpgateway.model.TaskAudit;
import com.mcpgateway.repository.TaskAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class SafetyService {
    private static final Logger logger = LoggerFactory.getLogger(SafetyService.class);
    private final Map<Long, Deque<Long>> calls = new ConcurrentHashMap<>();
    private final int maxCalls = 100; // Increased from 20
    private final int windowSec = 60;
    private final TaskAuditRepository auditRepo;

    public SafetyService(TaskAuditRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public boolean allow(Long userId) {
        long now = Instant.now().getEpochSecond();
        Deque<Long> dq = calls.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());
        synchronized (dq) {
            while (!dq.isEmpty() && dq.peekFirst() <= now - windowSec) dq.pollFirst();
            if (dq.size() >= maxCalls) return false;
            dq.addLast(now);
            return true;
        }
    }

    public void logCall(Long userId, Long orgId, String tool, String payload, String status, String errorMsg, long executionTime) {
        try {
            // Log to database
            TaskAudit audit = new TaskAudit(userId, orgId, tool, status);
            audit.setRequestPayload(payload);
            audit.setErrorMessage(errorMsg);
            audit.setExecutionTimeMs(executionTime);
            auditRepo.save(audit);
            
            // Also log to file and logger
            logger.info("Tool call - User: {}, Org: {}, Tool: {}, Status: {}, Time: {}ms", 
                       userId, orgId, tool, status, executionTime);
            
            try (PrintWriter pw = new PrintWriter(new FileWriter("backend-spring/calls.log", true))) {
                pw.printf("%d\tuser=%d\torg=%d\ttool=%s\tstatus=%s\ttime=%dms\n", 
                         Instant.now().getEpochSecond(), userId, orgId, tool, status, executionTime);
            }
        } catch (Exception e) {
            logger.error("Error logging call", e);
        }
    }

    public void logCallResponse(Long userId, Long orgId, String tool, String responsePayload) {
        try {
            // In a real system, you might want to update the latest audit record
            logger.debug("Tool response - User: {}, Tool: {}", userId, tool);
        } catch (Exception e) {
            logger.error("Error logging response", e);
        }
    }
}
