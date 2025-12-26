# Deployment Guide - E-commerce Microservices Platform

## 🚀 Quick Start Deployment

This guide covers deploying the complete e-commerce microservices platform to production.

---

## Prerequisites

### Local Development
- **Java 21** (JDK)
- **Docker** 20.x+
- **Docker Compose** 2.x+
- **Gradle** 8.x (included via wrapper)

### Production Deployment
- **Kubernetes** 1.24+
- **kubectl** configured
- **Helm** 3.x+
- **Container Registry** (Docker Hub, GitHub Container Registry, etc.)

---

## 🐳 Option 1: Docker Compose (Local/Testing)

### Step 1: Start Infrastructure

```bash
cd docker
docker-compose up -d

# Verify all services are running
docker-compose ps

# View logs
docker-compose logs -f
```

**Services Started:**
- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Zipkin: `localhost:9411`
- Prometheus: `localhost:9090`
- Grafana: `localhost:3000`
- Kibana: `localhost:5601`

### Step 2: Build Services

```bash
# Build all services
./gradlew build

# Or build specific service
./gradlew :services:auth-service:build
```

### Step 3: Run Services

```bash
# Terminal 1: Eureka Server
./gradlew :infrastructure:service-discovery:bootRun

# Terminal 2: Config Server
./gradlew :infrastructure:config-server:bootRun

# Terminal 3: API Gateway
./gradlew :infrastructure:api-gateway:bootRun

# Terminal 4-10: Business Services
./gradlew :services:auth-service:bootRun
./gradlew :services:user-service:bootRun
./gradlew :services:product-service:bootRun
./gradlew :services:order-service:bootRun
./gradlew :services:payment-service:bootRun
./gradlew :services:notification-service:bootRun
./gradlew :services:review-service:bootRun
```

### Step 4: Verify

- Eureka Dashboard: http://localhost:8761
- API Gateway Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## 🐋 Option 2: Docker Images

### Build All Docker Images

```bash
# Using provided script
./build-all-docker-images.sh

# Or manually for each service
docker build -t ecommerce/auth-service:latest -f services/auth-service/Dockerfile .
docker build -t ecommerce/user-service:latest -f services/user-service/Dockerfile .
docker build -t ecommerce/product-service:latest -f services/product-service/Dockerfile .
docker build -t ecommerce/order-service:latest -f services/order-service/Dockerfile .
docker build -t ecommerce/payment-service:latest -f services/payment-service/Dockerfile .
docker build -t ecommerce/notification-service:latest -f services/notification-service/Dockerfile .
docker build -t ecommerce/review-service:latest -f services/review-service/Dockerfile .
```

### Push to Registry

```bash
# Tag for your registry
export REGISTRY="your-registry.com"
export TAG="1.0.0"

# Tag and push
docker tag ecommerce/auth-service:latest $REGISTRY/ecommerce/auth-service:$TAG
docker push $REGISTRY/ecommerce/auth-service:$TAG

# Repeat for all services...
```

---

## ☸️ Option 3: Kubernetes Deployment

### Prerequisites

```bash
# Verify kubectl is configured
kubectl version
kubectl cluster-info

# Verify Helm is installed
helm version
```

### Method A: Using kubectl directly

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Apply secrets (WARNING: use external secrets in production!)
kubectl apply -f k8s/secrets.yaml

# Deploy services
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/order-service.yaml
# ... apply other services

# Deploy ingress
kubectl apply -f k8s/ingress.yaml

# Verify deployment
kubectl get pods -n ecommerce
kubectl get services -n ecommerce
kubectl get ingress -n ecommerce
```

### Method B: Using Helm Chart

```bash
# Add Bitnami repository (for dependencies)
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Install with default values
helm install ecommerce ./helm/ecommerce \
  --namespace ecommerce \
  --create-namespace

# Install with custom values
helm install ecommerce ./helm/ecommerce \
  --namespace ecommerce \
  --create-namespace \
  --values ./helm/ecommerce/values-prod.yaml \
  --set postgresql.auth.password=secure-password

# Verify installation
helm list -n ecommerce
kubectl get pods -n ecommerce
```

### Method C: Using Deployment Script

```bash
# Deploy everything
./deploy-kubernetes.sh

# Or with custom namespace
K8S_NAMESPACE=production ./deploy-kubernetes.sh
```

---

## 🔄 CI/CD Deployment (GitHub Actions)

### Setup

1. **Configure Secrets** in GitHub repository:
   - `KUBE_CONFIG`: Base64 encoded kubeconfig file
   - `SLACK_WEBHOOK`: (Optional) Slack webhook for notifications
   - `REGISTRY_USERNAME`: Container registry username
   - `REGISTRY_PASSWORD`: Container registry password

2. **Push to main branch**:
```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

3. **Pipeline automatically**:
   - ✅ Builds all services
   - ✅ Runs tests
   - ✅ Scans for vulnerabilities
   - ✅ Builds Docker images
   - ✅ Pushes to registry
   - ✅ Deploys to Kubernetes
   - ✅ Sends notification

---

## 🔍 Monitoring & Observability

### Access Monitoring Tools

