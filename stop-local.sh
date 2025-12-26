#!/bin/bash

# E-commerce Microservices Platform - Local Shutdown Script
# This script stops all running Docker containers

set -e  # Exit on any error

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}E-commerce Microservices Platform${NC}"
echo -e "${BLUE}Stopping All Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Navigate to docker directory
cd docker

echo -e "${YELLOW}🛑 Stopping all services...${NC}"
docker-compose down

echo ""
echo -e "${GREEN}✅ All services stopped${NC}"
echo ""

# Ask if user wants to remove volumes (data)
read -p "$(echo -e ${YELLOW}Do you want to remove all data volumes? \(y/N\): ${NC})" -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}🗑️  Removing all volumes and data...${NC}"
    docker-compose down -v
    echo -e "${GREEN}✅ All volumes removed${NC}"
else
    echo -e "${BLUE}ℹ️  Data volumes preserved${NC}"
    echo -e "${BLUE}   To remove manually: docker-compose -f docker/docker-compose.yml down -v${NC}"
fi

echo ""
echo -e "${BLUE}📊 Container Status:${NC}"
docker ps -a --filter "name=ecommerce-" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Shutdown complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}To start again: ./start-local.sh${NC}"
echo ""

