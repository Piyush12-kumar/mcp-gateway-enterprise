package com.mcpgateway.service;

import com.mcpgateway.model.McpServer;
import com.mcpgateway.model.User;
import com.mcpgateway.repository.McpServerRepository;
import com.mcpgateway.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class McpProxyService {
    private final McpServerRepository servers;
    private final UserRepository users;
    private final VaultService vault;

    public McpProxyService(McpServerRepository servers, UserRepository users, VaultService vault) {
        this.servers = servers;
        this.users = users;
        this.vault = vault;
    }

    public String proxyToolCall(Long userId, String tool, String jsonPayload) throws Exception {
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        Long orgId = u.getOrganizationId();
        List<McpServer> list = (orgId == null) ? servers.findAll() : servers.findByOrganizationId(orgId);
        if (list.isEmpty()) throw new IllegalStateException("no MCP servers available");
        McpServer pick = list.get(0);

        String apiKey = null;
        if (u.getEncryptedApiKey() != null) apiKey = vault.decrypt(u.getEncryptedApiKey());

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String endpoint = pick.getBaseUrl();
        if (!endpoint.endsWith("/")) endpoint += "/";
        endpoint += "call/" + tool;
        HttpRequest.Builder reqb = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload == null ? "{}" : jsonPayload));
        if (apiKey != null) reqb.header("Authorization", "Bearer " + apiKey);

        HttpResponse<String> resp = client.send(reqb.build(), HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }
}
