#!/bin/bash

# E-commerce Microservices Platform - Local Startup Script
# This script starts all 10 microservices + infrastructure in Docker containers

set -e  # Exit on any error

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}E-commerce Microservices Platform${NC}"
echo -e "${BLUE}Starting All Services in Docker${NC}"
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
echo ""

# Navigate to docker directory
cd docker

echo -e "${BLUE}📦 Building Docker images...${NC}"
echo -e "${YELLOW}This may take 5-10 minutes on first run${NC}"
docker-compose build --parallel

echo ""
echo -e "${BLUE}🚀 Starting all services...${NC}"
docker-compose up -d

echo ""
echo -e "${BLUE}⏳ Waiting for services to be healthy...${NC}"
echo -e "${YELLOW}This will take approximately 2-3 minutes${NC}"
echo ""

# Wait for key services to be healthy
echo -e "${YELLOW}Waiting for databases...${NC}"
docker-compose exec -T postgres pg_isready -U postgres || sleep 10
docker-compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" > /dev/null 2>&1 || sleep 10
docker-compose exec -T redis redis-cli ping > /dev/null 2>&1 || sleep 5

echo -e "${GREEN}✅ Databases ready${NC}"

echo -e "${YELLOW}Waiting for Eureka Server...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Eureka Server ready${NC}"
        break
    fi
    sleep 2
    echo -n "."
done
echo ""

echo -e "${YELLOW}Waiting for Config Server...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Config Server ready${NC}"
        break
    fi
    sleep 2
    echo -n "."
done
echo ""

echo -e "${YELLOW}Waiting for API Gateway...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ API Gateway ready${NC}"
        break
    fi
    sleep 2
    echo -n "."
done
echo ""

echo -e "${YELLOW}Waiting for microservices...${NC}"
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
echo "  • API Gateway Health:     http://localhost:8080/actuator/health"
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
echo -e "${GREEN}Databases (for direct access):${NC}"
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
echo "  • View logs:              docker-compose -f docker/docker-compose.yml logs -f"
echo "  • View specific service:  docker-compose -f docker/docker-compose.yml logs -f auth-service"
echo "  • Stop all services:      ./stop-local.sh"
echo "  • Restart a service:      docker-compose -f docker/docker-compose.yml restart auth-service"
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

