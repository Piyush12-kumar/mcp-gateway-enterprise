#!/bin/bash
# MCP Gateway - Linux/Mac Setup Script
# This script helps set up the entire project on Linux/Mac systems

set -e

echo ""
echo "========================================"
echo "MCP Gateway - Setup & Deployment Script"
echo "========================================"
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    echo "   Install from: https://www.docker.com/products/docker-desktop"
else
    echo "✓ Docker detected: $(docker --version)"
fi

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed"
else
    echo "✓ Docker Compose detected: $(docker-compose --version)"
fi

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed"
else
    echo "✓ Java detected:"
    java -version 2>&1 | head -n 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed"
else
    echo "✓ Maven detected: $(mvn --version 2>/dev/null | head -n 1)"
fi

echo ""
echo "========================================"
echo "MCP Gateway - Quick Start Options"
echo "========================================"
echo ""
echo "1. Docker Deploy (Recommended)"
echo "2. Local Development Setup"
echo "3. Build Backend Only"
echo "4. View Logs"
echo "5. Stop Services"
echo "6. Full Cleanup"
echo ""

read -p "Select option (1-6): " choice

case $choice in
    1)
        echo ""
        echo "🚀 Starting MCP Gateway with Docker..."
        docker-compose up -d
        echo ""
        echo "⏳ Waiting for services..."
        sleep 10
        echo ""
        echo "✅ Services started!"
        echo ""
        echo "Access points:"
        echo "  Frontend:    http://localhost"
        echo "  Backend API: http://localhost:8080"
        echo "  Swagger UI:  http://localhost:8080/swagger-ui.html"
        echo ""
        docker-compose ps
        ;;
    2)
        echo ""
        echo "📝 Local Development Setup"
        echo "========================================"
        echo ""
        echo "Prerequisites:"
        echo "  ✓ Java 17+"
        echo "  ✓ Maven 3.9+"
        echo "  ✓ MySQL 8.0"
        echo ""
        echo "Step 1: Create MySQL Database"
        echo "  mysql -u root -p"
        echo "  CREATE DATABASE mcp_gateway CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        echo ""
        read -p "Step 2: Enter Base64 master key (or press Enter for dev key): " masterkey
        if [ -z "$masterkey" ]; then
            masterkey="aGVsbG8gd29ybGQgdGhpcyBpcyBhIHRlc3Qga2V5IGZvciBkZXZlbG9wbWVudA=="
            echo "Using development key..."
        fi
        export MCP_GATEWAY_MASTER_KEY="$masterkey"
        echo ""
        echo "Step 3: Building backend..."
        cd backend-spring
        mvn clean package -DskipTests
        echo ""
        echo "Step 4: Starting backend..."
        mvn spring-boot:run
        ;;
    3)
        echo ""
        echo "🔨 Building backend..."
        cd backend-spring
        mvn clean package -DskipTests
        echo ""
        echo "✅ Build complete!"
        echo "JAR: target/backend-spring-0.1.0.jar"
        ;;
    4)
        echo ""
        echo "📊 Displaying logs..."
        docker-compose logs -f
        ;;
    5)
        echo ""
        echo "🛑 Stopping services..."
        docker-compose down
        echo "✅ Services stopped"
        ;;
    6)
        echo ""
        echo "🗑️  Full cleanup..."
        docker-compose down -v
        rm -rf mysql_data
        echo "✅ Cleanup complete"
        ;;
    *)
        echo "Invalid option"
        ;;
esac

echo ""

