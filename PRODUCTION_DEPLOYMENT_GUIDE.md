# Production Deployment Guide

## Overview

This guide covers **complete production deployment** for the e-commerce microservices platform using Docker, Kubernetes, Helm, and CI/CD pipelines. Learn how to deploy like companies such as Netflix, Uber, and Airbnb.

## 🎯 Deployment Architecture

```
Developer → Git Push → CI/CD Pipeline → Build → Test → Deploy → Production

CI/CD Pipeline:
├─ Build Docker Images
├─ Run Tests (Unit, Integration, E2E)
├─ Security Scanning
├─ Push to Registry
├─ Deploy to Kubernetes
└─ Health Checks & Rollback
```

---

## 1. Docker Containerization 🐳

### Why Docker?

**Without Docker**:
```
Developer: "Works on my machine!" 🤷
Operations: "Doesn't work in production!" 😡
```

**With Docker**:
```
Developer: Build once → Run anywhere
Operations: Same container dev to prod ✅
```

### Dockerfile for Spring Boot Service

**Multi-stage Dockerfile** (optimized for size and security):

```dockerfile
# services/order-service/Dockerfile

# ==================== STAGE 1: Build ====================
FROM gradle:8.5-jdk21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle files (for caching)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application (skip tests, they run in CI)
RUN gradle build -x test --no-daemon

# ==================== STAGE 2: Runtime ====================
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8089/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Key Points**:
- ✅ **Multi-stage build**: Separate build and runtime (smaller image)
- ✅ **Non-root user**: Security best practice
- ✅ **Health check**: Container orchestration integration
- ✅ **JVM tuning**: Optimize for containers
- ✅ **Layer caching**: Dependencies cached separately

### Build Docker Image

```bash
# Build image
docker build -t ecommerce/order-service:1.0.0 \
  -f services/order-service/Dockerfile .

# Tag as latest
docker tag ecommerce/order-service:1.0.0 \
  ecommerce/order-service:latest

# Push to registry
docker push ecommerce/order-service:1.0.0
docker push ecommerce/order-service:latest
```

### Docker Compose for Local Testing

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  # Order Service
  order-service:
    image: ecommerce/order-service:latest
    container_name: order-service
    ports:
      - "8089:8089"
    environment:
      # Spring profiles
      SPRING_PROFILES_ACTIVE: prod
      
      # Database connection
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/order_db
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      
      # Eureka
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      
      # Kafka
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      
      # Redis
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      
      # Observability
      MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans
      
    depends_on:
      - postgres
      - kafka
      - redis
      - eureka-server
    networks:
      - ecommerce-network
    restart: unless-stopped
    
    # Resource limits
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
    
    # Health check
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8089/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

networks:
  ecommerce-network:
    driver: bridge
```

### Docker Best Practices

1. **Use specific tags**: `ecommerce/order-service:1.0.0` not `:latest`
2. **Multi-stage builds**: Reduce image size
3. **Non-root user**: Security
4. **Health checks**: Enable orchestration
5. **Resource limits**: Prevent resource exhaustion
6. **Secrets management**: Use environment variables or secrets
7. **Layer caching**: Order Dockerfile commands by change frequency
8. **Scan images**: Security vulnerabilities

---

## 2. Kubernetes Deployment ☸️

### Why Kubernetes?

**Features**:
- ✅ **Orchestration**: Manage 1000s of containers
- ✅ **Self-healing**: Auto-restart failed containers
- ✅ **Scaling**: Horizontal and vertical scaling
- ✅ **Load balancing**: Built-in service discovery
- ✅ **Rolling updates**: Zero-downtime deployments
- ✅ **Secrets management**: Secure credentials
- ✅ **Storage orchestration**: Persistent volumes

### Kubernetes Architecture

