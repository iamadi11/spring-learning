# Docker Memory Configuration Fix

## Problem
The `start-local.sh` script fails with "cannot allocate memory" error when building multiple Java microservices.

## Solution Options

### Option 1: Increase Docker Memory (Recommended)
1. Open **Docker Desktop**
2. Go to **Settings/Preferences** → **Resources**
3. Increase **Memory** to at least:
   - **Minimum:** 8 GB
   - **Recommended:** 12-16 GB (for smooth operation)
4. Increase **Swap** to 2-4 GB
5. Click **Apply & Restart**

### Option 2: Build Images Sequentially
If you can't increase Docker memory, use this alternative script:

```bash
# From project root directory
cd docker

# Build services one at a time
docker-compose build postgres mongodb redis
docker-compose build zookeeper kafka
docker-compose build eureka-server
docker-compose build config-server
docker-compose build api-gateway
docker-compose build auth-service
docker-compose build user-service
docker-compose build product-service
docker-compose build order-service
docker-compose build payment-service
docker-compose build notification-service
docker-compose build review-service

# Then start everything
docker-compose up -d
```

### Option 3: Use Pre-built Images
Build services locally without Docker (requires Java 21):

```bash
# From project root
./gradlew build -x test

# Then start Docker services (they'll use local builds)
cd docker
docker-compose up -d
```

### Option 4: Build Only Critical Services
Edit `docker-compose.yml` to comment out less critical services initially:
- Review Service
- Notification Service  

Start with core services first, then add others later.

## Verify Docker Resources

Check current Docker resource usage:
```bash
docker system df
docker stats --no-stream
```

## After Fixing

Once you've increased Docker memory, run:
```bash
./start-local.sh
```

The first build will take 5-10 minutes. Subsequent starts will be much faster since images are cached.

## System Requirements

For the full e-commerce platform:
- **RAM:** 16 GB recommended (8 GB minimum)
- **Disk Space:** 20 GB for Docker images and volumes
- **CPU:** 4+ cores recommended

## Current Services Count

The platform runs:
- 7 Microservices (auth, user, product, order, payment, notification, review)
- 3 Infrastructure services (eureka, config-server, api-gateway)
- 3 Databases (PostgreSQL, MongoDB, Redis)
- 2 Message Queue (Zookeeper, Kafka + UI)
- 6 Observability tools (Zipkin, Prometheus, Grafana, Elasticsearch, Logstash, Kibana)

**Total: 21 containers** 🐳

