# 🚀 MCP Gateway - Enterprise Multi-Tenant AI Tool Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A production-ready, enterprise-grade gateway for managing **Model Context Protocol (MCP)** servers with complete multi-tenant isolation, advanced security, and cloud-native architecture.

---

## ✨ Key Features

| Category | Features |
|----------|----------|
| **🔐 Security** | JWT Authentication, RBAC (3-tier), AES-256-GCM Encryption |
| **⚡ Resilience** | Circuit Breaker, Retry with Backoff, Rate Limiting |
| **📊 Observability** | Prometheus Metrics, Comprehensive Audit Logging, Usage Metering |
| **🚀 Scalability** | Caffeine Caching, Pagination, Kafka Event Streaming |
| **🐳 Infrastructure** | Docker Multi-Stage Build, Kubernetes (HPA), GitHub Actions CI |

---

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Architecture](#-architecture)
- [API Endpoints](#-api-endpoints)
- [Security](#-security)
- [Configuration](#️-configuration)
- [Deployment](#-deployment)
- [Testing](#-testing)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)

---

## 🚀 Quick Start

### Prerequisites

- Docker Desktop (v20.10+)
- OR Java 17+ and Maven 3.8+ (for local development)

### Option 1: Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/mcp-gateway.git
cd mcp-gateway

# Start all services (MySQL + Backend + Frontend)
docker compose up -d

# Wait 30 seconds for initialization, then access:
# - Frontend Dashboard: http://localhost
# - API: http://localhost:8080
# - Swagger UI: http://localhost:8080/swagger-ui.html
```

### Option 2: Local Development

```bash
# 1. Start MySQL
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=mcp_gateway -p 3306:3306 mysql:8.0

# 2. Build and run backend
cd backend-spring
mvn clean package -DskipTests
java -jar target/*.jar

# 3. Open frontend/index.html in browser
```

### Default Login

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `orgadmin` | `orgadmin123` | ORG_ADMIN |
| `john.doe` | `password123` | USER |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MCP Gateway                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────┐    ┌─────────────────────────────────────────────────┐   │
│   │   Frontend   │───▶│              Spring Boot Backend               │   │
│   │   (Nginx)    │    │  ┌─────────────────────────────────────────┐   │   │
│   └──────────────┘    │  │  Security Layer (JWT + RBAC)            │   │   │
│                       │  └─────────────────────────────────────────┘   │   │
│                       │  ┌─────────────────────────────────────────┐   │   │
│                       │  │  Rate Limiter + Circuit Breaker         │   │   │
│                       │  └─────────────────────────────────────────┘   │   │
│                       │  ┌─────────────────────────────────────────┐   │   │
│                       │  │  MCP Proxy (JSON-RPC 2.0)               │───┼──▶ MCP Servers
│                       │  └─────────────────────────────────────────┘   │   │
│                       │  ┌───────────┐  ┌───────────┐  ┌───────────┐   │   │
│                       │  │  MySQL    │  │  Redis    │  │  Kafka    │   │   │
│                       │  │  (Data)   │  │  (Cache)  │  │  (Events) │   │   │
│                       │  └───────────┘  └───────────┘  └───────────┘   │   │
│                       └─────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Multi-Tenant Isolation

```
Organization A                    Organization B
┌─────────────────────┐          ┌─────────────────────┐
│  Users: A1, A2, A3  │          │  Users: B1, B2      │
│  Servers: S1, S2    │          │  Servers: S3, S4    │
│  Tools: T1, T2      │    ✗     │  Tools: T3          │
│  Audit: A's logs    │◄───────►│  Audit: B's logs    │
└─────────────────────┘  Isolated └─────────────────────┘
```

---

## 📡 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/login` | Login and get JWT token |
| `POST` | `/auth/register` | Register new user |

### Organizations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/organizations` | List all organizations |
| `POST` | `/organizations` | Create organization |
| `GET` | `/organizations/{id}` | Get organization by ID |

### MCP Servers

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/mcp/list` | List MCP servers |
| `POST` | `/mcp/register` | Register MCP server |
| `GET` | `/mcp/{serverId}/tools` | List tools from server |

### Tool Proxy

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/proxy/{toolName}/call` | Call a tool (with rate limiting) |

### Audit & Usage

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/audit/logs/org/{id}` | Get audit logs |
| `GET` | `/audit/stats/org/{id}` | Get statistics |
| `GET` | `/audit/usage/org/{id}` | Get usage/billing data |

📖 **Full API documentation**: [Swagger UI](http://localhost:8080/swagger-ui.html)

---

## 🔐 Security

### JWT Authentication

```http
POST /auth/login
Content-Type: application/json

{"username": "admin", "password": "admin123"}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "organizationId": 1,
  "role": "ADMIN"
}
```

Use token in subsequent requests:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| **ADMIN** | Full access to all organizations |
| **ORG_ADMIN** | Manage own organization |
| **USER** | Read-only, can call tools |

### AES-256-GCM Encryption

API keys are encrypted at rest using:
- **Algorithm**: AES-256-GCM (Authenticated Encryption)
- **IV**: Random 12-byte per encryption
- **Authentication Tag**: 128-bit for integrity

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | MySQL connection URL | `jdbc:mysql://mysql:3306/mcp_gateway` |
| `JWT_SECRET` | JWT signing secret | `mcp-gateway-secret-key` |
| `AES_MASTER_KEY` | Encryption master key (32 bytes, base64) | Auto-generated |
| `RATE_LIMIT_CALLS` | Max calls per window | `100` |
| `RATE_LIMIT_WINDOW_SECONDS` | Rate limit window | `60` |
| `FEATURES_REDIS_ENABLED` | Enable Redis rate limiting | `false` |
| `FEATURES_KAFKA_ENABLED` | Enable Kafka events | `false` |

### application.properties

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/mcp_gateway
spring.jpa.hibernate.ddl-auto=update

# Rate Limiting
rate.limit.calls=100
rate.limit.window-seconds=60

# Circuit Breaker
resilience4j.circuitbreaker.instances.mcpProxy.sliding-window-size=10
resilience4j.circuitbreaker.instances.mcpProxy.failure-rate-threshold=50
```

---

## 🐳 Deployment

### Docker Compose

```bash
# Basic deployment (MySQL + Backend + Frontend)
docker compose up -d

# Full stack (+ Redis, Kafka, Prometheus, Grafana)
docker compose --profile full up -d
```

### Kubernetes

```bash
# Deploy to Kubernetes
kubectl apply -f k8s/

# Check status
kubectl get all -n mcp-gateway

# Access via port-forward
kubectl port-forward -n mcp-gateway svc/mcp-gateway-backend 8080:8080
```

### Health Checks

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health |
| `/actuator/health/liveness` | Liveness probe |
| `/actuator/health/readiness` | Readiness probe |
| `/actuator/prometheus` | Prometheus metrics |

---

## 🧪 Testing

### Run Tests

```bash
cd backend-spring

# Unit tests
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify -P integration-tests
```

### Test Coverage

| Component | Test Type |
|-----------|-----------|
| `VaultService` | Unit tests for encryption/decryption |
| `SafetyService` | Unit tests for rate limiting |
| `AuthController` | Integration tests for JWT flow |
| `AuditController` | Integration tests for logging |

---

## 📁 Project Structure

```
mcp-gateway/
├── backend-spring/
│   ├── src/main/java/com/mcpgateway/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST controllers
│   │   ├── model/            # JPA entities
│   │   ├── repository/       # Data repositories
│   │   ├── security/         # JWT, RBAC, filters
│   │   ├── service/          # Business logic
│   │   │   ├── ratelimit/    # Rate limiting implementations
│   │   │   └── events/       # Kafka event publishers
│   │   └── web/              # Interceptors, context
│   └── src/test/             # Test classes
├── frontend/
│   └── index.html            # Single-page dashboard
├── k8s/                      # Kubernetes manifests
├── monitoring/               # Prometheus config
├── docker-compose.yml        # Docker orchestration
├── Dockerfile                # Multi-stage build
└── README.md                 # This file
```

---

## 📊 Monitoring

### Prometheus Metrics

Access metrics at `/actuator/prometheus`:

```promql
# Success rate of tool calls
sum(rate(mcp_tool_calls_total{status="SUCCESS"}[5m])) / 
sum(rate(mcp_tool_calls_total[5m])) * 100

# Circuit breaker state
resilience4j_circuitbreaker_state{state="open"} == 1

# Rate limited requests
rate(mcp_tool_calls_total{status="RATE_LIMITED"}[5m])
```

### Grafana Dashboard

When using `--profile full`, access Grafana at `http://localhost:3001`:
- Username: `admin`
- Password: `admin`

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Piyush Kumar**
- GitHub: [@Piyush12-kumar](https://github.com/Piyush12-kumar)
- LinkedIn: [Piyush Kumar](https://www.linkedin.com/in/piyush-kumar-a365342a7/)

---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Backend framework
- [Resilience4j](https://resilience4j.readme.io/) - Fault tolerance
- [JJWT](https://github.com/jwtk/jjwt) - JWT implementation
- [BellSoft Liberica JDK](https://bell-sw.com/libericajdk/) - Container-optimized JDK