```
Kubernetes Cluster
├─ Master Node (Control Plane)
│  ├─ API Server (kubectl talks to this)
│  ├─ Scheduler (decides where to run pods)
│  ├─ Controller Manager (maintains desired state)
│  └─ etcd (cluster state database)
│
└─ Worker Nodes
   ├─ Worker Node 1
   │  ├─ kubelet (manages pods)
   │  ├─ kube-proxy (networking)
   │  └─ Pods (containers)
   │     ├─ order-service
   │     └─ payment-service
   │
   └─ Worker Node 2
      ├─ kubelet
      ├─ kube-proxy
      └─ Pods
         ├─ product-service
         └─ user-service
```

### Kubernetes Manifests

#### 1. Namespace

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce
  labels:
    name: ecommerce
    environment: production
```

#### 2. ConfigMap

```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-service-config
  namespace: ecommerce
data:
  # Application configuration
  application.yml: |
    server:
      port: 8089
    spring:
      application:
        name: order-service
      profiles:
        active: prod
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      metrics:
        export:
          prometheus:
            enabled: true
  
  # Logging configuration
  logback.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
          <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
        </encoder>
      </appender>
      <root level="INFO">
        <appender-ref ref="STDOUT" />
      </root>
    </configuration>
```

#### 3. Secret

```yaml
# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: order-service-secret
  namespace: ecommerce
type: Opaque
data:
  # Base64 encoded values
  database-username: cG9zdGdyZXM=  # postgres
  database-password: cGFzc3dvcmQxMjM=  # password123
  jwt-secret: bXlzdXBlcnNlY3JldGtleQ==  # mysupersecretkey
```

**Create secret from command**:
```bash
kubectl create secret generic order-service-secret \
  --from-literal=database-username=postgres \
  --from-literal=database-password=password123 \
  --from-literal=jwt-secret=mysupersecretkey \
  -n ecommerce
```

#### 4. Deployment

```yaml
# k8s/order-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: ecommerce
  labels:
    app: order-service
    version: v1
spec:
  # Number of replicas
  replicas: 3
  
  # Selector for pods
  selector:
    matchLabels:
      app: order-service
  
  # Deployment strategy
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Max pods above desired during update
      maxUnavailable: 0  # Zero-downtime deployment
  
  # Pod template
  template:
    metadata:
      labels:
        app: order-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8089"
        prometheus.io/path: "/actuator/prometheus"
    
    spec:
      # Service account (for RBAC)
      serviceAccountName: order-service-sa
      
      # Security context
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      
      # Containers
      containers:
      - name: order-service
        image: ecommerce/order-service:1.0.0
        imagePullPolicy: IfNotPresent
        
        # Ports
        ports:
        - name: http
          containerPort: 8089
          protocol: TCP
        
        # Environment variables
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        
        # Database connection
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service:5432/order_db"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: order-service-secret
              key: database-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: order-service-secret
              key: database-password
        
        # Eureka
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: "http://eureka-service:8761/eureka/"
        
        # Kafka
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-service:9092"
        
        # Redis
        - name: SPRING_REDIS_HOST
          value: "redis-service"
        - name: SPRING_REDIS_PORT
          value: "6379"
        
        # Zipkin
        - name: MANAGEMENT_ZIPKIN_TRACING_ENDPOINT
          value: "http://zipkin-service:9411/api/v2/spans"
        
        # JVM options
        - name: JAVA_OPTS
          value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
        
        # Resource limits
        resources:
          requests:
            cpu: "500m"      # 0.5 CPU
            memory: "512Mi"  # 512 MB
          limits:
            cpu: "1000m"     # 1 CPU
            memory: "1Gi"    # 1 GB
        
        # Liveness probe (restart if unhealthy)
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8089
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness probe (remove from load balancer if not ready)
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8089
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Startup probe (for slow starting apps)
        startupProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8089
          initialDelaySeconds: 0
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 30  # 30 * 5 = 150s max startup time
        
        # Volume mounts
        volumeMounts:
        - name: config
          mountPath: /config
          readOnly: true
        - name: logs
          mountPath: /logs
      
      # Volumes
      volumes:
      - name: config
        configMap:
          name: order-service-config
      - name: logs
        emptyDir: {}
      
      # Restart policy
      restartPolicy: Always
      
      # Termination grace period
      terminationGracePeriodSeconds: 30
