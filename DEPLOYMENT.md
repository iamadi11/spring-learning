# Deployment Guide

## 📖 Overview

Complete deployment guide for the e-commerce microservices platform covering Docker, Kubernetes, Helm, and CI/CD.

---

## 🚀 Quick Start

### Prerequisites

**Local Development**:
- Java 21 (JDK)
- Docker 20.x+
- Docker Compose 2.x+
- Gradle 8.x (included via wrapper)

**Production Deployment**:
- Kubernetes 1.24+
- kubectl configured
- Helm 3.x+
- Container Registry (Docker Hub, GitHub Container Registry, etc.)

---

## 1. Docker Compose (Local Development)

### Start Infrastructure

```bash
cd docker
docker-compose up -d

# Verify services
docker-compose ps

# View logs
docker-compose logs -f
```

**Services Started**:
- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Zipkin: `localhost:9411`
- Prometheus: `localhost:9090`
- Grafana: `localhost:3000`
- Kibana: `localhost:5601`

### Build & Run Services

```bash
# Build all services
./gradlew build

# Run services in separate terminals
./gradlew :infrastructure:service-discovery:bootRun  # Terminal 1
./gradlew :infrastructure:config-server:bootRun      # Terminal 2
./gradlew :infrastructure:api-gateway:bootRun        # Terminal 3
./gradlew :services:auth-service:bootRun            # Terminal 4
./gradlew :services:user-service:bootRun            # Terminal 5
./gradlew :services:product-service:bootRun         # Terminal 6
./gradlew :services:order-service:bootRun           # Terminal 7
./gradlew :services:payment-service:bootRun         # Terminal 8
./gradlew :services:notification-service:bootRun    # Terminal 9
./gradlew :services:review-service:bootRun          # Terminal 10
```

### Verify

- Eureka Dashboard: http://localhost:8761
- API Gateway Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html

---

## 2. Docker Containerization

### Build Docker Images

```bash
# Using provided script (builds all services)
./build-all-docker-images.sh

# Or manually for specific service
docker build -t ecommerce/auth-service:latest -f services/auth-service/Dockerfile .
```

### Dockerfile Structure

**Multi-stage Dockerfile** (example):

```dockerfile
# Stage 1: Build
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY shared shared
COPY services/auth-service services/auth-service
RUN gradle :services:auth-service:build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /app/services/auth-service/build/libs/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Features**:
- ✅ Multi-stage build (smaller image)
- ✅ Non-root user (security)
- ✅ Health checks (orchestration)
- ✅ JVM container optimization

### Push to Registry

```bash
# Tag for your registry
export REGISTRY="your-registry.com"
export TAG="1.0.0"

docker tag ecommerce/auth-service:latest $REGISTRY/ecommerce/auth-service:$TAG
docker push $REGISTRY/ecommerce/auth-service:$TAG
```

---

## 3. Kubernetes Deployment

### Using kubectl

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

### Using Deployment Script

```bash
./deploy-kubernetes.sh

# Or with custom namespace
K8S_NAMESPACE=production ./deploy-kubernetes.sh
```

### Kubernetes Resources

**Deployment Example**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: ecommerce
spec:
  replicas: 3  # High availability
  selector:
    matchLabels:
      app: auth-service
  strategy:
    type: RollingUpdate  # Zero-downtime deployment
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: ecommerce/auth-service:latest
        ports:
        - containerPort: 8086
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "1000m"
            memory: "1Gi"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8086
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8086
          initialDelaySeconds: 30
          periodSeconds: 5
```

**HorizontalPodAutoscaler**:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 4. Helm Deployment

### Install with Helm

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

### Helm Operations

```bash
# Upgrade deployment
helm upgrade ecommerce ./helm/ecommerce \
  --namespace ecommerce \
  --set authService.replicaCount=5

# Rollback to previous version
helm rollback ecommerce 1 -n ecommerce

# Uninstall
helm uninstall ecommerce -n ecommerce
```

