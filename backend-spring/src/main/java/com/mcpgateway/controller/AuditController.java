package com.mcpgateway.controller;

import com.mcpgateway.model.TaskAudit;
import com.mcpgateway.repository.TaskAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Get audit logs for an organization.
     * Only admins or org admins of that org can view.
     */
    @GetMapping("/logs/org/{orgId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORG_ADMIN') and @securityService.belongsToOrg(authentication, #orgId))")
    public ResponseEntity<?> getOrganizationLogs(@PathVariable Long orgId) {
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        // Enforce multi-tenancy
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        
        List<TaskAudit> logs = auditRepo.findByOrganizationId(orgId);
        return ResponseEntity.ok(logs.stream().map(this::auditToMap).toList());
    }

    /**
     * Get audit logs for a specific tool in an organization.
     */
    @GetMapping("/logs/org/{orgId}/tool/{toolName}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORG_ADMIN') and @securityService.belongsToOrg(authentication, #orgId))")
    public ResponseEntity<?> getToolLogs(@PathVariable Long orgId, @PathVariable String toolName) {
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        // Enforce multi-tenancy
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        
        List<TaskAudit> logs = auditRepo.findByOrganizationIdAndToolName(orgId, toolName);
        return ResponseEntity.ok(logs.stream().map(this::auditToMap).toList());
    }

    /**
     * Get audit logs within a date range.
     */
    @GetMapping("/logs/org/{orgId}/range")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORG_ADMIN') and @securityService.belongsToOrg(authentication, #orgId))")
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

    /**
     * Get statistics for an organization.
     */
    @GetMapping("/stats/org/{orgId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORG_ADMIN') and @securityService.belongsToOrg(authentication, #orgId))")
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

    /**
     * Paginated audit logs for an organization (newest first). Avoids loading
     * the entire table when it grows large. Use ?page=0&size=20.
     */
    @GetMapping("/logs/org/{orgId}/page")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORG_ADMIN') and @securityService.belongsToOrg(authentication, #orgId))")
    public ResponseEntity<?> getOrgLogsPaged(
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        int safeSize = Math.min(Math.max(size, 1), 100); // clamp 1..100
        Page<TaskAudit> result = auditRepo.findByOrganizationId(
                orgId, PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(Map.of(
                "content", result.getContent().stream().map(this::auditToMap).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "last", result.isLast()
        ));
    }

    /**
     * Usage metering / billing summary for an organization: total calls plus a
     * per-tool breakdown and a simple monthly-quota status. This is the data a
     * billing system would meter tenants on.
     */
    @GetMapping("/usage/org/{orgId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ORG_ADMIN') and @securityService.belongsToOrg(authentication, #orgId))")
    public ResponseEntity<?> getOrgUsage(@PathVariable Long orgId) {
        Long reqOrgId = com.mcpgateway.web.RequestContext.getOrgId();
        if (reqOrgId != null && !reqOrgId.equals(orgId)) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }

        List<TaskAudit> logs = auditRepo.findByOrganizationId(orgId);
        long totalCalls = logs.size();

        Map<String, Long> perTool = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getToolName() == null ? "unknown" : l.getToolName(),
                        Collectors.counting()));

        long monthlyQuota = 10000;
        long remaining = Math.max(0, monthlyQuota - totalCalls);

        return ResponseEntity.ok(Map.of(
                "organizationId", orgId,
                "totalCalls", totalCalls,
                "perTool", perTool,
                "monthlyQuota", monthlyQuota,
                "remaining", remaining,
                "quotaExceeded", totalCalls > monthlyQuota
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

