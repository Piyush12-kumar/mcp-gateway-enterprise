@echo off
REM MCP Gateway - Windows Setup Script
REM This script helps set up the entire project on Windows

echo.
echo ========================================
echo MCP Gateway - Setup & Deployment Script
echo ========================================
echo.

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Docker is not installed or not in PATH
    echo Install Docker Desktop from https://www.docker.com/products/docker-desktop
    echo.
) else (
    echo ✓ Docker detected
)

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Docker Compose is not installed
    echo It usually comes with Docker Desktop on Windows
    echo.
) else (
    echo ✓ Docker Compose detected
)

REM Check if Java is installed (for local development)
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Java is not installed or not in PATH
    echo Install Java 17+ from https://www.oracle.com/java/technologies/downloads/
    echo.
) else (
    java -version
    echo ✓ Java detected
)

REM Check if Maven is installed (for local development)
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Maven is not installed or not in PATH
    echo Install Maven from https://maven.apache.org/download.cgi
    echo.
) else (
    echo ✓ Maven detected
)

echo.
echo ========================================
echo Setup Options
echo ========================================
echo.
echo 1. Quick Start with Docker (Recommended)
echo 2. Setup for Local Development
echo 3. View Logs
echo 4. Stop All Services
echo 5. Exit
echo.

set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" goto docker_start
if "%choice%"=="2" goto local_setup
if "%choice%"=="3" goto view_logs
if "%choice%"=="4" goto stop_services
if "%choice%"=="5" goto end

echo Invalid choice. Exiting.
goto end

:docker_start
echo.
echo Starting MCP Gateway with Docker Compose...
echo.
docker-compose up -d
echo.
echo Waiting for services to start...
timeout /t 10 /nobreak
echo.
echo ✓ Services started!
echo.
echo Access points:
echo   Frontend:    http://localhost
echo   Backend API: http://localhost:8080
echo   Swagger UI:  http://localhost:8080/swagger-ui.html
echo.
pause
goto end

:local_setup
echo.
echo Local Development Setup
echo ========================================
echo.
echo Prerequisites:
echo   - Java 17+
echo   - Maven 3.9+
echo   - MySQL 8.0
echo.
echo Step 1: Create MySQL Database
echo   Run in MySQL shell:
echo   CREATE DATABASE mcp_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
echo.
echo Step 2: Set Master Encryption Key
echo.
set /p masterkey="Enter Base64 encrypt key (or press Enter for default): "
if "%masterkey%"=="" (
    set "masterkey=aGVsbG8gd29ybGQgdGhpcyBpcyBhIHRlc3Qga2V5IGZvciBkZXZlbG9wbWVudA=="
    echo Using development key...
)
setx MCP_GATEWAY_MASTER_KEY "%masterkey%"
echo Master key set!
echo.
echo Step 3: Build & Run Backend
echo.
cd backend-spring
echo Building...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    goto end
)
echo.
echo Starting backend...
call mvn spring-boot:run
goto end

:view_logs
echo.
echo Viewing Docker logs...
echo.
docker-compose logs -f
goto end

:stop_services
echo.
echo Stopping all services...
docker-compose down
echo Done!
echo.
pause
goto end

:end
echo.
echo Setup script completed.
echo For more information, see README.md and IMPLEMENTATION_GUIDE.md
echo.