---

## 5. CI/CD Pipeline

### GitHub Actions Workflow

The project includes a complete CI/CD pipeline (`.github/workflows/ci-cd.yml`):

**Pipeline Stages**:

1. **Build & Test**
   - Checkout code
   - Set up JDK 21
   - Run unit tests
   - Run integration tests
   - Generate coverage report
   - Build all services

2. **Security Scan**
   - Run Trivy vulnerability scanner
   - Upload results to GitHub Security
   - Fail on CRITICAL/HIGH vulnerabilities

3. **Build Docker Images**
   - Set up Docker Buildx
   - Log in to container registry
   - Build and push images for all services
   - Tag with SHA, branch, and latest

4. **Deploy to Kubernetes**
   - Set up kubectl
   - Configure kubeconfig
   - Deploy to Kubernetes cluster
   - Verify deployment
   - Run smoke tests

5. **Notify**
   - Send Slack notification
   - Update deployment status

### Trigger Deployment

```bash
# Push to main branch triggers automatic deployment
git add .
git commit -m "Deploy to production"
git push origin main
```

---

## 6. Monitoring & Health Checks

### Access Monitoring Tools

**Prometheus** (Metrics):
```bash
kubectl port-forward -n ecommerce svc/prometheus 9090:9090
# Access: http://localhost:9090
```

**Grafana** (Dashboards):
```bash
kubectl port-forward -n ecommerce svc/grafana 3000:3000
# Access: http://localhost:3000 (admin/admin)
```

**Zipkin** (Distributed Tracing):
```bash
kubectl port-forward -n ecommerce svc/zipkin 9411:9411
# Access: http://localhost:9411
```

**Kibana** (Logs):
```bash
kubectl port-forward -n ecommerce svc/kibana 5601:5601
# Access: http://localhost:5601
```

### Health Checks

```bash
# Check pod status
kubectl get pods -n ecommerce

# View pod logs
kubectl logs -f deployment/order-service -n ecommerce

# Check service health
kubectl port-forward -n ecommerce svc/order-service 8089:8089
curl http://localhost:8089/actuator/health

# Check metrics
curl http://localhost:8089/actuator/prometheus
```

---

## 7. Scaling

### Manual Scaling

```bash
# Scale deployment
kubectl scale deployment order-service --replicas=5 -n ecommerce

# Scale using Helm
helm upgrade ecommerce ./helm/ecommerce \
  --set orderService.replicaCount=5 \
  -n ecommerce
```

### Auto-Scaling (HPA)

```bash
# HPA already configured in manifests
# Check HPA status
kubectl get hpa -n ecommerce

# View HPA details
kubectl describe hpa order-service-hpa -n ecommerce
```

---

## 8. Updates & Rollback

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

## 9. Troubleshooting

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

## 10. Cleanup

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

## 📋 Production Checklist

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

## 🎯 Deployment Strategies

### Rolling Update (Default)

- ✅ Zero downtime
- ✅ Gradual rollout
- ✅ Easy rollback
- ⚠️ Mixed versions during rollout

### Blue-Green Deployment

```bash
# Deploy green version
kubectl apply -f k8s/auth-service-green.yaml

# Test green version
kubectl port-forward svc/auth-service-green 8086:8086

# Switch traffic (update service selector)
kubectl patch service auth-service -p '{"spec":{"selector":{"version":"green"}}}'

# Remove blue version
kubectl delete deployment auth-service-blue
```

### Canary Deployment

```bash
# Deploy canary with 10% traffic
kubectl apply -f k8s/auth-service-canary.yaml

# Monitor metrics
# If successful, gradually increase traffic
# If issues, rollback canary
```

---

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)

---

## 🆘 Support

For issues or questions:
1. Check troubleshooting section above
2. Review service logs
3. Check Prometheus metrics
4. Review distributed traces in Zipkin
5. Search logs in Kibana

---

**Happy Deploying! 🚀**