```

**Key Components**:
- ✅ **Replicas**: 3 instances for high availability
- ✅ **Rolling update**: Zero-downtime deployment
- ✅ **Health probes**: Liveness, readiness, startup
- ✅ **Resource limits**: CPU and memory
- ✅ **Security**: Non-root user, security context
- ✅ **ConfigMap/Secret**: Configuration and credentials
- ✅ **Observability**: Prometheus annotations

#### 5. Service

```yaml
# k8s/order-service-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: ecommerce
  labels:
    app: order-service
spec:
  type: ClusterIP  # Internal service (use LoadBalancer for external)
  selector:
    app: order-service
  ports:
  - name: http
    port: 8089        # Service port
    targetPort: 8089  # Container port
    protocol: TCP
  sessionAffinity: None
```

#### 6. Ingress (External Access)

```yaml
# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  namespace: ecommerce
  annotations:
    # NGINX Ingress Controller
    kubernetes.io/ingress.class: "nginx"
    
    # SSL redirect
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    
    # Rate limiting
    nginx.ingress.kubernetes.io/limit-rps: "100"
    
    # CORS
    nginx.ingress.kubernetes.io/enable-cors: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://www.example.com"
    
    # SSL certificate (cert-manager)
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.ecommerce.com
    secretName: ecommerce-tls
  
  rules:
  - host: api.ecommerce.com
    http:
      paths:
      # Order Service
      - path: /api/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8089
      
      # Product Service
      - path: /api/products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 8088
      
      # User Service
      - path: /api/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8087
      
      # Auth Service
      - path: /api/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8086
```

#### 7. HorizontalPodAutoscaler

```yaml
# k8s/order-service-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: ecommerce
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  
  # Min and max replicas
  minReplicas: 3
  maxReplicas: 10
  
  # Scaling metrics
  metrics:
  # CPU utilization
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # Scale up if CPU > 70%
  
  # Memory utilization
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80  # Scale up if memory > 80%
  
  # Custom metric (requests per second)
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "1000"  # Scale up if > 1000 RPS per pod
  
  # Scaling behavior
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100  # Double pods
        periodSeconds: 15
      - type: Pods
        value: 2    # Add 2 pods
        periodSeconds: 15
      selectPolicy: Max
    
    scaleDown:
      stabilizationWindowSeconds: 300  # Wait 5 min before scale down
      policies:
      - type: Percent
        value: 50  # Remove 50% of pods
        periodSeconds: 15
      selectPolicy: Min
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create ConfigMap and Secret
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Deploy services
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/order-service-service.yaml
kubectl apply -f k8s/order-service-hpa.yaml

# Create Ingress
kubectl apply -f k8s/ingress.yaml

# Verify deployment
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
kubectl get ingress -n ecommerce

# Check logs
kubectl logs -f deployment/order-service -n ecommerce

# Describe pod (troubleshooting)
kubectl describe pod <pod-name> -n ecommerce

# Execute command in pod
kubectl exec -it <pod-name> -n ecommerce -- /bin/sh
```

---

## 3. Helm Charts 📦

### Why Helm?

**Without Helm**:
```
- 50+ YAML files to manage
- Copy-paste for each environment
- Hard to version
- Difficult to rollback
```

**With Helm**:
```
- Package all YAMLs as a "chart"
- Templating for different environments
- Version control
- One-command install/upgrade/rollback
```

### Helm Chart Structure

```
helm/order-service/
├── Chart.yaml           # Chart metadata
├── values.yaml          # Default values
├── values-dev.yaml      # Dev environment overrides
├── values-prod.yaml     # Prod environment overrides
└── templates/
    ├── deployment.yaml  # Deployment template
    ├── service.yaml     # Service template
    ├── ingress.yaml     # Ingress template
    ├── configmap.yaml   # ConfigMap template
    ├── secret.yaml      # Secret template
    ├── hpa.yaml         # HPA template
    ├── serviceaccount.yaml
    └── _helpers.tpl     # Helper functions
