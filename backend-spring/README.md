# MCP Gateway Backend — Spring Boot

A comprehensive REST API for managing a multi-tenant MCP (Model Context Protocol) server gateway with enterprise-grade security, encryption, and audit logging.

## 🎯 Features

✅ **Multi-Tenant Architecture**
- Organization-based data isolation
- User assignment to organizations  
- Tenant-aware API endpoints

✅ **Security & Encryption**
- AES-256-GCM encrypted API keys (VaultService)
- Per-user encrypted credentials storage
- Master key management via environment variables

✅ **Global Safety Policy**
- Rate limiting: 100 calls/60s per user
- Comprehensive audit logging
- Per-call performance metrics
- Error tracking and reporting

✅ **Enterprise API**
- RESTful endpoints with OpenAPI/Swagger documentation
- Organizations, Users, MCP Servers, Tool Schemas management
- Tool schema registry with dynamic form generation
- Audit log querying and analytics

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0
- Environment variable: `MCP_GATEWAY_MASTER_KEY` (Base64-encoded 32-byte key)

### Setup & Run

1. **Create Database**
   ```bash
   mysql -u root -p
   CREATE DATABASE mcp_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   EXIT;
   ```

2. **Configure Backend**
   Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mcp_gateway?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Set Master Key**
   ```bash
   # Windows PowerShell
   $env:MCP_GATEWAY_MASTER_KEY = "your-base64-encoded-32-byte-key"
   
   # Or generate one:
   [System.Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))
   ```

4. **Build & Run**
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

5. **Access API Documentation**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI JSON: http://localhost:8080/api-docs

## 📡 API Endpoints (Selection)

All endpoints (except user creation and schemas) require headers:
```
X-Org-Id: <organization_id>
X-User-Id: <user_id>
```

### Organizations
```
POST   /organizations                       # Create
GET    /organizations                       # List
GET    /organizations/{id}                  # Get
PUT    /organizations/{id}                  # Update
DELETE /organizations/{id}                  # Delete
```

### Users
```
POST   /users                               # Create
GET    /users/{id}                          # Get
POST   /users/{id}/apikey                   # Store encrypted key
GET    /users/{id}/apikey                   # Retrieve key
```

### Tool Schemas
```
POST   /schema                              # Create
GET    /schema/{tool}                       # Get by name
GET    /schema/all                          # List all
PUT    /schema/{id}                         # Update
DELETE /schema/{id}                         # Delete
```

### Tool Execution
```
POST   /proxy/{tool}/call                   # Execute tool (with proxy)
POST   /tools/{tool}/call                   # Execute tool (direct)
```

### Audit & Analytics
```
GET    /audit/logs/org/{orgId}              # Get org logs
GET    /audit/logs/user/{userId}            # Get user logs
GET    /audit/stats/org/{orgId}             # Get statistics
GET    /audit/logs/org/{orgId}/tool/{toolName}  # Tool-specific logs
GET    /audit/logs/org/{orgId}/range?startDate=...&endDate=...  # Date range
```

## 🔐 Security Details

### Encryption (VaultService)
```java
// Encrypts API keys using AES-256-GCM
byte[] encryptedKey = vaultService.encrypt(plainApiKey);

// Decrypts for use
String plainKey = vaultService.decrypt(encryptedKey);
```

**Features:**
- Algorithm: AES/GCM/NoPadding
- Key Size: 256-bit
- IV: Random 12-byte per encryption
- Authentication Tag: 128-bit
- Format: [IV_Length(4 bytes) + IV + Ciphertext]

### Multi-Tenancy Enforcement
- **AuthInterceptor**: Validates X-Org-Id and X-User-Id headers
- **RequestContext**: ThreadLocal storage for request auth data
- **Data Filtering**: All queries filtered by organization_id
- **Write Prevention**: Users can only set keys for their org's users

### Rate Limiting (SafetyService)
```
100 calls per user per 60-second sliding window
Status: SUCCESS, FAILED, or RATE_LIMITED
```

## 📊 Database Schema

**Organizations** → **Users** (many-to-one)
**Organizations** → **MCP Servers** (many-to-one)
**Organizations** → **Tool Schemas** (many-to-one, optional)
**Users** → **Task Audits** (one-to-many)

All audit queries indexed by `(organization_id, created_at)` for performance.

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run specific test
mvn test -Dtest=VaultServiceTest

# Run with coverage
mvn test jacoco:report
```

## 📈 Performance Optimization

- **Connection Pooling**: HikariCP configured automatically
- **Query Optimization**: Indexes on foreign keys and audit queries
- **Caching**: Spring Cache annotations ready for integration
- **Async Processing**: Rate-limit checks optimized with concurrent structures

## 🐳 Docker Deployment

```bash
# Build Docker image
docker build -t mcp-gateway .

# Run with Docker Compose
docker-compose up -d
```

## 🔍 Monitoring & Logging

- **Application Logs**: `backend-spring/application.log`
- **Call Logs**: `backend-spring/calls.log` (tab-delimited)
- **Structured Logging**: DEBUG level for detailed tracing
- **Metrics**: Performance metrics in audit logs (execution time)

## 🚨 Error Handling

| Status | Meaning |
|--------|---------|
| 401 | Missing/invalid auth headers |
| 403 | Organization mismatch (tenant isolation) |
| 429 | Rate limited |
| 503 | MCP server unavailable |
| 500 | Internal server error |

## 🔄 Future Enhancements

- [ ] JWT-based authentication
- [ ] Redis-based distributed rate limiting
- [ ] GraphQL API layer
- [ ] Webhook support for tool calls
- [ ] Request signing and verification
- [ ] Role-based access control (RBAC)
- [ ] Multi-region support

## 📚 Architecture Components

```
VaultService
├── encrypt(plaintext) → byte[]
└── decrypt(token) → plaintext

SafetyService  
├── allow(userId) → boolean
└── logCall(userId, orgId, tool, ...) → void

McpProxyService
└── proxyToolCall(userId, tool, payload) → response

Controllers
├── UserController
├── OrganizationController
├── SchemaController
├── ProxyController
└── AuditController
```

## ⚙️ Configuration

Key properties in `application.properties`:
```properties
spring.datasource.url=...
spring.jpa.hibernate.ddl-auto=update
logging.level.com.mcpgateway=DEBUG
springdoc.swagger-ui.enabled=true
```

## 📞 Support

For issues or questions, check the main `IMPLEMENTATION_GUIDE.md` for comprehensive documentation.

---

**Version**: 0.1.0 | **Java**: 17+ | **Spring Boot**: 3.2.0
