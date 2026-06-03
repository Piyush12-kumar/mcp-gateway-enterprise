# MCP Gateway — Comprehensive Implementation Guide

## 📋 Project Overview

The Multi-Tenant Enterprise MCP Gateway is a sophisticated centralized web hub designed to manage 100+ MCP servers for thousands of users. It provides:

- **Multi-Tenant Architecture**: Isolated organizations with tenant-aware data filtering
- **Credential Vault**: AES-256-GCM encrypted per-user API keys
- **Dynamic UI**: Generated from JSON tool schemas
- **Safety Policy Agent**: Global rate-limiting (100 calls/60s per user), comprehensive audit logging
- **Enterprise Features**: Organizations, users, tools, audit trails, statistics

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.2.0 with Java 17+
- **Database**: MySQL 8.0 (Docker included)
- **Frontend**: HTML5 + Vanilla JavaScript with responsive design
- **Container**: Docker + Docker Compose
- **API Documentation**: Swagger/OpenAPI (available at `/swagger-ui.html`)

### System Components

```
┌─────────────────────────────────────────────────────────┐
│                     Frontend (Nginx)                    │
│        Dashboard, Tool Management, Audit Viewer        │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/REST
┌────────────────────▼────────────────────────────────────┐
│              Backend (Spring Boot)                      │
│  ┌──────────────┬──────────────┬──────────────────────┐│
│  │ Controllers  │  Services    │   Repositories      ││
│  │ • Users      │ • Vault      │ • User              ││
│  │ • Tools      │ • Safety     │ • Organization      ││
│  │ • Orgs       │ • Proxy      │ • MCP Server        ││
│  │ • Audit      │              │ • Tool Schema       ││
│  │ • Proxy      │              │ • Task Audit        ││
│  └──────────────┴──────────────┴──────────────────────┘│
└────────────────────┬────────────────────────────────────┘
                     │ SQL
┌────────────────────▼────────────────────────────────────┐
│                   MySQL Database                        │
│  (organizations, users, mcp_servers, tool_schemas,     │
│   task_audits, encrypted api keys)                     │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Clone/navigate to project directory
cd "D:\InternShip ESkill"

# Build and start all services
docker-compose up -d

# Wait for services to be healthy (MySQL takes ~10s)
# Access the application at http://localhost
```

The dashboard will be available at:
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html

### Option 2: Local Development

#### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0
- Node.js (optional, for frontend development)

#### Setup Steps

1. **Create MySQL Database**
   ```bash
   mysql -u root -p
   CREATE DATABASE mcp_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   EXIT;
   ```

2. **Configure Backend**
   ```bash
   cd backend-spring
   
   # Generate a secure master key for encryption (Base64 encoded 32-byte key)
   # On Windows PowerShell:
   [System.Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))
   
   # Set environment variable (Windows)
   $env:MCP_GATEWAY_MASTER_KEY = "your-base64-encoded-key"
   
   # Or add to application.properties:
   # vault.master-key=${MCP_GATEWAY_MASTER_KEY:ephemeral}
   ```

3. **Update Database Credentials**
   Edit `backend-spring/src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/mcp_gateway?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your-password
   ```

4. **Build and Run Backend**
   ```bash
   cd backend-spring
   mvn clean package -DskipTests
   mvn spring-boot:run
   
   # Or run the JAR
   java -jar target/backend-spring-0.1.0.jar
   ```

5. **Access Frontend**
   Open `frontend/index.html` in your browser

## 📚 API Endpoints

### Authentication
All requests (except `/users` POST and `/schema`) require headers:
```
X-Org-Id: <organization_id>
X-User-Id: <user_id>
```

### Core Endpoints

#### Organizations
```
POST   /organizations                      # Create org
GET    /organizations                      # List orgs
GET    /organizations/{id}                 # Get org
PUT    /organizations/{id}                 # Update org
DELETE /organizations/{id}                 # Delete org
```

