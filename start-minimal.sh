#!/bin/bash

# E-commerce Microservices Platform - Minimal Startup Script
# Only core services: Eureka, API Gateway, Auth Service, User Service
# Perfect for low-resource systems (4GB Docker RAM)

set -e  # Exit on any error

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}E-commerce Platform - Minimal Mode${NC}"
echo -e "${BLUE}5 Services + 2 Databases${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: Docker is not running${NC}"
    echo -e "${YELLOW}Please start Docker Desktop and try again${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker is running${NC}"
echo -e "${YELLOW}📊 Docker Resources:${NC}"
docker system info | grep -E "Memory|CPUs" | sed 's/^/ /'
echo ""

# Navigate to docker directory
cd docker

echo -e "${BLUE}📦 Building minimal services sequentially...${NC}"
echo -e "${YELLOW}This will take about 8-10 minutes on first run${NC}"
echo ""

# Build services one by one using minimal compose file
echo -e "${YELLOW}[1/5] Building Eureka Server...${NC}"
docker-compose -f docker-compose.minimal.yml build eureka-server

echo -e "${YELLOW}[2/5] Building Config Server...${NC}"
docker-compose -f docker-compose.minimal.yml build config-server

echo -e "${YELLOW}[3/5] Building API Gateway...${NC}"
docker-compose -f docker-compose.minimal.yml build api-gateway

echo -e "${YELLOW}[4/5] Building Auth Service...${NC}"
docker-compose -f docker-compose.minimal.yml build auth-service

echo -e "${YELLOW}[5/5] Building User Service...${NC}"
docker-compose -f docker-compose.minimal.yml build user-service

echo ""
echo -e "${GREEN}✅ All services built successfully!${NC}"
echo ""

echo -e "${BLUE}🚀 Starting services...${NC}"
docker-compose -f docker-compose.minimal.yml up -d

echo ""
echo -e "${BLUE}⏳ Waiting for services to be healthy...${NC}"
echo ""

# Wait for databases
echo -e "${YELLOW}Waiting for PostgreSQL & Redis...${NC}"
sleep 10
echo -e "${GREEN}✅ Databases ready${NC}"

# Wait for Eureka
echo -e "${YELLOW}Waiting for Eureka Server...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Eureka Server ready${NC}"
        break
    fi
    sleep 3
    echo -n "."
done
echo ""

# Wait for Config Server
echo -e "${YELLOW}Waiting for Config Server...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Config Server ready${NC}"
        break
    fi
    sleep 3
    echo -n "."
done
echo ""

# Wait for API Gateway
echo -e "${YELLOW}Waiting for API Gateway...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ API Gateway ready${NC}"
        break
    fi
    sleep 3
    echo -n "."
done
echo ""

# Wait for services to register
echo -e "${YELLOW}Waiting for services to register with Eureka...${NC}"
sleep 20

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Minimal setup ready!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

echo -e "${BLUE}📊 Running Services:${NC}"
echo ""
echo -e "${GREEN}Infrastructure:${NC}"
echo "  ✓ Eureka Dashboard:       http://localhost:8761"
echo "  ✓ Config Server:          http://localhost:8888"
echo "  ✓ API Gateway:            http://localhost:8080"
echo ""
echo -e "${GREEN}Microservices (via API Gateway):${NC}"
echo "  ✓ Auth Service:           http://localhost:8080/api/auth"
echo "  ✓ User Service:           http://localhost:8080/api/users"
echo ""
echo -e "${GREEN}Databases:${NC}"
echo "  ✓ PostgreSQL:             localhost:5432 (postgres/postgres)"
echo "  ✓ Redis:                  localhost:6379"
echo ""

echo -e "${BLUE}📝 Quick Test:${NC}"
echo ""
echo "1. Register a user:"
echo '   curl -X POST http://localhost:8080/api/auth/register \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{"username":"test","email":"test@example.com","password":"Test123!"}'"'"''
echo ""
echo "2. Login:"
echo '   curl -X POST http://localhost:8080/api/auth/login \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{"username":"test","password":"Test123!"}'"'"''
echo ""

echo -e "${BLUE}📋 Useful Commands:${NC}"
echo "  • View logs:              docker-compose -f docker/docker-compose.minimal.yml logs -f"
echo "  • Check status:           docker-compose -f docker/docker-compose.minimal.yml ps"
echo "  • Stop services:          docker-compose -f docker/docker-compose.minimal.yml down"
echo "  • Restart service:        docker-compose -f docker/docker-compose.minimal.yml restart auth-service"
echo ""

echo -e "${BLUE}💡 To Add More Services:${NC}"
echo "  Edit docker-compose.minimal.yml and add:"
echo "  - Product Service (MongoDB needed)"
echo "  - Order Service (Kafka recommended)"
echo "  - Payment Service"
echo ""

echo -e "${YELLOW}🌐 Opening Eureka Dashboard...${NC}"
sleep 2
if command -v open &> /dev/null; then
    open http://localhost:8761
elif command -v xdg-open &> /dev/null; then
    xdg-open http://localhost:8761
fi

echo ""
echo -e "${GREEN}🎉 Minimal setup complete!${NC}"
echo -e "${YELLOW}💾 Memory Usage: ~2-3GB (much lighter than full setup)${NC}"
echo ""

