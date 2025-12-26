# ☁️ Free Cloud Deployment Guide

This guide shows you how to deploy your e-commerce microservices platform to various free cloud services.

## 📋 Table of Contents
- [Option 1: Railway (Recommended)](#option-1-railway-recommended)
- [Option 2: Render](#option-2-render)
- [Option 3: AWS Free Tier](#option-3-aws-free-tier)
- [Option 4: Google Cloud Run](#option-4-google-cloud-run)
- [Option 5: Azure Container Apps](#option-5-azure-container-apps)
- [Minimal Setup for Free Tier](#minimal-setup-for-free-tier)

---

## Option 1: Railway (Recommended) ⭐

**Free Tier:** $5 credit/month (enough for 2-3 services)
**Best For:** Quick deployment, PostgreSQL, Redis included

### Deployment Steps:

1. **Sign up:** https://railway.app/
2. **Install Railway CLI:**
   ```bash
   npm i -g @railway/cli
   railway login
   ```

3. **Create Railway project:**
   ```bash
   railway init
   ```

4. **Add Services:**
   ```bash
   # Add PostgreSQL
   railway add --database postgres
   
   # Add Redis
   railway add --database redis
   
   # Add MongoDB (from template)
   railway add --template mongodb
   ```

5. **Deploy Microservices:**

   Create `railway.toml` in project root:
   ```toml
   [build]
   builder = "DOCKERFILE"
   dockerfilePath = "services/auth-service/Dockerfile"
   
   [deploy]
   numReplicas = 1
   startCommand = ""
   restartPolicyType = "ON_FAILURE"
   ```

6. **Deploy each service:**
   ```bash
   # Deploy Auth Service
   railway up -d services/auth-service
   
   # Deploy API Gateway
   railway up -d infrastructure/api-gateway
   
   # Deploy other services as needed
   ```

### Railway Configuration:

For each service, set environment variables in Railway dashboard:
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=${DATABASE_URL}  # Auto-provided by Railway
REDIS_URL=${REDIS_URL}        # Auto-provided by Railway
```

---

## Option 2: Render

**Free Tier:** 750 hours/month, PostgreSQL included
**Best For:** Continuous deployment from GitHub

### Deployment Steps:

1. **Sign up:** https://render.com/
2. **Push code to GitHub** (if not already)
3. **Create `render.yaml` in root:**

```yaml
services:
  # PostgreSQL Database
  - type: pserv
    name: ecommerce-postgres
    env: docker
    plan: free
    
  # Redis
  - type: redis
    name: ecommerce-redis
    plan: free
    maxmemoryPolicy: allkeys-lru

  # Auth Service
  - type: web
    name: auth-service
    env: docker
    dockerfilePath: ./services/auth-service/Dockerfile
    plan: free
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: DATABASE_URL
        fromDatabase:
          name: ecommerce-postgres
          property: connectionString

  # API Gateway
  - type: web
    name: api-gateway
    env: docker
    dockerfilePath: ./infrastructure/api-gateway/Dockerfile
    plan: free
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
```

4. **Deploy:**
   - Connect GitHub repo in Render dashboard
   - Select `render.yaml` for automatic deployment
   - Render will deploy all services automatically

---

## Option 3: AWS Free Tier

**Free Tier:** 12 months, includes EC2, RDS, ElastiCache
**Best For:** Production-like setup, more control

### Deployment Steps:

1. **Create AWS Account:** https://aws.amazon.com/free/

2. **Setup ECS (Elastic Container Service):**
   ```bash
   # Install AWS CLI
   brew install awscli  # macOS
   aws configure
   ```

3. **Create ECR repositories for each service:**
   ```bash
   aws ecr create-repository --repository-name ecommerce/auth-service
   aws ecr create-repository --repository-name ecommerce/api-gateway
   # ... repeat for other services
   ```

4. **Build and push images:**
   ```bash
   # Login to ECR
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
   
   # Build and push
   docker build -t ecommerce/auth-service -f services/auth-service/Dockerfile .
   docker tag ecommerce/auth-service:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce/auth-service:latest
   docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ecommerce/auth-service:latest
   ```

5. **Create ECS Task Definitions and Services** via AWS Console

6. **Setup RDS for PostgreSQL, ElastiCache for Redis**

---

## Option 4: Google Cloud Run

**Free Tier:** 2 million requests/month
**Best For:** Serverless, auto-scaling

### Deployment Steps:

1. **Install Google Cloud SDK:**
   ```bash
   brew install --cask google-cloud-sdk
   gcloud init
   ```

2. **Create project:**
   ```bash
   gcloud projects create ecommerce-platform
   gcloud config set project ecommerce-platform
   ```

3. **Build and deploy with Cloud Build:**
   ```bash
   # Enable APIs
   gcloud services enable cloudbuild.googleapis.com run.googleapis.com
   
   # Build image
   gcloud builds submit --tag gcr.io/ecommerce-platform/auth-service services/auth-service
   
   # Deploy to Cloud Run
   gcloud run deploy auth-service \
     --image gcr.io/ecommerce-platform/auth-service \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars "SPRING_PROFILES_ACTIVE=prod"
   ```

4. **Setup Cloud SQL for PostgreSQL:**
   ```bash
   gcloud sql instances create ecommerce-postgres \
     --database-version=POSTGRES_15 \
     --tier=db-f1-micro \
     --region=us-central1
   ```

---

## Option 5: Azure Container Apps

**Free Tier:** 180,000 vCPU-seconds, 360,000 GiB-seconds/month
**Best For:** Microsoft ecosystem integration

### Deployment Steps:

1. **Install Azure CLI:**
   ```bash
   brew install azure-cli
   az login
   ```

2. **Create resource group:**
   ```bash
   az group create --name ecommerce-rg --location eastus
   ```

3. **Create Container Apps environment:**
   ```bash
   az containerapp env create \
     --name ecommerce-env \
     --resource-group ecommerce-rg \
     --location eastus
   ```

4. **Deploy containers:**
   ```bash
   # Build and push to Azure Container Registry
   az acr create --resource-group ecommerce-rg --name ecommerceacr --sku Basic
   az acr build --registry ecommerceacr --image auth-service:latest services/auth-service
   
   # Deploy to Container Apps
   az containerapp create \
     --name auth-service \
     --resource-group ecommerce-rg \
     --environment ecommerce-env \
     --image ecommerceacr.azurecr.io/auth-service:latest \
     --target-port 9001 \
     --ingress external
   ```

---

## 🎯 Minimal Setup for Free Tier

To stay within free tier limits, deploy only essential services:

### Core Services (Priority 1):
1. **PostgreSQL Database** (managed service from provider)
2. **Redis Cache** (managed service from provider)
3. **API Gateway** (1 instance)
4. **Auth Service** (1 instance)
5. **User Service** (1 instance)

### Optional Services (Priority 2):
6. **Product Service** (1 instance)
7. **Order Service** (1 instance)

### Skip Initially:
- Payment Service (use mock for testing)
- Notification Service (use logs instead)
- Review Service (add later)
- Kafka (use direct HTTP calls instead)
- ELK Stack (use cloud provider logs)
- Grafana/Prometheus (use cloud metrics)

### Minimal docker-compose.yml

Create `docker-compose.minimal.yml`:

```yaml
version: '3.8'

services:
  # Only essential databases
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # Eureka (required for service discovery)
  eureka-server:
    build:
      context: ..
      dockerfile: infrastructure/service-discovery/Dockerfile
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  # API Gateway (entry point)
  api-gateway:
    build:
      context: ..
      dockerfile: infrastructure/api-gateway/Dockerfile
    depends_on:
      - eureka-server
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/

  # Auth Service (authentication)
  auth-service:
    build:
      context: ..
      dockerfile: services/auth-service/Dockerfile
    depends_on:
      - postgres
      - redis
      - eureka-server
    ports:
      - "9001:9001"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db
      - SPRING_REDIS_HOST=redis

  # User Service (user management)
  user-service:
    build:
      context: ..
      dockerfile: services/user-service/Dockerfile
    depends_on:
      - postgres
      - redis
      - eureka-server
    ports:
      - "9002:9002"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_PRIMARY_URL=jdbc:postgresql://postgres:5432/user_db
      - SPRING_REDIS_HOST=redis

volumes:
  postgres-data:

networks:
  default:
    name: ecommerce-network
```

---

## 📊 Cost Comparison

| Provider | Free Tier | Best For | Limitations |
|----------|-----------|----------|-------------|
| **Railway** | $5/month credit | Quick start, hobby projects | 2-3 services max |
| **Render** | 750 hrs/month | GitHub integration | Services sleep after 15 min inactivity |
| **AWS** | 12 months | Learning AWS, scalability | Complex setup, credit card required |
| **Google Cloud** | 2M requests/month | Serverless, auto-scale | Cold starts, stateless only |
| **Azure** | Generous compute | Enterprise integration | Microsoft ecosystem lock-in |

---

## 🚀 Quick Deploy Script for Railway

Create `deploy-railway.sh`:

```bash
#!/bin/bash

echo "🚀 Deploying to Railway..."

# Deploy infrastructure
railway up -s eureka-server -d infrastructure/service-discovery
railway up -s api-gateway -d infrastructure/api-gateway

# Deploy core services
railway up -s auth-service -d services/auth-service
railway up -s user-service -d services/user-service

echo "✅ Deployment complete! Check Railway dashboard for URLs"
```

Make executable:
```bash
chmod +x deploy-railway.sh
./deploy-railway.sh
```

---

## 🔧 Environment Variables for Cloud

Create `.env.cloud`:

```bash
# Database (Railway/Render provides these)
DATABASE_URL=${DATABASE_URL}
REDIS_URL=${REDIS_URL}

# Service URLs (update after deployment)
EUREKA_URL=https://eureka-server.railway.app
API_GATEWAY_URL=https://api-gateway.railway.app

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# Security (use proper secrets in production!)
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000
```

---

## 📝 Next Steps

1. Choose a cloud provider based on your needs
2. Start with minimal setup (4-5 services)
3. Use managed databases (PostgreSQL, Redis)
4. Monitor your usage to stay within free tier
5. Scale up as needed

## 🆘 Support

- Railway: https://docs.railway.app
- Render: https://render.com/docs
- AWS: https://aws.amazon.com/documentation/
- Google Cloud: https://cloud.google.com/docs
- Azure: https://docs.microsoft.com/azure/

Good luck with your deployment! 🎉