#### Users
```
POST   /users                              # Create user
GET    /users/{id}                         # Get user
POST   /users/{id}/apikey                  # Set API key
GET    /users/{id}/apikey                  # Get API key
```

#### MCP Servers
```
POST   /mcp/register                       # Register MCP server
GET    /mcp/list                           # List servers (org-filtered)
```

#### Tool Schemas
```
POST   /schema                             # Create tool schema
GET    /schema/{tool}                      # Get tool schema
GET    /schema/all                         # List all tools
PUT    /schema/{id}                        # Update schema
DELETE /schema/{id}                        # Delete schema
```

#### Tool Execution
```
POST   /proxy/{tool}/call                  # Call tool (proxied)
POST   /tools/{tool}/call                  # Call tool (direct)
```

#### Audit & Logging
```
GET    /audit/logs/user/{userId}          # User logs
GET    /audit/logs/org/{orgId}            # Org logs
GET    /audit/logs/org/{orgId}/tool/{toolName}  # Tool-specific logs
GET    /audit/logs/org/{orgId}/range      # Date range query
GET    /audit/stats/org/{orgId}           # Org statistics
```

## 🔐 Security Features

### 1. Encryption (VaultService)
- **Algorithm**: AES-256-GCM (authenticated encryption)
- **Key Management**: Master key from environment variable `MCP_GATEWAY_MASTER_KEY`
- **Per-Request**: Random IV for each encryption
- **Storage**: Encrypted API keys stored in database BLOB column

### 2. Multi-Tenancy
- **Org Isolation**: All queries filtered by `X-Org-Id` header
- **Auth Enforcement**: AuthInterceptor validates headers
- **Data Segregation**: Users can only see their org's data

### 3. Rate Limiting (SafetyService)
- **Policy**: 100 tool calls per user per 60-second window
- **Implementation**: Sliding window with in-memory deque per user
- **Future**: Upgrade to Redis for distributed rate-limiting

### 4. Audit Logging
- **Logging Scope**: Every tool call is logged (user, org, tool, status, time, errors)
- **Storage**: Database + file (`calls.log`)
- **Query**: Full audit trail accessible via API

## 💾 Database Schema

### Organizations Table
```sql
CREATE TABLE organizations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL
);
```

### Users Table
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  encrypted_api_key LONGBLOB,
  organization_id BIGINT,
  FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
```

### MCP Servers Table
```sql
CREATE TABLE mcp_servers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255),
  base_url VARCHAR(500),
  organization_id BIGINT,
  FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
```

### Tool Schemas Table
```sql
CREATE TABLE tool_schemas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tool_name VARCHAR(255) UNIQUE NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  schema_json LONGTEXT NOT NULL,
  organization_id BIGINT,
  enabled BOOLEAN DEFAULT true,
  FOREIGN KEY (organization_id) REFERENCES organizations(id)
);
```

### Task Audits Table
```sql
CREATE TABLE task_audits (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  tool_name VARCHAR(255) NOT NULL,
  request_payload LONGTEXT,
  response_payload LONGTEXT,
  created_at DATETIME NOT NULL,
  status VARCHAR(50) NOT NULL,
  error_message TEXT,
  execution_time_ms BIGINT,
  FOREIGN KEY (organization_id) REFERENCES organizations(id),
  INDEX (organization_id, created_at),
  INDEX (user_id, created_at)
);
```

## 📊 Usage Examples

### 1. Create an Organization
```bash
curl -X POST http://localhost:8080/organizations \
  -H "Content-Type: application/json" \
  -d '{"name": "Acme Corp"}'
```

### 2. Create a User
```bash
curl -X POST http://localhost:8080/users \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"username": "john.doe", "organizationId": 1}'
```

### 3. Store Encrypted API Key
```bash
curl -X POST http://localhost:8080/users/1/apikey \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"api_key": "sk-1234567890abcdef"}'
```

### 4. Create a Tool Schema
```bash
curl -X POST http://localhost:8080/schema \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "translate",
    "title": "Translator",
    "description": "Translate text between languages",
    "schemaJson": "{\"fields\": [{\"name\": \"text\", \"type\": \"textarea\", \"label\": \"Text\"}, {\"name\": \"targetLang\", \"type\": \"text\", \"label\": \"Target Language\"}]}"
  }'
