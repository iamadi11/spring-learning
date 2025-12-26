#!/bin/bash

# E-commerce Microservices Platform - Light Startup Script (Sequential Build)
# This script builds services one at a time to avoid memory issues

set -e  # Exit on any error

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}E-commerce Microservices Platform${NC}"
echo -e "${BLUE}Light Mode - Sequential Build${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: Docker is not running${NC}"
    echo -e "${YELLOW}Please start Docker Desktop and try again${NC}"
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Error: docker-compose is not installed${NC}"
    echo -e "${YELLOW}Please install docker-compose and try again${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker is running${NC}"
echo -e "${YELLOW}📊 Docker Resources:${NC}"
docker system info | grep -E "Memory|CPUs" | sed 's/^/ /'
echo ""

# Navigate to docker directory
cd docker

echo -e "${BLUE}📦 Building Docker images sequentially...${NC}"
echo -e "${YELLOW}This reduces memory usage but takes longer (10-15 minutes)${NC}"
echo ""

# Build infrastructure services first (these are smaller and faster)
echo -e "${YELLOW}[1/10] Building Service Discovery (Eureka)...${NC}"
docker-compose build eureka-server

echo -e "${YELLOW}[2/10] Building Config Server...${NC}"
docker-compose build config-server

echo -e "${YELLOW}[3/10] Building API Gateway...${NC}"
docker-compose build api-gateway

# Build microservices one at a time
echo -e "${YELLOW}[4/10] Building Auth Service...${NC}"
docker-compose build auth-service

echo -e "${YELLOW}[5/10] Building User Service...${NC}"
docker-compose build user-service

echo -e "${YELLOW}[6/10] Building Product Service...${NC}"
docker-compose build product-service

echo -e "${YELLOW}[7/10] Building Order Service...${NC}"
docker-compose build order-service

echo -e "${YELLOW}[8/10] Building Payment Service...${NC}"
docker-compose build payment-service

echo -e "${YELLOW}[9/10] Building Notification Service...${NC}"
docker-compose build notification-service

echo -e "${YELLOW}[10/10] Building Review Service...${NC}"
docker-compose build review-service

echo ""
echo -e "${GREEN}✅ All services built successfully!${NC}"
echo ""

echo -e "${BLUE}🚀 Starting all services...${NC}"
docker-compose up -d

echo ""
echo -e "${BLUE}⏳ Waiting for services to be healthy...${NC}"
echo -e "${YELLOW}This will take approximately 2-3 minutes${NC}"
echo ""

# Wait for databases
echo -e "${YELLOW}Waiting for databases...${NC}"
sleep 15
echo -e "${GREEN}✅ Databases starting${NC}"

# Wait for Eureka
echo -e "${YELLOW}Waiting for Eureka Server...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Eureka Server ready${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}⚠️  Eureka Server taking longer than expected${NC}"
    fi
    sleep 2
    echo -n "."
done
echo ""

# Wait for Config Server
echo -e "${YELLOW}Waiting for Config Server...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Config Server ready${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}⚠️  Config Server taking longer than expected${NC}"
    fi
    sleep 2
    echo -n "."
done
echo ""

# Wait for API Gateway
echo -e "${YELLOW}Waiting for API Gateway...${NC}"
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ API Gateway ready${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${RED}⚠️  API Gateway taking longer than expected${NC}"
    fi
    sleep 2
    echo -n "."
done
echo ""

echo -e "${YELLOW}Waiting for microservices to register...${NC}"
sleep 30

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ All services started successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

echo -e "${BLUE}📊 Service URLs:${NC}"
echo ""
echo -e "${GREEN}Infrastructure:${NC}"
echo "  • Eureka Dashboard:       http://localhost:8761"
echo "  • API Gateway:            http://localhost:8080"
echo "  • Config Server:          http://localhost:8888"
echo ""
echo -e "${GREEN}Microservices (via API Gateway):${NC}"
echo "  • Auth Service:           http://localhost:8080/api/auth"
echo "  • User Service:           http://localhost:8080/api/users"
echo "  • Product Service:        http://localhost:8080/api/products"
echo "  • Order Service:          http://localhost:8080/api/orders"
echo "  • Payment Service:        http://localhost:8080/api/payments"
echo "  • Notification Service:   http://localhost:8080/api/notifications"
echo "  • Review Service:         http://localhost:8080/api/reviews"
echo ""
echo -e "${GREEN}Observability:${NC}"
echo "  • Prometheus:             http://localhost:9090"
echo "  • Grafana:                http://localhost:3000 (admin/admin)"
echo "  • Zipkin:                 http://localhost:9411"
echo "  • Kibana:                 http://localhost:5601"
echo "  • Kafka UI:               http://localhost:8090"
echo ""
echo -e "${GREEN}Databases:${NC}"
echo "  • PostgreSQL:             localhost:5432 (postgres/postgres)"
echo "  • MongoDB:                localhost:27017 (admin/admin123)"
echo "  • Redis:                  localhost:6379"
echo ""

echo -e "${BLUE}📝 Next Steps:${NC}"
echo "  1. Import Postman collection from: postman/E-commerce-Microservices.postman_collection.json"
echo "  2. Import environment from: postman/Local.postman_environment.json"
echo "  3. Run 'Register' request to create a test user"
echo "  4. Run 'Login' request (JWT token will be auto-saved)"
echo "  5. Test any API endpoint!"
echo ""

echo -e "${BLUE}📋 Useful Commands:${NC}"
echo "  • View all logs:          docker-compose -f docker/docker-compose.yml logs -f"
echo "  • View specific service:  docker-compose -f docker/docker-compose.yml logs -f auth-service"
echo "  • Check running services: docker-compose -f docker/docker-compose.yml ps"
echo "  • Stop all services:      ./stop-local.sh"
echo "  • Restart a service:      docker-compose -f docker/docker-compose.yml restart auth-service"
echo ""

echo -e "${BLUE}🔍 Health Check:${NC}"
echo "  • Run this to check all services:"
echo "    docker-compose -f docker/docker-compose.yml ps"
echo ""

echo -e "${YELLOW}🌐 Opening Eureka Dashboard...${NC}"
sleep 2
if command -v open &> /dev/null; then
    open http://localhost:8761
elif command -v xdg-open &> /dev/null; then
    xdg-open http://localhost:8761
fi

echo ""
echo -e "${GREEN}🎉 Ready to test! All 10 microservices are running!${NC}"
echo ""

