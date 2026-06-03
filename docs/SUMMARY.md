Summary — VaultService & SafetyService

VaultService (backend-spring/src/main/java/com/mcpgateway/service/VaultService.java):
- Purpose: securely store per-user API keys used to call downstream MCP servers.
- Implementation: AES-GCM symmetric encryption. The service reads a Base64 master key from the environment variable `MCP_GATEWAY_MASTER_KEY`. If not present, the prototype generates an ephemeral key (not for production).
- Methods: `encrypt(String)` returns a byte[] token that contains IV + ciphertext; `decrypt(byte[])` returns the plaintext API key.
- Notes: In production you should use a managed KMS, rotate keys, and avoid storing plaintext master keys on disk.

SafetyService (backend-spring/src/main/java/com/mcpgateway/service/SafetyService.java):
- Purpose: globally enforce safety policies for tool calls: logging and basic rate-limiting per user.
- Implementation: in-memory sliding window rate limiter keyed by user id (max 20 calls / 60s). Calls are also appended to `backend-spring/calls.log` for audit.
- Methods: `allow(userId)` returns boolean; `logCall(userId, tool, payload)` writes an audit line.
- Notes: For production, replace in-memory structures with distributed rate-limiter (Redis, token bucket) and send logs to centralized observability.

Running the prototype
1. Configure MySQL and create database `mcp_gateway`. Update `backend-spring/src/main/resources/application.properties`.
2. Build and run the Spring Boot app:

```bash
cd backend-spring
mvn -U spring-boot:run
```

3. Use the sample frontend: open `frontend/index.html` in a browser and use the schema demo. The backend schema endpoint is at `GET /schema/{tool}`.

Security & Next steps
- Add JWT-based authentication and role checks.
- Use a KMS for the vault master key, and store secrets in DB encrypted at rest.
- Add MCP proxy implementation to forward calls to registered MCP servers, using per-user API keys.