```

### Chart.yaml

```yaml
# helm/order-service/Chart.yaml
apiVersion: v2
name: order-service
description: Order Service Helm Chart for E-commerce Platform
type: application
version: 1.0.0        # Chart version
appVersion: "1.0.0"   # Application version

# Dependencies
dependencies:
- name: postgresql
  version: "12.x.x"
  repository: https://charts.bitnami.com/bitnami
  condition: postgresql.enabled

- name: redis
  version: "17.x.x"
  repository: https://charts.bitnami.com/bitnami
  condition: redis.enabled

maintainers:
- name: DevOps Team
  email: devops@ecommerce.com
```

### values.yaml

```yaml
# helm/order-service/values.yaml

# Replica count
replicaCount: 3

# Image configuration
image:
  repository: ecommerce/order-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent

# Image pull secrets (for private registries)
imagePullSecrets: []

# Service account
serviceAccount:
  create: true
  name: order-service-sa

# Service configuration
service:
  type: ClusterIP
  port: 8089
  targetPort: 8089

# Ingress configuration
ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
  hosts:
  - host: api.ecommerce.com
    paths:
    - path: /api/orders
      pathType: Prefix
  tls:
  - secretName: ecommerce-tls
    hosts:
    - api.ecommerce.com

# Resource limits
resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

# Autoscaling
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

# Health probes
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8089
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8089
  initialDelaySeconds: 30
  periodSeconds: 5

# Environment variables
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: JAVA_OPTS
    value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# ConfigMap data
configMap:
  data:
    application.yml: |
      server:
        port: 8089

# Secret data (base64 encoded)
secret:
  data:
    database-username: cG9zdGdyZXM=
    database-password: cGFzc3dvcmQxMjM=

# PostgreSQL dependency
postgresql:
  enabled: true
  auth:
    username: postgres
    password: password123
    database: order_db

# Redis dependency
redis:
  enabled: true
  auth:
    enabled: false
```

### Template: deployment.yaml

```yaml
# helm/order-service/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "order-service.fullname" . }}
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "order-service.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "order-service.selectorLabels" . | nindent 6 }}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        {{- include "order-service.selectorLabels" . | nindent 8 }}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "{{ .Values.service.port }}"
    spec:
      serviceAccountName: {{ include "order-service.serviceAccountName" . }}
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: {{ .Values.service.targetPort }}
        env:
        {{- toYaml .Values.env | nindent 8 }}
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
        livenessProbe:
          {{- toYaml .Values.livenessProbe | nindent 10 }}
        readinessProbe:
          {{- toYaml .Values.readinessProbe | nindent 10 }}
```

### Helm Commands

```bash
# Install chart
helm install order-service ./helm/order-service \
  --namespace ecommerce \
  --create-namespace

# Install with custom values
helm install order-service ./helm/order-service \
  --namespace ecommerce \
  --values ./helm/order-service/values-prod.yaml

# Upgrade release
helm upgrade order-service ./helm/order-service \
  --namespace ecommerce \
  --values ./helm/order-service/values-prod.yaml

# Rollback to previous version
helm rollback order-service 1 --namespace ecommerce

# List releases
helm list --namespace ecommerce

# Get release status
helm status order-service --namespace ecommerce

# Uninstall release
helm uninstall order-service --namespace ecommerce

# Dry run (see what will be deployed)
helm install order-service ./helm/order-service \
  --dry-run --debug \
  --namespace ecommerce
