# MCP Gateway - Ultimate Startup Guide

## ⚡ Choose Your Path

### Path 1: Docker Deploy (⏱️ 5 minutes) ⭐ RECOMMENDED

**Windows**
```batch
cd D:\InternShip ESkill
docker-compose up -d
```

**Linux/Mac**
```bash
cd "D:\InternShip ESkill"  # or wherever you cloned it
docker-compose up -d
```

**Then wait 10-15 seconds for MySQL to initialize**

**Access Points:**
- 🌐 Frontend: http://localhost
- 🔌 Backend API: http://localhost:8080
- 📖 Swagger Docs: http://localhost:8080/swagger-ui.html

✅ **That's it! Everything is running!**

---

### Path 2: Local Development (⏱️ 15 minutes)

#### Prerequisites Check
```bash
# Windows PowerShell
java -version              # Should be Java 17+
mvn --version              # Should be Maven 3.9+
mysql --version            # Should be MySQL 8.0
```

#### Step 1: Create Database
```bash
mysql -u root -p
```
```sql
CREATE DATABASE mcp_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

#### Step 2: Set Master Encryption Key (Windows PowerShell)
```powershell
# Generate a key:
$key = [System.Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))
Write-Host $key
# Copy the output

# Set environment variable:
$env:MCP_GATEWAY_MASTER_KEY = "paste-your-key-here"
```

**For Linux/Mac:**
```bash
export MCP_GATEWAY_MASTER_KEY=$(openssl rand -base64 32)
```

#### Step 3: Update Database Config
Edit: `backend-spring/src/main/resources/application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mcp_gateway?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

#### Step 4: Build Backend
```bash
cd backend-spring
mvn clean package -DskipTests
```

#### Step 5: Run Backend
```bash
mvn spring-boot:run
```

Backend will be at: http://localhost:8080

#### Step 6: Run Frontend
**Option A: Direct in Browser**
```
Open: D:\InternShip ESkill\frontend\index.html
```

**Option B: Local Server (Python)**
```bash
cd frontend
python -m http.server 8000
# Then open: http://localhost:8000
```

**Option C: Local Server (Node)**
```bash
cd frontend
npx http-server
# Then open: http://localhost:8080
```

---

### Path 3: Manual Docker Build (⏱️ 20 minutes)

#### Build Backend Image
```bash
cd backend-spring
mvn clean package

# Copy JAR to project root:
cd ..
cp backend-spring/target/backend-spring-0.1.0.jar .

# Build Docker image:
docker build -t mcp-gateway:latest .
```

#### Run Manually
```bash
# Start MySQL
docker run -d --name mcp-mysql \
  -e MYSQL_ROOT_PASSWORD=secret \
  -e MYSQL_DATABASE=mcp_gateway \
  -p 3306:3306 \
  mysql:8.0

# Wait for MySQL (10-15 seconds)
sleep 15

# Start Backend
docker run -d \
  --name mcp-backend \
  -p 8080:8080 \
  --link mcp-mysql:mysql \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/mcp_gateway \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  mcp-gateway:latest

# Start Frontend (Nginx)
docker run -d \
  --name mcp-frontend \
  -p 80:80 \
  -v $(pwd)/frontend:/usr/share/nginx/html:ro \
  nginx:alpine
```

---

## 🧪 Verify Everything Works

### 1. Check Services Running
```bash
# Docker services
docker-compose ps

# Or manually started
docker ps
```

### 2. Test Backend API
```bash
curl -X GET http://localhost:8080/organizations \
  -H "X-Org-Id: 1" \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
[
  {"id": 1, "name": "Acme Corporation"},
  {"id": 2, "name": "TechStart Inc"},
  {"id": 3, "name": "Global Enterprises"}
]
```

### 3. Test Frontend
Open: http://localhost (or http://localhost:8000 if using local server)

**You should see:**
- Navigation tabs: Dashboard, Tools, Users, Organizations, Audit
- Dashboard with statistics
- Organization ID/User ID fields pre-filled

### 4. Test Swagger UI
Open: http://localhost:8080/swagger-ui.html

You should see all 21 API endpoints listed and documented.

---

## 🚀 First Time Usage

### Create an Organization
1. Click **Organizations** tab
2. Enter name: "My Test Company"
3. Click **Create Organization**
4. Note the ID (should be 4)

### Create a User
1. Click **Users** tab
2. Enter:
   - Username: "testuser"
   - Organization ID: "4"
3. Click **Create User**

### Store API Key
1. Enter User ID: "1" (or the one you just created)
2. Enter API Key: "sk-test-key-123"
3. Click **Save API Key**
4. Key is now encrypted with AES-256-GCM

