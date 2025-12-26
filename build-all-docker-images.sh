#!/bin/bash
# Build all Docker images for the e-commerce platform

set -e  # Exit on error

echo "🐳 Building all Docker images for E-commerce Microservices Platform..."
echo ""

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Registry and tag
REGISTRY="${DOCKER_REGISTRY:-localhost:5000}"
TAG="${IMAGE_TAG:-latest}"

echo -e "${BLUE}Registry:${NC} $REGISTRY"
echo -e "${BLUE}Tag:${NC} $TAG"
echo ""

# Function to build and tag image
build_image() {
    local service=$1
    local context=$2
    local dockerfile=$3
    
    echo -e "${GREEN}Building $service...${NC}"
    docker build -t "$REGISTRY/$service:$TAG" -f "$dockerfile" "$context"
    echo -e "${GREEN}✓ Built $service${NC}"
    echo ""
}

# Build Infrastructure Services
echo "📦 Building Infrastructure Services..."
build_image "eureka-server" "." "infrastructure/service-discovery/Dockerfile"
build_image "config-server" "." "infrastructure/config-server/Dockerfile"
build_image "api-gateway" "." "infrastructure/api-gateway/Dockerfile"

# Build Business Services
echo "🛍️  Building Business Services..."
build_image "auth-service" "." "services/auth-service/Dockerfile"
build_image "user-service" "." "services/user-service/Dockerfile"
build_image "product-service" "." "services/product-service/Dockerfile"
build_image "order-service" "." "services/order-service/Dockerfile"
build_image "payment-service" "." "services/payment-service/Dockerfile"
build_image "notification-service" "." "services/notification-service/Dockerfile"
build_image "review-service" "." "services/review-service/Dockerfile"

echo ""
echo -e "${GREEN}✅ All images built successfully!${NC}"
echo ""
echo "📋 Available images:"
docker images | grep -E "(eureka-server|config-server|api-gateway|auth-service|user-service|product-service|order-service|payment-service|notification-service|review-service)" | head -n 10

echo ""
echo "🚀 To push images to registry:"
echo "   docker push $REGISTRY/auth-service:$TAG"
echo "   (repeat for each service)"
echo ""
echo "☸️  To deploy to Kubernetes:"
echo "   kubectl apply -f k8s/"

