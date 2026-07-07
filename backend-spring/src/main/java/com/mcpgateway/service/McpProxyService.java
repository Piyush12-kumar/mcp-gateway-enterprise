package com.mcpgateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mcpgateway.model.McpServer;
import com.mcpgateway.model.User;
import com.mcpgateway.repository.McpServerRepository;
import com.mcpgateway.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Proxies tool calls to downstream MCP servers using the real
 * <b>Model Context Protocol</b> transport: JSON-RPC 2.0.
 *
 * <p>Why this matters: MCP's value is turning N×M custom integrations into
 * N+M via ONE standard protocol. A tool call is expressed as a JSON-RPC
 * request with method {@code tools/call} and params {@code {name, arguments}},
 * and the server replies with either a {@code result} or an {@code error}.
 * Previously this class did a plain {@code POST /call/{tool}} with a raw body,
 * which is NOT the MCP wire format — this version speaks the actual protocol.</p>
 */
@Service
public class McpProxyService {
    private static final Logger logger = LoggerFactory.getLogger(McpProxyService.class);

    /** MCP/JSON-RPC protocol constants. */
    private static final String JSONRPC_VERSION = "2.0";
    private static final String METHOD_TOOLS_CALL = "tools/call";
    private static final String METHOD_TOOLS_LIST = "tools/list";

    private final McpServerRepository servers;
    private final UserRepository users;
    private final VaultService vault;
    private final ObjectMapper mapper = new ObjectMapper();
    /** Monotonic id so each JSON-RPC request/response pair can be correlated. */
    private final AtomicLong requestId = new AtomicLong(1);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public McpProxyService(McpServerRepository servers, UserRepository users, VaultService vault) {
        this.servers = servers;
        this.users = users;
        this.vault = vault;
    }

    /**
     * Forward a tool call to the tenant's MCP server as a JSON-RPC 2.0 {@code tools/call}.
     *
     * @param userId      the calling user (used for tenant scoping + per-user API key)
     * @param tool        the tool name (becomes JSON-RPC params.name)
     * @param jsonPayload the tool arguments as a JSON string (becomes params.arguments)
     * @return the JSON-RPC {@code result} serialized as a string
     * @throws IllegalStateException if no MCP server is registered for the tenant
     */
    @CircuitBreaker(name = "mcpServer", fallbackMethod = "proxyFallback")
    @Retry(name = "mcpServer")
    public String proxyToolCall(Long userId, String tool, String jsonPayload) throws Exception {
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        Long orgId = u.getOrganizationId();
        List<McpServer> list = (orgId == null) ? servers.findAll() : servers.findByOrganizationId(orgId);
        if (list.isEmpty()) throw new IllegalStateException("no MCP servers available");
        McpServer pick = list.get(0);

        String apiKey = null;
        if (u.getEncryptedApiKey() != null) apiKey = vault.decrypt(u.getEncryptedApiKey());

        // Parse the user-supplied arguments into a JSON node (default to empty object).
        JsonNode arguments;
        if (jsonPayload == null || jsonPayload.isBlank()) {
            arguments = mapper.createObjectNode();
        } else {
            try {
                arguments = mapper.readTree(jsonPayload);
            } catch (Exception e) {
                // Not valid JSON -> wrap it as a single "input" argument so the call still works.
                ObjectNode wrap = mapper.createObjectNode();
                wrap.put("input", jsonPayload);
                arguments = wrap;
            }
        }

        // Build the JSON-RPC 2.0 envelope: {"jsonrpc":"2.0","id":N,"method":"tools/call","params":{name,arguments}}
        long id = requestId.getAndIncrement();
        ObjectNode params = mapper.createObjectNode();
        params.put("name", tool);
        params.set("arguments", arguments);
        ObjectNode rpc = mapper.createObjectNode();
        rpc.put("jsonrpc", JSONRPC_VERSION);
        rpc.put("id", id);
        rpc.put("method", METHOD_TOOLS_CALL);
        rpc.set("params", params);
        String rpcBody = mapper.writeValueAsString(rpc);

        JsonNode root = sendRpc(pick, apiKey, rpcBody);

        // JSON-RPC: a response carries EITHER "result" OR "error".
        if (root.has("error") && !root.get("error").isNull()) {
            JsonNode err = root.get("error");
            String message = err.path("message").asText("unknown_error");
            int code = err.path("code").asInt(0);
            throw new RuntimeException("MCP error " + code + ": " + message);
        }
        JsonNode result = root.get("result");
        return result == null ? root.toString() : result.toString();
    }

    /**
     * Resilience4j fallback, invoked when retries are exhausted or the circuit
     * breaker is OPEN. Domain errors (bad user / no server) are propagated as-is;
     * transport/availability failures are surfaced as a clean "mcp_unavailable"
     * IllegalStateException, which the controller maps to HTTP 503.
     */
    @SuppressWarnings("unused")
    public String proxyFallback(Long userId, String tool, String jsonPayload, Throwable t) throws Exception {
        if (t instanceof IllegalArgumentException || t instanceof IllegalStateException) {
            throw (Exception) t;
        }
        String detail = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        logger.warn("MCP call falling back for tool={} : {}", tool, detail);
        throw new IllegalStateException("mcp_unavailable: " + detail);
    }

    /**
     * Discover the tools a server exposes via JSON-RPC {@code tools/list}.
     * Returned as the raw {@code result} JSON string (e.g. {@code {"tools":[...]}}).
     */
    public String listTools(Long serverId, String apiKey) throws Exception {
        McpServer server = servers.findById(serverId)
                .orElseThrow(() -> new IllegalArgumentException("server not found"));
        long id = requestId.getAndIncrement();
        ObjectNode rpc = mapper.createObjectNode();
        rpc.put("jsonrpc", JSONRPC_VERSION);
        rpc.put("id", id);
        rpc.put("method", METHOD_TOOLS_LIST);
        rpc.set("params", mapper.createObjectNode());
        JsonNode root = sendRpc(server, apiKey, mapper.writeValueAsString(rpc));
        if (root.has("error") && !root.get("error").isNull()) {
            throw new RuntimeException("MCP error: " + root.get("error").path("message").asText());
        }
        JsonNode result = root.get("result");
        return result == null ? root.toString() : result.toString();
    }

    /** Send a JSON-RPC body to the server's endpoint and parse the JSON response. */
    private JsonNode sendRpc(McpServer server, String apiKey, String rpcBody) throws Exception {
        String endpoint = rpcEndpoint(server);
        logger.debug("MCP JSON-RPC -> {} body={}", endpoint, rpcBody);
        HttpRequest.Builder reqb = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rpcBody));
        if (apiKey != null) reqb.header("Authorization", "Bearer " + apiKey);

        HttpResponse<String> resp;
        try {
            resp = client.send(reqb.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Connection refused / timeout / DNS etc. often have a null message — make it explicit.
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new RuntimeException("MCP connection failed to " + endpoint + ": " + detail, e);
        }
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("MCP transport error: HTTP " + resp.statusCode() + " - " + resp.body());
        }
        return mapper.readTree(resp.body());
    }

    /**
     * Resolve the JSON-RPC endpoint for a server. In MCP the method lives in the
     * request body, so we POST to the server's base URL (its single RPC endpoint).
     */
    private String rpcEndpoint(McpServer server) {
        String base = server.getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("server has no base URL");
        }
        return base;
    }
}