**Prometheus** (Metrics):
```bash
# Port forward
kubectl port-forward -n ecommerce svc/prometheus 9090:9090

# Access: http://localhost:9090
```

**Grafana** (Dashboards):
```bash
# Port forward
kubectl port-forward -n ecommerce svc/grafana 3000:3000

# Access: http://localhost:3000
# Default: admin/admin
```

**Zipkin** (Distributed Tracing):
```bash
# Port forward
kubectl port-forward -n ecommerce svc/zipkin 9411:9411

# Access: http://localhost:9411
```

**Kibana** (Logs):
```bash
# Port forward
kubectl port-forward -n ecommerce svc/kibana 5601:5601

# Access: http://localhost:5601
```

---

## 🔧 Troubleshooting

### Check Pod Status
```bash
# List all pods
kubectl get pods -n ecommerce

# Describe pod
kubectl describe pod <pod-name> -n ecommerce

# View logs
kubectl logs -f <pod-name> -n ecommerce

# Exec into pod
kubectl exec -it <pod-name> -n ecommerce -- /bin/sh
```

### Check Service Health
```bash
# Port forward to service
kubectl port-forward -n ecommerce svc/order-service 8089:8089

# Check health
curl http://localhost:8089/actuator/health

# Check metrics
curl http://localhost:8089/actuator/prometheus
```

### Common Issues

**Issue 1: Pods not starting**
```bash
# Check events
kubectl get events -n ecommerce --sort-by='.lastTimestamp'

# Check pod logs
kubectl logs <pod-name> -n ecommerce --previous
```

**Issue 2: Service not accessible**
```bash
# Check service endpoints
kubectl get endpoints -n ecommerce

# Check ingress
kubectl describe ingress ecommerce-ingress -n ecommerce
```

**Issue 3: Database connection failed**
```bash
# Check secrets
kubectl get secrets -n ecommerce

# Verify database pods
kubectl get pods -n ecommerce | grep postgres
```

---

## 🎯 Production Checklist

### Before Production Deployment

- [ ] **Security**
  - [ ] Use external secrets manager (AWS Secrets Manager, Vault)
  - [ ] Enable TLS/SSL certificates
  - [ ] Configure network policies
  - [ ] Set up RBAC
  - [ ] Scan images for vulnerabilities

- [ ] **Reliability**
  - [ ] Configure resource limits
  - [ ] Set up health checks
  - [ ] Configure horizontal pod autoscaling
  - [ ] Set up pod disruption budgets
  - [ ] Test disaster recovery

- [ ] **Monitoring**
  - [ ] Configure Prometheus alerts
  - [ ] Set up Grafana dashboards
  - [ ] Configure log aggregation
  - [ ] Set up distributed tracing
  - [ ] Configure uptime monitoring

- [ ] **Performance**
  - [ ] Load test the system
  - [ ] Optimize database queries
  - [ ] Configure caching
  - [ ] Set up CDN for static assets
  - [ ] Enable response compression

- [ ] **Backup & Recovery**
  - [ ] Automated database backups
  - [ ] Test restore procedures
  - [ ] Document RTO/RPO
  - [ ] Set up cross-region backup
  - [ ] Version control all configs

---

## 📊 Scaling

### Manual Scaling
```bash
# Scale deployment
kubectl scale deployment order-service --replicas=5 -n ecommerce

# Scale using Helm
helm upgrade ecommerce ./helm/ecommerce \
  --set orderService.replicaCount=5 \
  -n ecommerce
```

### Horizontal Pod Autoscaling
```bash
# HPA already configured in manifests
# Check HPA status
kubectl get hpa -n ecommerce

# View HPA details
kubectl describe hpa order-service-hpa -n ecommerce
```

### Cluster Autoscaling
```bash
# Enable cluster autoscaler (cloud-specific)
# AWS: Configure node groups
# GCP: Configure instance groups
# Azure: Configure VMSS
```

---

## 🔄 Updates & Rollback

### Rolling Update
```bash
# Update image
kubectl set image deployment/order-service \
  order-service=ecommerce/order-service:v2 \
  -n ecommerce

# Watch rollout
kubectl rollout status deployment/order-service -n ecommerce
```

### Rollback
```bash
# View rollout history
kubectl rollout history deployment/order-service -n ecommerce

# Rollback to previous version
kubectl rollout undo deployment/order-service -n ecommerce

# Rollback to specific revision
kubectl rollout undo deployment/order-service --to-revision=2 -n ecommerce
```

### Using Helm
```bash
# Upgrade
helm upgrade ecommerce ./helm/ecommerce \
  --set orderService.image.tag=v2 \
  -n ecommerce

# Rollback
helm rollback ecommerce 1 -n ecommerce
```

---

## 🗑️ Cleanup

### Remove All Services
```bash
# Using kubectl
kubectl delete namespace ecommerce

# Using Helm
helm uninstall ecommerce -n ecommerce

# Using Docker Compose
cd docker
docker-compose down -v
```

---

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

## 🆘 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review service logs
3. Check Prometheus metrics
4. Review distributed traces in Zipkin
5. Search logs in Kibana

---

**Happy Deploying! 🚀**