```

---

## 4. CI/CD Pipeline 🚀

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ecommerce/order-service

jobs:
  # ==================== BUILD & TEST ====================
  build:
    name: Build & Test
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: 'gradle'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run unit tests
      run: ./gradlew test
    
    - name: Run integration tests
      run: ./gradlew integrationTest
    
    - name: Generate test coverage report
      run: ./gradlew jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./build/reports/jacoco/test/jacocoTestReport.xml
    
    - name: Build JAR
      run: ./gradlew build -x test
    
    - name: Upload JAR artifact
      uses: actions/upload-artifact@v3
      with:
        name: app-jar
        path: build/libs/*.jar
  
  # ==================== SECURITY SCAN ====================
  security:
    name: Security Scan
    runs-on: ubuntu-latest
    needs: build
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Run Trivy vulnerability scanner
      uses: aquasecurity/trivy-action@master
      with:
        scan-type: 'fs'
        scan-ref: '.'
        format: 'sarif'
        output: 'trivy-results.sarif'
    
    - name: Upload Trivy results to GitHub Security
      uses: github/codeql-action/upload-sarif@v2
      with:
        sarif_file: 'trivy-results.sarif'
  
  # ==================== BUILD DOCKER IMAGE ====================
  docker:
    name: Build & Push Docker Image
    runs-on: ubuntu-latest
    needs: [build, security]
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=semver,pattern={{version}}
          type=sha,prefix={{branch}}-
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        file: ./services/order-service/Dockerfile
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
    
    - name: Scan Docker image
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
        format: 'table'
        exit-code: '1'
        severity: 'CRITICAL,HIGH'
  
  # ==================== DEPLOY TO KUBERNETES ====================
  deploy:
    name: Deploy to Kubernetes
    runs-on: ubuntu-latest
    needs: docker
    if: github.ref == 'refs/heads/main'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Set up kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: 'latest'
    
    - name: Set up Helm
      uses: azure/setup-helm@v3
      with:
        version: 'latest'
    
    - name: Configure Kubernetes context
      uses: azure/k8s-set-context@v3
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG }}
    
    - name: Deploy with Helm
      run: |
        helm upgrade --install order-service ./helm/order-service \
          --namespace ecommerce \
          --create-namespace \
          --values ./helm/order-service/values-prod.yaml \
          --set image.tag=${{ github.sha }} \
          --wait \
          --timeout 5m
    
    - name: Verify deployment
      run: |
        kubectl rollout status deployment/order-service -n ecommerce
        kubectl get pods -n ecommerce -l app=order-service
    
    - name: Run smoke tests
      run: |
        kubectl run smoke-test --image=curlimages/curl:latest \
          --restart=Never -n ecommerce \
          --command -- curl -f http://order-service:8089/actuator/health
        kubectl wait --for=condition=completed pod/smoke-test -n ecommerce --timeout=60s
        kubectl logs smoke-test -n ecommerce
        kubectl delete pod smoke-test -n ecommerce
  
  # ==================== NOTIFY ====================
  notify:
    name: Notify Deployment
    runs-on: ubuntu-latest
    needs: deploy
    if: always()
    
    steps:
    - name: Send Slack notification
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        text: |
          Deployment ${{ job.status }}
          Repository: ${{ github.repository }}
          Branch: ${{ github.ref }}
          Commit: ${{ github.sha }}
        webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### GitLab CI/CD

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - security
  - docker
  - deploy

variables:
  DOCKER_REGISTRY: registry.gitlab.com
  IMAGE_NAME: $CI_REGISTRY_IMAGE/order-service
  KUBECONFIG: /etc/deploy/config

# ==================== BUILD ====================
build:
  stage: build
  image: gradle:8.5-jdk21
  script:
    - ./gradlew build -x test
  artifacts:
    paths:
      - build/libs/*.jar
    expire_in: 1 day
  cache:
    paths:
      - .gradle/

# ==================== TEST ====================
test:unit:
  stage: test
  image: gradle:8.5-jdk21
  script:
    - ./gradlew test
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit: build/test-results/test/TEST-*.xml

test:integration:
  stage: test
  image: gradle:8.5-jdk21
  services:
    - postgres:15
    - redis:7
  variables:
    POSTGRES_DB: testdb
    POSTGRES_USER: test
    POSTGRES_PASSWORD: test
  script:
    - ./gradlew integrationTest

# ==================== SECURITY ====================
security:scan:
  stage: security
  image: aquasec/trivy:latest
  script:
    - trivy fs --exit-code 1 --severity CRITICAL,HIGH .

# ==================== DOCKER ====================
docker:build:
  stage: docker
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - docker build -t $IMAGE_NAME:$CI_COMMIT_SHA -f services/order-service/Dockerfile .
    - docker tag $IMAGE_NAME:$CI_COMMIT_SHA $IMAGE_NAME:latest
    - docker push $IMAGE_NAME:$CI_COMMIT_SHA
    - docker push $IMAGE_NAME:latest

# ==================== DEPLOY ====================
deploy:production:
  stage: deploy
  image: alpine/helm:latest
  only:
    - main
  script:
    - helm upgrade --install order-service ./helm/order-service \
        --namespace ecommerce \
        --create-namespace \
        --values ./helm/order-service/values-prod.yaml \
        --set image.tag=$CI_COMMIT_SHA \
        --wait
    - kubectl rollout status deployment/order-service -n ecommerce
  environment:
    name: production
    url: https://api.ecommerce.com
```

