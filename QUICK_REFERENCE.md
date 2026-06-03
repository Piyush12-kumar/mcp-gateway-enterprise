# MCP Gateway - Quick Reference Card

## 🚀 30-Second Start

### Docker (Recommended)
```bash
cd "D:\InternShip ESkill"
docker-compose up -d
# Wait 10-15 seconds, then visit http://localhost
```

### Local Development
```bash
cd backend-spring
mvn spring-boot:run
# Backend runs on http://localhost:8080
# Visit frontend/index.html in browser
```

---

## 📡 API Quick Reference

### Authentication Headers (Required)
```
X-Org-Id: <org_id>
X-User-Id: <user_id>
```

### Essential Endpoints

**Organizations**
```
POST   /organizations           # Create org
GET    /organizations           # List all
GET    /organizations/{id}      # Get one
```

**Users**
```
POST   /users                   # Create user
POST   /users/{id}/apikey       # Store encrypted key
GET    /users/{id}              # Get user
```

**Tools**
```
POST   /schema                  # Create tool schema
GET    /schema/{tool}           # Get tool schema
POST   /proxy/{tool}/call       # Execute tool
```

**Audit**
```
GET    /audit/logs/org/{orgId}              # View logs
GET    /audit/stats/org/{orgId}             # Get stats
```

---

## 🗄️ Database Setup

### Create DB
```sql
CREATE DATABASE mcp_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Tables (Auto-Created)
- organizations
- users
- mcp_servers
- tool_schemas
- task_audits

---

## 🔐 Security Quick Guide

| Item | Details |
|------|---------|
| **Encryption** | AES-256-GCM for API keys |
| **Master Key** | Set `MCP_GATEWAY_MASTER_KEY` env var |
| **Rate Limit** | 100 calls/60s per user |
| **Multi-Tenancy** | X-Org-Id header enforced |
| **Audit** | Every call logged with status |

---

## 📁 File Structure

```
.
├── frontend/index.html              # Dashboard UI
├── backend-spring/
│   ├── pom.xml                      # Dependencies
│   ├── src/main/java/.../
│   │   ├── controller/              # REST endpoints
│   │   ├── model/                   # JPA entities
│   │   ├── repository/              # Data access
│   │   ├── service/                 # Business logic
│   │   └── web/                     # Interceptors
│   └── src/main/resources/
│       ├── application.properties   # Config
│       └── init-data.sql            # Sample data
├── docker-compose.yml               # Full stack
├── Dockerfile                       # Backend image
├── nginx.conf                       # Proxy config
├── IMPLEMENTATION_GUIDE.md          # Full docs
└── README.md                        # Overview
```

---

## 🧪 Testing Workflow

### 1. Create Organization
```bash
curl -X POST http://localhost:8080/organizations \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Org"}'
# Returns: {"id": 1, "name": "Test Org"}
```

### 2. Create User
```bash
curl -X POST http://localhost:8080/users \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "organizationId": 1}'
```

### 3. Store API Key (Encrypted)
```bash
curl -X POST http://localhost:8080/users/1/apikey \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"api_key": "secret-key-123"}'
```

### 4. Call Tool
```bash
curl -X POST http://localhost:8080/proxy/translate/call \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello", "targetLang": "es"}'
```

### 5. View Audit Logs
```bash
curl -X GET http://localhost:8080/audit/logs/org/1 \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1"
```

---

## 🐛 Troubleshooting

| Problem | Solution |
|---------|----------|
| MySQL connection error | Wait 15s for MySQL startup; check `docker-compose ps` |
| Port 8080/80 in use | Stop conflicting service or change ports in docker-compose.yml |
| Master key not set | Use: `docker-compose up -d` or set `MCP_GATEWAY_MASTER_KEY` |
| Build fails | Run `mvn clean` then `mvn compile` |
| Database not created | HibernateΝDL=update auto-creates tables on startup |

---

## 📊 Default Sample Data

When running with dev profile, auto-loads:

**Organizations**
- Acme Corporation (ID: 1)
- TechStart Inc (ID: 2)
- Global Enterprises (ID: 3)

**Users**
- john.doe (Org: 1)
- jane.smith (Org: 1)
- bob.wilson (Org: 2)

**Tool Schemas (Global)**
- translate
- summarize
- sentiment

**MCP Servers**
- Primary (http://localhost:3000)
- Backup (http://localhost:3001)

---

## 🔑 Environment Variables

```bash
# Encryption Key (Base64 32-byte)
MCP_GATEWAY_MASTER_KEY=aGVs...==

# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/mcp_gateway
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=secret

# Profile
SPRING_PROFILES_ACTIVE=dev
```

---

## 📱 Dashboard Tabs

1. **Dashboard**: Stats & authentication setup
2. **Tools**: Manage schemas, call tools
3. **Users**: Create users, set API keys
4. **Organizations**: Manage tenants
5. **Audit Logs**: View calls, statistics

---

## 🎯 Key Features Checklist

- ✅ Multi-tenant architecture
- ✅ AES-256-GCM encryption
- ✅ Rate limiting (100 calls/60s)
- ✅ Audit logging & statistics
- ✅ Dynamic form generation
- ✅ Organization isolation
- ✅ RESTful API
- ✅ Swagger/OpenAPI docs
- ✅ Docker deployment
- ✅ MySQL persistence

---

## 📖 Documentation

- **Full Guide**: IMPLEMENTATION_GUIDE.md
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Backend Docs**: backend-spring/README.md
- **Code Comments**: Inline in source files

---

## 🚀 Production Prep

- [ ] Use KMS for master key
- [ ] Enable HTTPS/TLS
- [ ] Implement JWT auth
- [ ] Setup Redis for distributed rate limit
- [ ] Configure backups
- [ ] Set up monitoring
- [ ] Load test (1000+ users)
- [ ] Security audit
- [ ] API rate limiting at gateway

---

**Version**: 0.1.0  
**Last Updated**: June 2026  
**Status**: ✅ Production Ready Prototype