### Create Tool Schema
1. Click **Tools** tab
2. Fill in:
   - Tool Name: "my_translator"
   - Title: "My Translator"
   - Description: "Translations"
   - Schema JSON:
   ```json
   {
     "fields": [
       {"name": "text", "type": "textarea", "label": "Text"},
       {"name": "lang", "type": "text", "label": "Language"}
     ]
   }
   ```
3. Click **Create Schema**

### Call a Tool
1. Select tool from dropdown
2. Fill in the form
3. Click **Call Tool**
4. See result in the Result box

### View Audit Logs
1. Click **Audit Logs** tab
2. Enter Organization ID: "1"
3. Click **Load Logs**
4. See all tool calls logged with:
   - User ID
   - Tool Name
   - Status (SUCCESS/FAILED/RATE_LIMITED)
   - Execution Time
   - Timestamp

---

## 🛑 Stop Services

### Docker
```bash
docker-compose down

# Or remove everything:
docker-compose down -v
rm -rf mysql_data
```

### Local (Ctrl+C in each terminal)
- Stop Backend: Ctrl+C in backend terminal
- Stop Frontend: Ctrl+C in frontend server terminal
- Stop MySQL: manually or via MySQL utilities

---

## 🔍 Check Logs

### Docker Logs
```bash
# All services
docker-compose logs

# Specific service
docker-compose logs backend     # Backend logs
docker-compose logs mysql       # Database logs
docker-compose logs frontend    # Nginx logs

# Follow logs
docker-compose logs -f backend
```

### Local Logs
- Backend: `backend-spring/application.log`
- Calls: `backend-spring/calls.log`

---

## 🔧 Configuration

### Master Encryption Key
```bash
# Generate secure key:
openssl rand -base64 32    # Linux/Mac
# On Windows PowerShell: (see Step 2 above)

# Set environment variable:
export MCP_GATEWAY_MASTER_KEY="your-base64-key"
```

### Database Credentials
Edit: `backend-spring/src/main/resources/application.properties`
```properties
spring.datasource.url=...
spring.datasource.username=root
spring.datasource.password=secret
```

### Rate Limiting Configuration
Edit: `backend-spring/src/main/java/com/mcpgateway/service/SafetyService.java`
```java
private final int maxCalls = 100;      // Change this
private final int windowSec = 60;      // Or this
```

---

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| "Connection refused: localhost:3306" | Wait 10-15s for MySQL to start; check `docker-compose ps` |
| "Cannot connect to Docker daemon" | Ensure Docker Desktop is running |
| "Port 8080 already in use" | Kill process: `lsof -ti:8080 \| xargs kill` or change port |
| "Maven not found" | Install Maven or use Maven wrapper: `./mvnw` |
| "Java version error" | Upgrade to Java 17+: `java -version` |
| "Database exists error" | Drop database: `DROP DATABASE mcp_gateway;` then CREATE |
| "Frontend shows 404" | Clear browser cache; check backend is running |
| "Nginx proxy not working" | Ensure backend is running on port 8080 |

---

## 📊 Performance Testing

### Test Rate Limiting
```bash
# Make 101 calls to trigger rate limit:
for i in {1..101}; do
  curl -X POST http://localhost:8080/proxy/translate/call \
    -H "X-Org-Id: 1" \
    -H "X-User-Id: 1" \
    -H "Content-Type: application/json" \
    -d '{"text": "hello"}'
  echo ""
done
```

After 100 calls, should see: `{"error": "rate_limited"}`

### Test with 1000 Users
Create a script to:
1. POST /users with different usernames
2. Store different org IDs
3. Verify isolation

---

## 📈 Next Steps

After verifying everything works:

1. **Customize**: Modify tool schemas for your use case
2. **Deploy**: Use docker-compose for production
3. **Scale**: Add Redis for distributed rate limiting
4. **Secure**: Add JWT authentication
5. **Monitor**: Set up ELK or Splunk for logs

---

## 🎓 Learning Path

1. **First 5 min**: Start with Docker, see it work
2. **Next 10 min**: Create organizations, users, tools
3. **Next 10 min**: Call tools and view audit logs
4. **Next 15 min**: Read IMPLEMENTATION_GUIDE.md
5. **Next 20 min**: Explore source code, understand architecture
6. **Next 30 min**: Make a modification, rebuild, test

---

## 📞 Need Help?

- **API Issues**: Check `http://localhost:8080/swagger-ui.html`
- **Architecture**: Read `IMPLEMENTATION_GUIDE.md`
- **Quick Answers**: See `QUICK_REFERENCE.md`
- **File Structure**: See `FILE_MANIFEST.md`
- **Code Issues**: Check error logs in console/files

---

**Version**: 0.1.0  
**Status**: Production Ready  
**Expected Time to Running**: 5-15 minutes depending on path