```

### 5. Call a Tool
```bash
curl -X POST http://localhost:8080/proxy/translate/call \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello world", "targetLang": "es"}'
```

### 6. View Audit Logs
```bash
curl -X GET http://localhost:8080/audit/logs/org/1 \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1"
```

### 7. Get Organization Statistics
```bash
curl -X GET http://localhost:8080/audit/stats/org/1 \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1"
```

## 🧪 Testing

### Unit Tests
```bash
cd backend-spring
mvn test
```

### Integration Testing
The project includes VaultServiceTest for encryption round-trip validation:
```bash
mvn test -Dtest=VaultServiceTest
```

## 📱 Frontend Features

The modern dashboard includes:

1. **Dashboard Tab**: Quick stats and authentication setup
2. **Tools Tab**: Create tool schemas, browse available tools, call tools with dynamic forms
3. **Users Tab**: Create users, set/retrieve API keys
4. **Organizations Tab**: Manage organizations
5. **Audit Logs Tab**: View audit trails, statistics, and call history

### UI Technologies
- Responsive grid layout
- Real-time form validation
- JSON result display
- Status indicators and alerts
- Local storage for headers (for convenience)

## 🔧 Configuration

### Environment Variables
```
MCP_GATEWAY_MASTER_KEY    # Base64-encoded 32-byte encryption key
SPRING_DATASOURCE_URL     # MySQL connection URL
SPRING_DATASOURCE_USERNAME # DB username
SPRING_DATASOURCE_PASSWORD # DB password
```

### application.properties
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/mcp_gateway?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=update

# Logging
logging.level.com.mcpgateway=DEBUG
logging.file.name=backend-spring/application.log

# Rate Limiting (in SafetyService)
# Currently: 100 calls / 60 seconds per user
```

## 📈 Production Recommendations

1. **Key Management**
   - Use AWS KMS or Vault for master key storage
   - Implement key rotation policy
   - Never commit keys to version control

2. **Rate Limiting**
   - Replace in-memory deque with Redis for distributed systems
   - Implement token bucket algorithm for flexible limits
   - Consider per-organization rate limits

3. **Logging & Monitoring**
   - Send logs to centralized system (ELK, Splunk, CloudWatch)
   - Implement distributed tracing (Jaeger, Zipkin)
   - Set up alerts for rate limit breaches

4. **Security**
   - Implement JWT-based authentication
   - Add HTTPS/TLS encryption
   - Use role-based access control (RBAC)
   - Implement request signing and verification

5. **Database**
   - Enable encryption at rest
   - Implement automated backups
   - Use read replicas for scale
   - Index audit tables for faster queries

6. **API**
   - Implement API versioning
   - Add request/response validation
   - Implement CORS properly
   - Consider GraphQL for complex queries

## 🐛 Troubleshooting

### MySQL Connection Issues
```
Error: Access denied for user 'root'@'localhost'
Solution: Check credentials in application.properties; ensure MySQL is running
```

### Encryption Errors
```
Error: MCP_GATEWAY_MASTER_KEY not set
Solution: Set environment variable with Base64-encoded 32-byte key
```

### Rate Limiting Too Strict
Edit `SafetyService.java`:
```java
private final int maxCalls = 100;  // Increase this value
private final int windowSec = 60;  // Or increase time window
```

### Docker Build Issues
```bash
# Clean build
docker-compose down
docker system prune -a
docker-compose up --build
```

## 📞 Support & Contribution

For issues, questions, or contributions, please follow the development guidelines in the code comments.

## 📄 License

This project is part of the internship assessment for ESkill.

---

**Version**: 0.1.0 | **Last Updated**: June 2026

