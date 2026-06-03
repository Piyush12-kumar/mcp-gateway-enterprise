package com.mcpgateway.controller;

import com.mcpgateway.model.User;
import com.mcpgateway.repository.UserRepository;
import com.mcpgateway.service.SafetyService;
import com.mcpgateway.service.VaultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {
    private final UserRepository users;
    private final VaultService vault;
    private final SafetyService safety;

    public UserController(UserRepository users, VaultService vault, SafetyService safety) {
        this.users = users;
        this.vault = vault;
        this.safety = safety;
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String orgIdStr = body.get("organizationId");
        if (username == null) return ResponseEntity.badRequest().body(Map.of("error", "username required"));
        
        if (users.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_already_exists"));
        }
        
        User u = new User();
        u.setUsername(username);
        if (orgIdStr != null) {
            try { u.setOrganizationId(Long.valueOf(orgIdStr)); } catch (Exception ignored) {}
        }
        User saved = users.save(u);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "username", saved.getUsername(), "organizationId", saved.getOrganizationId()));
    }

    @PostMapping("/users/{id}/apikey")
    public ResponseEntity<?> setApiKey(@PathVariable Long id, @RequestBody Map<String, String> body) throws Exception {
        String key = body.get("api_key");
        if (key == null) return ResponseEntity.badRequest().body(Map.of("error", "api_key required"));
        User u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        // enforce tenant: header org must match user's org
        Long reqOrg = com.mcpgateway.web.RequestContext.getOrgId();
        if (reqOrg != null && u.getOrganizationId() != null && !reqOrg.equals(u.getOrganizationId())) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        byte[] token = vault.encrypt(key);
        u.setEncryptedApiKey(token);
        users.save(u);
        return ResponseEntity.ok(Map.of("status", "stored"));
    }

    @PostMapping("/tools/{tool}/call")
    public ResponseEntity<?> callTool(@PathVariable String tool, @RequestBody Map<String, Object> body) throws Exception {
        Long userId = com.mcpgateway.web.RequestContext.getUserId();
        Long orgId = com.mcpgateway.web.RequestContext.getOrgId();
        
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "missing_user_header"));
        }
        
        long startTime = System.currentTimeMillis();
        String bodyStr = body.toString();
        
        if (!safety.allow(userId)) {
            long execTime = System.currentTimeMillis() - startTime;
            safety.logCall(userId, orgId, tool, bodyStr, "RATE_LIMITED", "Rate limit exceeded", execTime);
            return ResponseEntity.status(429).body(Map.of("error", "rate_limited"));
        }
        
        long execTime = System.currentTimeMillis() - startTime;
        safety.logCall(userId, orgId, tool, bodyStr, "SUCCESS", null, execTime);
        // For prototype, just return success. Real implementation would proxy to MCP server using stored API key.
        return ResponseEntity.ok(Map.of("status", "ok", "tool", tool));
    }

    @GetMapping("/users/{id}/apikey")
    public ResponseEntity<?> getApiKey(@PathVariable Long id) throws Exception {
        User u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        Long reqOrg = com.mcpgateway.web.RequestContext.getOrgId();
        if (reqOrg != null && u.getOrganizationId() != null && !reqOrg.equals(u.getOrganizationId())) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        if (u.getEncryptedApiKey() == null) return ResponseEntity.ok(Map.of("api_key", null));
        String val = vault.decrypt(u.getEncryptedApiKey());
        return ResponseEntity.ok(Map.of("api_key", val));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        User u = users.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        Long reqOrg = com.mcpgateway.web.RequestContext.getOrgId();
        if (reqOrg != null && u.getOrganizationId() != null && !reqOrg.equals(u.getOrganizationId())) {
            return ResponseEntity.status(403).body(Map.of("error", "organization_mismatch"));
        }
        return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername(), "organizationId", u.getOrganizationId()));
    }
}
