#!/bin/bash
# Deploy all services to Kubernetes

set -e  # Exit on error

echo "☸️  Deploying E-commerce Platform to Kubernetes..."
echo ""

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
NAMESPACE="${K8S_NAMESPACE:-ecommerce}"
CONTEXT="${K8S_CONTEXT:-minikube}"

echo -e "${BLUE}Namespace:${NC} $NAMESPACE"
echo -e "${BLUE}Context:${NC} $CONTEXT"
echo ""

# Set kubectl context
kubectl config use-context "$CONTEXT" 2>/dev/null || echo "Using default context"

# Create namespace
echo -e "${GREEN}Creating namespace...${NC}"
kubectl apply -f k8s/namespace.yaml

# Apply secrets (WARNING: use external secrets in production)
echo -e "${YELLOW}⚠️  Applying secrets (use external secrets manager in production)${NC}"
kubectl apply -f k8s/secrets.yaml

# Deploy services
echo -e "${GREEN}Deploying services...${NC}"

# Check if there are other k8s files
for file in k8s/*.yaml; do
    if [[ "$file" != "k8s/namespace.yaml" ]] && [[ "$file" != "k8s/secrets.yaml" ]]; then
        echo "Applying $file..."
        kubectl apply -f "$file"
    fi
done

# Wait for deployments
echo ""
echo -e "${GREEN}Waiting for deployments to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s \
  deployment/auth-service \
  deployment/order-service \
  -n "$NAMESPACE" 2>/dev/null || echo "Some deployments may still be starting..."

# Show status
echo ""
echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo "📊 Pod Status:"
kubectl get pods -n "$NAMESPACE"

echo ""
echo "🌐 Service Status:"
kubectl get services -n "$NAMESPACE"

echo ""
echo "🔗 Ingress Status:"
kubectl get ingress -n "$NAMESPACE"

echo ""
echo "📝 To view logs:"
echo "   kubectl logs -f deployment/order-service -n $NAMESPACE"
echo ""
echo "🔍 To check pod details:"
echo "   kubectl describe pod <pod-name> -n $NAMESPACE"
echo ""
echo "🗑️  To delete everything:"
echo "   kubectl delete namespace $NAMESPACE"

