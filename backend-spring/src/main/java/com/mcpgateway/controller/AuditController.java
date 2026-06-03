package com.mcpgateway.controller;

import com.mcpgateway.model.TaskAudit;
import com.mcpgateway.repository.TaskAuditRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final TaskAuditRepository auditRepo;

    public AuditController(TaskAuditRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<?> getUserLogs(@PathVariable Long userId) {
        Long reqOrg = com.mcpgateway.web.RequestContext.getOrgId();
        List<TaskAudit> logs = auditRepo.findByUserId(userId);
        
        // Filter by organization if present in context
        if (reqOrg != null) {
            logs = logs.stream()
                .filter(log -> log.getOrganizationId().equals(reqOrg))
                .toList();
        }
        
        return ResponseEntity.ok(logs.stream().map(this::auditToMap).toList());
    }

    @GetMapping("/logs/org/{orgId}")
    public ResponseEntity<?> getOrganizationLogs(@PathVariable Long orgId) {
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        // Enforce multi-tenancy
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        
        List<TaskAudit> logs = auditRepo.findByOrganizationId(orgId);
        return ResponseEntity.ok(logs.stream().map(this::auditToMap).toList());
    }

    @GetMapping("/logs/org/{orgId}/tool/{toolName}")
    public ResponseEntity<?> getToolLogs(@PathVariable Long orgId, @PathVariable String toolName) {
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        // Enforce multi-tenancy
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        
        List<TaskAudit> logs = auditRepo.findByOrganizationIdAndToolName(orgId, toolName);
        return ResponseEntity.ok(logs.stream().map(this::auditToMap).toList());
    }

    @GetMapping("/logs/org/{orgId}/range")
    public ResponseEntity<?> getAuditLogRange(
            @PathVariable Long orgId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        // Enforce multi-tenancy
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);
            
            List<TaskAudit> logs = auditRepo.findAuditLog(orgId, start, end);
            return ResponseEntity.ok(logs.stream().map(this::auditToMap).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_date_format"));
        }
    }

    @GetMapping("/stats/org/{orgId}")
    public ResponseEntity<?> getOrgStats(@PathVariable Long orgId) {
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        // Enforce multi-tenancy
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        
        List<TaskAudit> logs = auditRepo.findByOrganizationId(orgId);
        
        long totalCalls = logs.size();
        long successCalls = logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long failedCalls = logs.stream().filter(l -> "FAILED".equals(l.getStatus())).count();
        long rateLimitedCalls = logs.stream().filter(l -> "RATE_LIMITED".equals(l.getStatus())).count();
        double avgExecutionTime = logs.stream()
            .mapToLong(l -> l.getExecutionTimeMs() != null ? l.getExecutionTimeMs() : 0)
            .average()
            .orElse(0.0);
        
        return ResponseEntity.ok(Map.of(
            "totalCalls", totalCalls,
            "successCalls", successCalls,
            "failedCalls", failedCalls,
            "rateLimitedCalls", rateLimitedCalls,
            "avgExecutionTimeMs", avgExecutionTime
        ));
    }

    private Map<String, Object> auditToMap(TaskAudit audit) {
        return Map.of(
            "id", audit.getId(),
            "userId", audit.getUserId(),
            "organizationId", audit.getOrganizationId(),
            "toolName", audit.getToolName(),
            "status", audit.getStatus(),
            "createdAt", audit.getCreatedAt().toString(),
            "executionTimeMs", audit.getExecutionTimeMs() != null ? audit.getExecutionTimeMs() : 0,
            "errorMessage", audit.getErrorMessage() != null ? audit.getErrorMessage() : ""
        );
    }
}

