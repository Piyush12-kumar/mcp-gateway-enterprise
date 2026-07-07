package com.mcpgateway.controller;

import com.mcpgateway.model.McpServer;
import com.mcpgateway.repository.McpServerRepository;
import com.mcpgateway.service.McpProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP Server management endpoints.
 * 
 * RBAC rules:
 * - Register server: ADMIN or ORG_ADMIN only (creating infrastructure)
 * - List servers: Any authenticated user (discovery)
 * - List tools: Any authenticated user (tool catalog)
 */
@RestController
@RequestMapping("/mcp")
public class McpController {
    private final McpServerRepository repo;
    private final McpProxyService proxy;

    public McpController(McpServerRepository repo, McpProxyService proxy) {
        this.repo = repo;
        this.proxy = proxy;
    }

    /**
     * Register a new MCP server. Only admins can add infrastructure.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN','ORG_ADMIN')")
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

    /**
     * Discover the tools a registered MCP server exposes, using the real MCP
     * JSON-RPC {@code tools/list} method. Demonstrates the standard protocol
     * handshake rather than a custom REST shape.
     */
    @GetMapping("/{serverId}/tools")
    public ResponseEntity<?> listTools(@PathVariable Long serverId) {
        try {
            String result = proxy.listTools(serverId, null);
            return ResponseEntity.ok(Map.of("status", "ok", "result", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Most commonly the downstream MCP server isn't reachable in a demo env.
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(502).body(Map.of("error", "mcp_unreachable", "message", msg));
        }
    }
}
