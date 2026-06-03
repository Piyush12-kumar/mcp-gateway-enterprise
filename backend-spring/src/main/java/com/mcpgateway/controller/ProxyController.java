package com.mcpgateway.controller;

import com.mcpgateway.service.McpProxyService;
import com.mcpgateway.service.SafetyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/proxy")
public class ProxyController {
    private final McpProxyService proxy;
    private final SafetyService safety;

    public ProxyController(McpProxyService proxy, SafetyService safety) {
        this.proxy = proxy;
        this.safety = safety;
    }

    @PostMapping("/{tool}/call")
    public ResponseEntity<?> call(@PathVariable String tool, @RequestBody String body) throws Exception {
        Long userId = com.mcpgateway.web.RequestContext.getUserId();
        Long orgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "missing_user_header"));
        }
        
        long startTime = System.currentTimeMillis();
        
        if (!safety.allow(userId)) {
            long execTime = System.currentTimeMillis() - startTime;
            safety.logCall(userId, orgId, tool, body, "RATE_LIMITED", "Rate limit exceeded", execTime);
            return ResponseEntity.status(429).body(Map.of("error", "rate_limited"));
        }
        
        try {
            String resp = proxy.proxyToolCall(userId, tool, body);
            long execTime = System.currentTimeMillis() - startTime;
            safety.logCall(userId, orgId, tool, body, "SUCCESS", null, execTime);
            return ResponseEntity.ok(Map.of("status", "ok", "result", resp));
        } catch (IllegalStateException e) {
            long execTime = System.currentTimeMillis() - startTime;
            safety.logCall(userId, orgId, tool, body, "FAILED", e.getMessage(), execTime);
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            long execTime = System.currentTimeMillis() - startTime;
            safety.logCall(userId, orgId, tool, body, "FAILED", e.getMessage(), execTime);
            return ResponseEntity.status(500).body(Map.of("error", "internal_error", "message", e.getMessage()));
        }
    }
}
