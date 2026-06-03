package com.mcpgateway.controller;

import com.mcpgateway.model.McpServer;
import com.mcpgateway.repository.McpServerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mcp")
public class McpController {
    private final McpServerRepository repo;

    public McpController(McpServerRepository repo) { this.repo = repo; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody McpServer server) {
        // enforce organization: if header present, set server org to requester org
        Long reqOrg = com.mcpgateway.web.RequestContext.getOrgId();
        if (reqOrg != null) server.setOrganizationId(reqOrg);
        McpServer s = repo.save(server);
        return ResponseEntity.ok(s);
    }

    @GetMapping("/list")
    public ResponseEntity<List<McpServer>> list(@RequestParam(required = false) Long organizationId) {
        if (organizationId == null) return ResponseEntity.ok(repo.findAll());
        return ResponseEntity.ok(repo.findByOrganizationId(organizationId));
    }
}