---

## 5. Deployment Strategies 📈

### 1. Rolling Update (Default)

**How it works**:
```
Old version (v1): [Pod1] [Pod2] [Pod3]
                      ↓
Step 1:          [Pod1] [Pod2] [Pod3] [Pod4-v2]  (Add new)
Step 2:          [Pod2] [Pod3] [Pod4-v2] [Pod5-v2]  (Remove old)
Step 3:          [Pod3] [Pod4-v2] [Pod5-v2] [Pod6-v2]
Step 4:          [Pod4-v2] [Pod5-v2] [Pod6-v2]  (All new)
```

**Pros**:
- ✅ Zero downtime
- ✅ Gradual rollout
- ✅ Easy rollback

**Cons**:
- ❌ Both versions running simultaneously
- ❌ Slower deployment

**Configuration**:
```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # Add 1 pod at a time
    maxUnavailable: 0  # Keep all pods running
```

### 2. Blue-Green Deployment

**How it works**:
```
Blue (v1):  [Pod1] [Pod2] [Pod3] ← Traffic (100%)
Green (v2): [Pod4] [Pod5] [Pod6] ← No traffic

Switch traffic:
Blue (v1):  [Pod1] [Pod2] [Pod3] ← No traffic
Green (v2): [Pod4] [Pod5] [Pod6] ← Traffic (100%)

If successful, delete Blue. If failed, switch back to Blue.
```

**Pros**:
- ✅ Instant rollback
- ✅ Testing in production environment
- ✅ Zero downtime

**Cons**:
- ❌ 2x resources during deployment
- ❌ Database migrations tricky

**Implementation**:
```yaml
# Blue deployment (current)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
      version: blue

---
# Green deployment (new)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service-green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
      version: green

---
# Service (switch between blue and green)
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
    version: blue  # Change to 'green' to switch traffic
```

### 3. Canary Deployment

**How it works**:
```
Step 1: 95% → v1, 5% → v2    (Test with small traffic)
Step 2: 90% → v1, 10% → v2   (Increase if metrics good)
Step 3: 50% → v1, 50% → v2   (Half traffic)
Step 4: 0% → v1, 100% → v2   (Complete migration)
```

**Pros**:
- ✅ Gradual rollout
- ✅ Real user testing
- ✅ Early problem detection

**Cons**:
- ❌ Complex monitoring
- ❌ Requires traffic splitting

**Implementation with Istio**:
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service
spec:
  hosts:
  - order-service
  http:
  - match:
    - headers:
        user-agent:
          regex: ".*Chrome.*"  # Canary for Chrome users
    route:
    - destination:
        host: order-service
        subset: v2
      weight: 100
  - route:
    - destination:
        host: order-service
        subset: v1
      weight: 95
    - destination:
        host: order-service
        subset: v2
      weight: 5
```

---

## 6. Production Best Practices 🏆

### 1. Resource Management

**Set Resource Requests and Limits**:
```yaml
resources:
  requests:
    cpu: "500m"      # Guaranteed CPU
    memory: "512Mi"  # Guaranteed memory
  limits:
    cpu: "1000m"     # Max CPU
    memory: "1Gi"    # Max memory (OOMKilled if exceeded)
```

**Why**:
- ✅ Prevents resource starvation
- ✅ Enables autoscaling
- ✅ Protects other pods
- ✅ Better scheduling

### 2. Health Checks

**Three types**:
```yaml
# Liveness: Restart if unhealthy
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8089
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 3

# Readiness: Remove from load balancer if not ready
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8089
  initialDelaySeconds: 30
  periodSeconds: 5

# Startup: For slow-starting apps
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8089
  failureThreshold: 30
  periodSeconds: 5
```

### 3. Security

**Security Context**:
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true
  capabilities:
    drop:
    - ALL
```

**Network Policies**:
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: order-service-netpol
spec:
  podSelector:
    matchLabels:
      app: order-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: api-gateway
    ports:
    - protocol: TCP
      port: 8089
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
```

### 4. Secrets Management

**Use External Secrets** (not in Git):
```yaml
# External Secrets Operator
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-service-secrets
spec:
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: order-service-secret
  data:
  - secretKey: database-password
    remoteRef:
      key: prod/order-service/db-password
```

### 5. Observability

**Distributed Tracing**:
```yaml
annotations:
  sidecar.jaegertracing.io/inject: "true"
```

**Metrics**:
```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8089"
  prometheus.io/path: "/actuator/prometheus"
```

### 6. Disaster Recovery

**Backup Strategy**:
```bash
# Backup PostgreSQL
kubectl exec -n ecommerce postgres-0 -- \
  pg_dump -U postgres order_db > backup.sql

# Backup MongoDB
kubectl exec -n ecommerce mongodb-0 -- \
  mongodump --archive > mongo-backup.archive

# Backup Kubernetes resources
kubectl get all -n ecommerce -o yaml > k8s-backup.yaml
```

**Restore**:
```bash
# Restore PostgreSQL
cat backup.sql | kubectl exec -i -n ecommerce postgres-0 -- \
  psql -U postgres -d order_db

# Restore MongoDB
cat mongo-backup.archive | kubectl exec -i -n ecommerce mongodb-0 -- \
  mongorestore --archive

# Restore Kubernetes
kubectl apply -f k8s-backup.yaml
```

---

## 7. Monitoring & Alerting 📊

### Prometheus Alerts for Production

```yaml
# prometheus-rules.yaml
groups:
- name: production-alerts
  interval: 30s
  rules:
  # Pod is down
  - alert: PodDown
    expr: up{job="kubernetes-pods"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Pod {{ $labels.pod }} is down"
      description: "{{ $labels.pod }} in {{ $labels.namespace }} has been down for more than 1 minute"
  
  # High error rate
  - alert: HighErrorRate
    expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate on {{ $labels.service }}"
  
  # Pod CPU throttling
  - alert: PodCPUThrottling
    expr: rate(container_cpu_cfs_throttled_seconds_total[5m]) > 0.5
    for: 10m
    labels:
      severity: warning
  
  # Pod memory near limit
  - alert: PodMemoryNearLimit
    expr: (container_memory_usage_bytes / container_spec_memory_limit_bytes) > 0.9
    for: 5m
    labels:
      severity: warning
```

---

## 8. Key Takeaways 🎯

1. **Docker**: Containerize for consistency
2. **Kubernetes**: Orchestrate for scale
3. **Helm**: Package for reusability
4. **CI/CD**: Automate everything
5. **Health Checks**: Enable self-healing
6. **Resource Limits**: Prevent resource exhaustion
7. **Security**: Non-root, NetworkPolicies, Secrets
8. **Monitoring**: Prometheus, Grafana, Alerts
9. **Backup**: Disaster recovery plan
10. **Documentation**: Keep it updated

---

**Congratulations!** You now have production-ready deployment strategies! 🎉

