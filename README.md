# E-commerce Microservices Platform

A comprehensive, production-grade e-commerce platform built with **Spring Boot microservices architecture**. This project demonstrates all major backend concepts, system design patterns, authentication strategies, multithreading, and distributed systems principles.

**✅ 100% COMPLETE** - All 13 phases implemented with full deployment infrastructure and comprehensive documentation!

## ✨ Features Implemented

### 🎯 Core Microservices (10 Services)
✅ API Gateway with rate limiting & circuit breaker  
✅ Service Discovery (Netflix Eureka)  
✅ Centralized Configuration Server  
✅ Authentication Service (OAuth2, JWT, Social Login, 2FA)  
✅ User Service (CQRS pattern)  
✅ Product Service (Event Sourcing)  
✅ Order Service (Saga pattern)  
✅ Payment Service (Resilience patterns)  
✅ Notification Service (Multithreading, WebSocket)  
✅ Review Service (gRPC communication)  

### 🔐 Authentication & Security
✅ OAuth2 Authorization Server (All grant types)  
✅ JWT Access & Refresh Tokens  
✅ Social Login (Google, GitHub, Facebook)  
✅ Two-Factor Authentication (TOTP, SMS, Email)  
✅ API Key Management  
✅ Multi-Tenancy Support  
✅ Role-Based Access Control (RBAC)  
✅ BCrypt Password Hashing  

### 🏗️ System Design Patterns
✅ CQRS (Command Query Responsibility Segregation)  
✅ Event Sourcing  
✅ Saga Pattern for distributed transactions  
✅ Circuit Breaker, Retry, Bulkhead  
✅ Outbox Pattern  
✅ Cache-Aside, Write-Through caching  
✅ Database Replication & Sharding  
✅ API Gateway pattern  
✅ Service Discovery pattern  

### 📊 Databases & Caching
✅ PostgreSQL with Primary-Replica replication  
✅ MongoDB with Sharding by category  
✅ Redis for distributed caching  
✅ Database per Service pattern  
✅ Flyway/Liquibase migrations  

### 📨 Messaging & Communication
✅ Apache Kafka event streaming  
✅ gRPC for high-performance RPC  
✅ WebSocket for real-time notifications  
✅ REST APIs (80+ endpoints)  
✅ Dead Letter Queue for failed messages  

### 🔍 Monitoring & Observability
✅ Prometheus metrics collection  
✅ Grafana dashboards  
✅ Zipkin distributed tracing  
✅ ELK Stack (Elasticsearch, Logstash, Kibana)  
✅ Spring Boot Actuator health checks  
✅ Correlation IDs for request tracking  

### 🧪 Testing & Documentation
✅ Unit tests (JUnit 5 + Mockito)  
✅ Integration tests (Testcontainers)  
✅ Postman collection (80+ endpoints)  
✅ Swagger/OpenAPI documentation  
✅ 10,000+ lines of comprehensive guides  

### 🚀 Deployment & DevOps
✅ Docker containerization  
✅ Docker Compose orchestration  
✅ Kubernetes manifests  
✅ Helm charts  
✅ Multiple deployment scripts  
✅ Cloud deployment guides (AWS, GCP, Azure, Railway)  

## 📋 Table of Contents

- [Complete Documentation](#-complete-documentation-10000-lines)
- [Technology Stack](#️-technology-stack)
- [Architecture Overview](#️-architecture-overview)
- [Quick Start](#-quick-start)
- [Service URLs](#-service-urls-after-startup)
- [What's Running in Docker](#-whats-running-in-docker)
- [Manual Build & Run](#️-manual-build--run-development)
- [API Testing with Postman](#-api-testing-with-postman)
- [Key Concepts Covered](#-key-concepts-covered)
- [Database Strategy](#️-database-strategy)
- [Security](#-security)
- [Monitoring & Observability](#-monitoring--observability)
- [Testing](#-testing)
- [API Documentation](#-api-documentation)
- [Project Structure](#️-project-structure)
- [Development](#-development)
- [Learning Resources](#-learning-resources)
- [Troubleshooting](#-troubleshooting)
- [Production Deployment](#-production-deployment)

## 📚 Complete Documentation (10,000+ lines)

This project includes comprehensive guides for every aspect:

### Quick Start & Getting Started
- **[README.md](README.md)** - Complete overview and quick start (this file)
- **[START_HERE.md](START_HERE.md)** - Absolute beginner's guide
- **[QUICK_START.md](QUICK_START.md)** - Fast setup for all experience levels
- **[LEARNING_PATH.md](LEARNING_PATH.md)** - Week-by-week learning guide

### Core Technical Guides
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Complete system architecture & design
- **[AUTH_GUIDE.md](AUTH_GUIDE.md)** - OAuth2, JWT, Social Login, 2FA comprehensive guide
- **[MULTITHREADING_GUIDE.md](MULTITHREADING_GUIDE.md)** - Threading & concurrency patterns
- **[SYSTEM_DESIGN_PATTERNS.md](SYSTEM_DESIGN_PATTERNS.md)** - 20+ design patterns explained
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - All 80+ API endpoints documented
- **[DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)** - PostgreSQL & MongoDB schemas

### Deployment & Operations
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Complete production deployment guide
- **[DEPLOYMENT_OPTIONS.md](DEPLOYMENT_OPTIONS.md)** - Comparing deployment strategies
- **[CLOUD_DEPLOYMENT_GUIDE.md](CLOUD_DEPLOYMENT_GUIDE.md)** - Railway, AWS, GCP, Azure deployment
- **[DOCKER_MEMORY_FIX.md](DOCKER_MEMORY_FIX.md)** - Docker memory optimization guide

### Testing & Verification
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Comprehensive testing strategies
- **[POSTMAN_GUIDE.md](POSTMAN_GUIDE.md)** - API testing with Postman collection
- **[POSTMAN_SETUP_GUIDE.md](POSTMAN_SETUP_GUIDE.md)** - Detailed Postman configuration
- **[VERIFICATION_SUMMARY.md](VERIFICATION_SUMMARY.md)** - Project verification checklist

### Project Status
- **[PROJECT_STATUS.md](PROJECT_STATUS.md)** - Current implementation status
- **[FINAL_STATUS.md](FINAL_STATUS.md)** - Final project completion report

## 🛠️ Technology Stack

### Core Technologies
- **Java 21** - Latest LTS version with Virtual Threads
- **Spring Boot 3.2+** - Framework for microservices
- **Spring Cloud** - Microservices patterns (Gateway, Config, Discovery)
- **Gradle 8.5+** - Build automation and dependency management

### Databases
- **PostgreSQL** - Relational data (Auth, User, Order, Payment services)
- **MongoDB** - Document data (Product, Review, Notification services)
- **Redis** - Caching and session storage

### Message Queue & Communication
- **Apache Kafka** - Event streaming and async messaging
- **gRPC** - High-performance RPC (Review service)
- **WebSocket** - Real-time notifications

### Infrastructure & DevOps
- **Docker & Docker Compose** - Containerization
- **Kubernetes** - Container orchestration
- **Helm** - Kubernetes package manager
- **Netflix Eureka** - Service discovery
- **Spring Cloud Gateway** - API Gateway with rate limiting

### Monitoring & Observability
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization
- **Zipkin** - Distributed tracing
- **ELK Stack** - Logging (Elasticsearch, Logstash, Kibana)
- **Spring Boot Actuator** - Health checks and metrics

### Security
- **Spring Security** - Authentication and authorization
- **OAuth2** - Authorization framework
- **JWT** - Stateless token-based auth
- **BCrypt** - Password hashing

### Testing
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Testcontainers** - Integration testing with containers
- **Postman** - API testing (80+ endpoints included)

### Resilience & Reliability
- **Resilience4j** - Circuit breaker, retry, rate limiting
- **Spring Retry** - Retry logic
- **Caffeine** - In-memory caching

## 🏗️ Architecture Overview

This platform consists of **10 microservices** (3 infrastructure + 7 business) implementing enterprise-grade patterns:

### Infrastructure Services
- **API Gateway** (Port 8080) - Single entry point, routing, rate limiting, circuit breaking
- **Service Discovery** (Port 8761) - Netflix Eureka for service registration
- **Config Server** (Port 8888) - Centralized configuration management

### Business Services
- **Auth Service** (Port 9001) - OAuth2, JWT, Social Login, 2FA, Multi-tenancy
- **User Service** (Port 9002) - CQRS pattern, PostgreSQL replication
- **Product Service** (Port 9003) - Event Sourcing, MongoDB sharding
- **Order Service** (Port 9004) - Saga pattern for distributed transactions
- **Payment Service** (Port 9005) - Circuit Breaker, Resilience patterns
- **Notification Service** (Port 9006) - Multithreading, WebSocket
- **Review Service** (Port 9007) - gRPC communication

## 🚀 Quick Start

### Prerequisites
- **Docker Desktop** (latest version)
- **Postman** (for API testing)
- **Java 21** (optional, only needed for development)

### Choose Your Setup

| Setup Option | RAM Required | Startup Time | Services | Use Case |
|-------------|--------------|--------------|----------|----------|
| **Minimal** | 2-3 GB | 8-10 min | 5 core + 2 DBs | Learning, testing basic features |
| **Light** | 6-8 GB | 10-12 min | 7 services + infrastructure | Development, most features |
| **Full** | 12-16 GB | 10-15 min | All 10 services + monitoring | Complete experience, production simulation |
| **Cloud** | 0 GB (remote) | Varies | All services | Production, 24/7 availability |

#### Option 1: Minimal Setup (Recommended for < 8GB Docker RAM)
**Perfect for:** Testing, learning, low-resource systems  
**Includes:** Auth, User, Product, Order services + PostgreSQL, MongoDB

```bash
./start-minimal.sh
```

#### Option 2: Light Setup (Recommended for 8-12GB Docker RAM)
**Perfect for:** Development without monitoring stack  
**Includes:** All 7 business services + infrastructure services + databases

```bash
./start-local-light.sh
```

#### Option 3: Full Setup (Requires 12-16GB Docker RAM)
**Perfect for:** Full-stack development, production simulation  
**Includes:** All microservices + Kafka + Monitoring (Prometheus, Grafana, Zipkin, ELK)

```bash
./start-local.sh
```

#### Option 4: Cloud Deployment (Zero local resources!)
**Perfect for:** Production, 24/7 availability  
**Includes:** All services deployed on cloud infrastructure  
See **[CLOUD_DEPLOYMENT_GUIDE.md](CLOUD_DEPLOYMENT_GUIDE.md)** for Railway, AWS, GCP, Azure

**📖 Detailed guide:** [QUICK_START.md](QUICK_START.md) | [DEPLOYMENT_OPTIONS.md](DEPLOYMENT_OPTIONS.md)

### Test with Postman

```bash
# 1. Import collection
postman/E-commerce-Microservices.postman_collection.json

# 2. Import environment
postman/Local.postman_environment.json

# 3. Select "Local Environment" (top right)

# 4. Test APIs:
- Run "Register" to create user
- Run "Login" (JWT auto-saves!)
- Run any API endpoint
```

See **[POSTMAN_GUIDE.md](POSTMAN_GUIDE.md)** for detailed testing instructions.

### Stop All Services

```bash
./stop-local.sh
```

## 🌐 Service URLs (After Startup)

### Infrastructure
- **Eureka Dashboard:** http://localhost:8761
- **API Gateway:** http://localhost:8080
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)
- **Zipkin:** http://localhost:9411
- **Kibana:** http://localhost:5601
- **Kafka UI:** http://localhost:8090

### Microservices (via API Gateway)
All APIs accessed through: `http://localhost:8080/api/...`
- Auth: `/api/auth`
- Users: `/api/users`
- Products: `/api/products`
- Orders: `/api/orders`
- Payments: `/api/payments`
- Notifications: `/api/notifications`
- Reviews: `/api/reviews`

### Databases (Direct Access)
- **PostgreSQL:** `localhost:5432` (postgres/postgres)
- **MongoDB:** `localhost:27017` (admin/admin123)
- **Redis:** `localhost:6379`

## 📦 What's Running in Docker?

After `./start-local.sh`, you have **10 microservices** running in separate containers:

1. `ecommerce-eureka` - Service Discovery
2. `ecommerce-config-server` - Configuration Management
3. `ecommerce-api-gateway` - API Gateway
4. `ecommerce-auth-service` - Authentication
5. `ecommerce-user-service` - User Management
6. `ecommerce-product-service` - Product Catalog
7. `ecommerce-order-service` - Order Processing
8. `ecommerce-payment-service` - Payments
9. `ecommerce-notification-service` - Notifications
10. `ecommerce-review-service` - Reviews

Plus infrastructure: PostgreSQL, MongoDB, Redis, Kafka, Zipkin, Prometheus, Grafana, ELK Stack

## 🛠️ Manual Build & Run (Development)

```bash
# 1. Start Service Discovery first (other services need this)
./gradlew :infrastructure:service-discovery:bootRun

# Wait for Eureka to start (check http://localhost:8761)

# 2. Start Config Server
./gradlew :infrastructure:config-server:bootRun

# 3. Start API Gateway
./gradlew :infrastructure:api-gateway:bootRun

# 4. Start Business Services (can be started in parallel)
./gradlew :services:auth-service:bootRun
./gradlew :services:user-service:bootRun
./gradlew :services:product-service:bootRun
./gradlew :services:order-service:bootRun
./gradlew :services:payment-service:bootRun
./gradlew :services:notification-service:bootRun
./gradlew :services:review-service:bootRun
```

**Or use multiple terminal windows/tabs for parallel startup**

### Step 4: Verify Deployment

1. **Eureka Dashboard**: `http://localhost:8761`
   - Should show all services registered
   
2. **API Gateway Health**: `http://localhost:8080/actuator/health`
   - Should return `{"status":"UP"}`
   
3. **Zipkin Tracing**: `http://localhost:9411`
   - View distributed traces
   
4. **Kafka UI**: `http://localhost:8090`
   - View topics and messages

## 🧪 API Testing with Postman

This project includes a comprehensive Postman collection with **80+ API endpoints**, auto-authentication, sample data, and test scripts.

### Quick Test Flow

**1. Import Collection**
```
File: postman/E-commerce-Microservices.postman_collection.json
```

**2. Import Environment**
```
File: postman/Local.postman_environment.json
```

**3. Select Environment**
- Click environment dropdown (top right)
- Select "Local Environment"

**4. Test Complete User Journey**

```
Step 1: Authentication → Register
  POST /api/auth/register
  Creates user account

Step 2: Authentication → Login
  POST /api/auth/login
  Returns JWT (auto-saved to {{token}})

Step 3: User Service → Add Address
  POST /api/users/me/addresses
  Adds shipping address

Step 4: Product Service → Create Product (Admin)
  POST /api/products
  Creates product (saves {{testProductId}})

Step 5: Order Service → Create Order
  POST /api/orders
  Triggers Saga: Reserve Inventory → Process Payment → Confirm Order

Step 6: Order Service → Get Order
  GET /api/orders/{{testOrderId}}
  View order status

Step 7: Review Service → Create Review
  POST /api/reviews
  Write product review
```

### Features

✅ **Auto-Authentication**: JWT tokens auto-save after login
✅ **Smart Variables**: Response data auto-saves (userId, orderId, productId)
✅ **Token Refresh**: Expired tokens auto-refresh
✅ **Test Scripts**: Every request has automated validation
✅ **Sample Data**: All requests include realistic sample data

### View Results

After each request:
- **Body**: Response data
- **Test Results**: Automated tests (✓ passed / ✗ failed)
- **Console**: Detailed logs

### Complete Guide

See **[POSTMAN_GUIDE.md](POSTMAN_GUIDE.md)** for:
- Detailed API documentation
- Troubleshooting guide
- Advanced features
- Environment switching

## 📚 Key Concepts Covered

### 1. Microservices Architecture
- **Service Decomposition**: Breaking monolith into independent services
- **Service Discovery**: Dynamic service registration with Eureka
- **API Gateway**: Single entry point for all client requests
- **Inter-Service Communication**: REST, gRPC, Kafka messaging
- **Database per Service**: Each service owns its data

### 2. OAuth2 & Authentication (Auth Service)
- **OAuth2 Authorization Server**: All grant types implemented
- **JWT Tokens**: Access tokens, refresh tokens, ID tokens
- **Social Login**: Google, GitHub, Facebook integration
- **API Key Management**: For service-to-service auth
- **Multi-Tenancy**: Tenant isolation and data segregation
- **Two-Factor Authentication**: TOTP, SMS, Email verification

### 3. Multithreading & Concurrency (Notification Service)
- **Thread Pools**: ExecutorService, custom thread pool configuration
- **CompletableFuture**: Async programming patterns
- **Virtual Threads**: Java 21+ lightweight threads
- **Parallel Streams**: Batch processing
- **Synchronization**: Locks, Semaphores, Latches
- **Thread-Safe Collections**: ConcurrentHashMap, BlockingQueue

### 4. System Design Patterns

#### Resilience Patterns
- **Circuit Breaker**: Prevent cascade failures (Resilience4j)
- **Retry**: Exponential backoff for transient failures
- **Bulkhead**: Resource isolation
- **Timeout**: Prevent hanging requests

#### Data Patterns
- **CQRS**: Command Query Responsibility Segregation (User Service)
- **Event Sourcing**: Store all changes as events (Product Service)
- **Saga Pattern**: Distributed transactions (Order Service)
- **Outbox Pattern**: Reliable event publishing
- **Database Replication**: Primary-Replica (PostgreSQL)
- **Database Sharding**: MongoDB sharding by category

#### API Patterns
- **Rate Limiting**: Token Bucket algorithm (API Gateway)
- **Load Balancing**: Round Robin, Least Connections
- **Caching**: Redis distributed cache (Cache-Aside, Write-Through)
- **API Versioning**: URL, Header, Content Negotiation

### 5. Distributed Systems
- **CAP Theorem**: Consistency, Availability, Partition Tolerance tradeoffs
- **Eventual Consistency**: MongoDB, event-driven updates
- **Idempotency**: Preventing duplicate operations
- **Distributed Tracing**: Zipkin for request tracking
- **Service Mesh**: (Future: Istio/Linkerd)

### 6. Message Queue (Kafka)
- **Event-Driven Architecture**: Asynchronous communication
- **Topics**: `order.created`, `payment.processed`, etc.
- **Consumer Groups**: Parallel processing
- **Dead Letter Queue**: Failed message handling
- **Event Sourcing**: Complete audit trail

### 7. gRPC Communication (Review Service)
- **Protocol Buffers**: Efficient serialization
- **Streaming**: Server streaming, client streaming, bidirectional
- **Service-to-Service**: High-performance RPC calls

### 8. WebSocket (Notification Service)
- **Real-Time Updates**: Push notifications to clients
- **STOMP Protocol**: WebSocket subprotocol
- **Kafka-WebSocket Bridge**: Event streaming to clients

## 🗄️ Database Strategy

### PostgreSQL (Relational Data)
- **Auth Service**: Users, roles, permissions, OAuth2 tokens
- **User Service**: User profiles, addresses, preferences
- **Order Service**: Orders, order items, order history
- **Payment Service**: Payments, transactions, refunds

**Features Used**:
- Primary-Replica replication
- Optimistic locking
- ACID transactions
- Foreign key constraints

### MongoDB (Document Data)
- **Product Service**: Products (dynamic schema), categories, inventory
- **Review Service**: Reviews, ratings, aggregations
- **Notification Service**: Notifications, templates, preferences

**Features Used**:
- Sharding (by category)
- Replica sets
- Text indexes for search
- Aggregation pipelines

### Redis (Cache & Sessions)
- Distributed caching (products, users)
- Rate limiting counters
- Session storage
- Pub/Sub for cache invalidation

## 🔐 Security

- **JWT Authentication**: Stateless authentication
- **BCrypt Password Hashing**: Secure password storage
- **CORS Configuration**: Cross-origin resource sharing
- **Input Validation**: JSR-380 Bean Validation
- **SQL Injection Prevention**: Parameterized queries
- **XSS Protection**: Output encoding

## 📊 Monitoring & Observability

### Metrics (Prometheus + Grafana)
- Request rates, latencies, error rates
- JVM metrics (heap, GC, threads)
- Database connection pool metrics
- Custom business metrics

### Distributed Tracing (Zipkin)
- Trace requests across services
- Identify bottlenecks
- Visualize service dependencies
- Measure latency at each hop

### Logging
- Structured logging with SLF4J + Logback
- Correlation IDs for request tracking
- Log levels per service
- (Future: ELK Stack for log aggregation)

### Health Checks
- Spring Boot Actuator `/actuator/health`
- Custom health indicators
- Readiness and liveness probes (for Kubernetes)

## 🧪 Testing

### Unit Tests
- JUnit 5 + Mockito
- 80%+ code coverage target
- Mock external dependencies

### Integration Tests
- @SpringBootTest
- Testcontainers (PostgreSQL, MongoDB, Kafka)
- Test full request/response flow

### Contract Tests
- Spring Cloud Contract
- Producer-consumer contract verification

### Load Tests
- Gatling / JMeter
- Measure throughput and latency
- Identify performance bottlenecks

## 📖 API Documentation

### Swagger UI (OpenAPI 3.0)
Each service exposes Swagger UI at:
```
http://localhost:{port}/swagger-ui.html
```

Example:
- Auth Service: `http://localhost:9001/swagger-ui.html`
- User Service: `http://localhost:9002/swagger-ui.html`

### API Endpoints

All requests go through API Gateway: `http://localhost:8080/api/*`

#### Authentication
```bash
# Register new user
POST /api/auth/register
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "John Doe"
}

# Login
POST /api/auth/login
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

# Response
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 86400
}
```

#### Products
```bash
# List products
GET /api/products?page=0&size=20&sort=price,desc
Authorization: Bearer {accessToken}

# Get product details
GET /api/products/{id}

# Create product (Admin only)
POST /api/products
Authorization: Bearer {adminToken}
Content-Type: application/json
{
  "name": "Laptop",
  "price": 999.99,
  "category": "Electronics",
  "description": "High-performance laptop",
  "inventory": 50
}
```

#### Orders
```bash
# Create order
POST /api/orders
Authorization: Bearer {accessToken}
Content-Type: application/json
{
  "items": [
    {
      "productId": "prod-123",
      "quantity": 2
    }
  ],
  "shippingAddressId": "addr-456",
  "paymentMethodId": "pm-789"
}

# Get order status
GET /api/orders/{orderId}
Authorization: Bearer {accessToken}
```

## 🏗️ Project Structure

```
backend-learning/
├── infrastructure/                    # Infrastructure services
│   ├── api-gateway/                   # API Gateway (Spring Cloud Gateway)
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── build.gradle
│   ├── service-discovery/             # Eureka Server
│   │   ├── src/
│   │   ├── Dockerfile
│   │   └── build.gradle
│   └── config-server/                 # Centralized Config Server
│       ├── src/
│       ├── Dockerfile
│       └── build.gradle
├── services/                          # Business microservices
│   ├── auth-service/                  # Authentication & Authorization
│   │   ├── src/main/java/com/       # OAuth2, JWT, Social Login, 2FA
│   │   ├── src/main/resources/       # application.yml, db migrations
│   │   ├── Dockerfile
│   │   ├── build.gradle
│   │   └── README.md
│   ├── user-service/                  # User Management (CQRS pattern)
│   │   ├── src/main/java/com/
│   │   ├── src/main/resources/
│   │   ├── Dockerfile
│   │   └── build.gradle
│   ├── product-service/               # Product Catalog (Event Sourcing)
│   │   ├── src/main/java/com/
│   │   ├── src/main/resources/
│   │   └── build.gradle
│   ├── order-service/                 # Order Management (Saga pattern)
│   │   ├── src/main/java/com/
│   │   ├── src/main/resources/
│   │   └── build.gradle
│   ├── payment-service/               # Payment Processing (Circuit Breaker)
│   │   ├── src/main/java/com/
│   │   ├── src/main/resources/
│   │   └── build.gradle
│   ├── notification-service/          # Notifications (Multithreading, WebSocket)
│   │   ├── src/main/java/com/
│   │   ├── src/main/resources/
│   │   └── build.gradle
│   └── review-service/                # Reviews & Ratings (gRPC)
│       ├── src/main/java/com/
│       ├── src/proto/                 # Protocol Buffers definitions
│       ├── src/main/resources/
│       └── build.gradle
├── shared/                            # Shared libraries
│   ├── common-lib/                    # Common utilities, DTOs, exceptions
│   │   ├── src/main/java/com/
│   │   └── build.gradle
│   └── event-lib/                     # Event definitions for Kafka
│       ├── src/main/java/com/
│       └── build.gradle
├── docker/                            # Docker configurations
│   ├── docker-compose.yml             # Full stack (21 containers)
│   ├── docker-compose.minimal.yml     # Minimal setup (7 containers)
│   ├── init-scripts/                  # Database initialization scripts
│   │   └── init-postgres.sh
│   ├── logstash/
│   │   └── logstash.conf
│   └── prometheus/
│       └── prometheus.yml
├── k8s/                               # Kubernetes manifests
│   ├── namespace.yaml
│   ├── secrets.yaml
│   ├── ingress.yaml
│   ├── auth-service.yaml
│   └── order-service.yaml
├── helm/                              # Helm charts
│   └── ecommerce/
│       ├── Chart.yaml
│       └── values.yaml
├── postman/                           # Postman API collections
│   ├── E-commerce-Microservices.postman_collection.json
│   ├── Local.postman_environment.json
│   ├── Development.postman_environment.json
│   └── Production.postman_environment.json
├── Startup & Deployment Scripts (root level)
│   ├── start-minimal.sh               # Start minimal stack (2-3GB RAM)
│   ├── start-local.sh                 # Start full stack (12-16GB RAM)
│   ├── start-local-light.sh           # Start light stack (6-8GB RAM)
│   ├── stop-local.sh                  # Stop all services
│   ├── build-all-docker-images.sh     # Build all Docker images
│   └── deploy-kubernetes.sh           # Deploy to K8s cluster
├── Documentation/                     # Complete documentation (14 guides)
│   ├── START_HERE.md
│   ├── QUICK_START.md
│   ├── ARCHITECTURE.md
│   ├── AUTH_GUIDE.md
│   ├── MULTITHREADING_GUIDE.md
│   ├── SYSTEM_DESIGN_PATTERNS.md
│   ├── API_DOCUMENTATION.md
│   ├── DATABASE_SCHEMA.md
│   ├── DEPLOYMENT.md
│   ├── DEPLOYMENT_OPTIONS.md
│   ├── CLOUD_DEPLOYMENT_GUIDE.md
│   ├── TESTING_GUIDE.md
│   ├── POSTMAN_GUIDE.md
│   ├── POSTMAN_SETUP_GUIDE.md
│   ├── VERIFICATION_SUMMARY.md
│   ├── PROJECT_STATUS.md
│   └── FINAL_STATUS.md
├── build.gradle                       # Root Gradle build file
├── settings.gradle                    # Multi-module project settings
├── gradlew                            # Gradle wrapper (Unix)
├── gradlew.bat                        # Gradle wrapper (Windows)
├── .gitignore                         # Git ignore patterns
└── README.md                          # This file
```

### Key Directories Explained

- **`infrastructure/`** - Core platform services that other services depend on
- **`services/`** - Business domain microservices (7 services)
- **`shared/`** - Reusable libraries shared across services
- **`docker/`** - Docker Compose files and container configurations
- **`k8s/`** - Kubernetes deployment manifests
- **`helm/`** - Helm charts for K8s deployment
- **`postman/`** - API testing collections with 80+ endpoints
- **`build/`** - Generated build artifacts (ignored in git)

## 🔧 Development

### Build Specific Service
```bash
./gradlew :services:auth-service:build
```

### Run Tests
```bash
# All tests
./gradlew test

# Specific service tests
./gradlew :services:auth-service:test
```

### Clean Build
```bash
./gradlew clean build
```

### Check Dependencies
```bash
./gradlew dependencies
```

## 📚 Learning Resources

### Documentation Files
1. **ARCHITECTURE.md** - Detailed architecture explanation
2. **AUTH_GUIDE.md** - OAuth2, JWT, authentication concepts
3. **MULTITHREADING_GUIDE.md** - Threading, concurrency, async programming
4. **SYSTEM_DESIGN_PATTERNS.md** - All design patterns explained
5. **API_DOCUMENTATION.md** - Complete API reference
6. **DATABASE_SCHEMA.md** - Database schemas and relationships
7. **DEPLOYMENT.md** - Production deployment guide
8. **TESTING_GUIDE.md** - Testing strategies and examples

## 🐛 Troubleshooting

### Common Issues & Solutions

#### Docker Memory Issues
**Problem:** Services crashing with "Out of Memory" errors
**Solution:** 
```bash
# Check Docker memory allocation
docker stats

# Increase Docker Desktop memory to 12-16GB
# Docker Desktop → Settings → Resources → Memory

# Or use minimal setup
./start-minimal.sh
```
See **[DOCKER_MEMORY_FIX.md](DOCKER_MEMORY_FIX.md)** for detailed solutions.

#### Service Not Registering with Eureka
**Problem:** Services not appearing in Eureka dashboard
**Solution:**
```bash
# 1. Verify Eureka is running
curl http://localhost:8761

# 2. Check service logs
docker logs ecommerce-auth-service

# 3. Verify eureka.client.serviceUrl in application.yml
# 4. Ensure proper startup order (Eureka → Config → Other services)
```

#### Database Connection Issues
**Problem:** Services can't connect to PostgreSQL/MongoDB
**Solution:**
```bash
# 1. Check containers are running
docker-compose ps

# 2. Test PostgreSQL connection
docker exec -it ecommerce-postgres psql -U postgres -c "SELECT version();"

# 3. Test MongoDB connection
docker exec -it ecommerce-mongodb mongosh -u admin -p admin123 --eval "db.version()"

# 4. Check database credentials in service application.yml
# 5. Restart services if database started after service
docker-compose restart auth-service user-service order-service
```

#### Kafka Connection Issues
**Problem:** Services can't connect to Kafka
**Solution:**
```bash
# 1. Check Zookeeper is running
docker logs ecommerce-zookeeper

# 2. Check Kafka is running
docker logs ecommerce-kafka

# 3. List topics
docker exec -it ecommerce-kafka kafka-topics --list --bootstrap-server localhost:9092

# 4. Check consumer groups
docker exec -it ecommerce-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# 5. Restart in correct order
docker-compose restart zookeeper kafka
```

#### Port Already in Use
**Problem:** `Port 8080 already in use` or similar
**Solution:**
```bash
# Find process using port
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill process or change port in docker-compose.yml
# Or stop all services first
./stop-local.sh
```

#### API Gateway Not Routing
**Problem:** 404 errors when accessing `/api/*` endpoints
**Solution:**
```bash
# 1. Check Gateway is running
curl http://localhost:8080/actuator/health

# 2. Verify Eureka shows all services
open http://localhost:8761

# 3. Check Gateway logs
docker logs ecommerce-api-gateway

# 4. Test direct service access (bypass gateway)
curl http://localhost:9001/actuator/health  # Auth Service
```

#### JWT Token Issues
**Problem:** "Invalid token" or "Token expired" errors
**Solution:**
```bash
# 1. Get new token via Postman "Login" request
# 2. Verify token in environment variables
# 3. Check token expiration time in auth-service/application.yml
# 4. For development, increase token expiry:
#    jwt.expiration: 86400000  # 24 hours
```

#### Services Won't Start
**Problem:** Services fail during startup
**Solution:**
```bash
# 1. Check logs for specific error
docker-compose logs -f [service-name]

# 2. Clean build and restart
./stop-local.sh
docker-compose down -v  # Remove volumes
./gradlew clean build
./start-local.sh

# 3. Try minimal setup first
./start-minimal.sh
```

### Getting Additional Help

1. **Check Documentation:** Review relevant guide in documentation folder
2. **View Logs:** `docker-compose logs -f [service-name]`
3. **Check Eureka:** Verify service registration at http://localhost:8761
4. **Health Checks:** Test each service health endpoint
5. **Postman Tests:** Run Postman collection to verify APIs
6. **Verification Guide:** See **[VERIFICATION_SUMMARY.md](VERIFICATION_SUMMARY.md)**

## 🚀 Production Deployment

### Deployment Scripts Available

```bash
# Build Docker images for all services
./build-all-docker-images.sh

# Deploy to Kubernetes cluster
./deploy-kubernetes.sh

# Use different compose files
docker-compose -f docker/docker-compose.yml up -d        # Full stack
docker-compose -f docker/docker-compose.minimal.yml up -d # Minimal
```

### Docker Deployment
Build all services as Docker images:
```bash
# Using Gradle
./gradlew bootBuildImage

# Or use build script
./build-all-docker-images.sh

# Push to registry
docker tag ecommerce-auth-service:latest myregistry/auth-service:latest
docker push myregistry/auth-service:latest
```

### Kubernetes Deployment
Deploy to Kubernetes cluster:
```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/

# Or use deployment script
./deploy-kubernetes.sh

# Using Helm
helm install ecommerce ./helm/ecommerce
```

### Cloud Deployment Options

This project supports multiple cloud platforms. See **[CLOUD_DEPLOYMENT_GUIDE.md](CLOUD_DEPLOYMENT_GUIDE.md)** for detailed instructions:

1. **Railway** - Easiest deployment, $5/month per service
2. **AWS** - ECS/EKS with comprehensive AWS services
3. **Google Cloud Platform** - GKE with managed services
4. **Azure** - AKS with Azure services
5. **DigitalOcean** - Kubernetes with affordable pricing
6. **Heroku** - Simple container deployment

### CI/CD Pipeline (Coming Soon)
- GitHub Actions workflow in `.github/workflows/`
- Automated testing on PR
- Automated deployment to staging/production
- Blue-green deployment strategy
- Automated rollback on failures

### Environment-Specific Configuration

Use different Postman environments for testing:
- **Local.postman_environment.json** - localhost:8080
- **Development.postman_environment.json** - dev environment
- **Production.postman_environment.json** - production environment

See **[DEPLOYMENT_OPTIONS.md](DEPLOYMENT_OPTIONS.md)** and **[DEPLOYMENT.md](DEPLOYMENT.md)** for complete deployment guides.

## 🎯 Quick Command Reference

### Startup & Shutdown
```bash
# Start services
./start-minimal.sh          # Minimal setup (2-3GB)
./start-local-light.sh      # Light setup (6-8GB)
./start-local.sh            # Full setup (12-16GB)

# Stop all services
./stop-local.sh

# View running containers
docker-compose ps

# View logs
docker-compose logs -f [service-name]
docker-compose logs -f auth-service
```

### Build Commands
```bash
# Build all services
./gradlew clean build

# Build specific service
./gradlew :services:auth-service:build

# Build Docker images
./build-all-docker-images.sh

# Run tests
./gradlew test
./gradlew :services:auth-service:test
```

### Docker Commands
```bash
# View container stats
docker stats

# Restart specific service
docker-compose restart auth-service

# View container logs
docker logs ecommerce-auth-service -f

# Execute command in container
docker exec -it ecommerce-postgres psql -U postgres
docker exec -it ecommerce-mongodb mongosh -u admin -p admin123

# Remove all containers and volumes
docker-compose down -v
```

### Kafka Commands
```bash
# List topics
docker exec -it ecommerce-kafka kafka-topics --list --bootstrap-server localhost:9092

# View topic messages
docker exec -it ecommerce-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created --from-beginning

# List consumer groups
docker exec -it ecommerce-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --list
```

### Database Commands
```bash
# PostgreSQL
docker exec -it ecommerce-postgres psql -U postgres -d authdb
docker exec -it ecommerce-postgres psql -U postgres -c "SELECT * FROM users;"

# MongoDB
docker exec -it ecommerce-mongodb mongosh -u admin -p admin123
docker exec -it ecommerce-mongodb mongosh --eval "db.products.find().pretty()"

# Redis
docker exec -it ecommerce-redis redis-cli
docker exec -it ecommerce-redis redis-cli KEYS "*"
```

### Health Checks
```bash
# Check service health
curl http://localhost:8080/actuator/health        # API Gateway
curl http://localhost:9001/actuator/health        # Auth Service
curl http://localhost:8761                        # Eureka

# Check all registered services
curl http://localhost:8761/eureka/apps

# View metrics
curl http://localhost:8080/actuator/metrics
```

### Kubernetes Commands
```bash
# Deploy to K8s
./deploy-kubernetes.sh

# Or manually
kubectl apply -f k8s/

# View pods
kubectl get pods -n ecommerce

# View logs
kubectl logs -f deployment/auth-service -n ecommerce

# Port forward
kubectl port-forward svc/api-gateway 8080:8080 -n ecommerce
```

### Gradle Commands
```bash
# List all tasks
./gradlew tasks

# Check dependencies
./gradlew dependencies

# Run specific service
./gradlew :services:auth-service:bootRun

# Clean build artifacts
./gradlew clean
```

## 📝 License

This project is created for educational purposes to learn Spring Boot and microservices architecture.

## 👨‍💻 Author

Created by **Aditya Raj** - Backend Developer learning Spring Boot Microservices

**Project Type:** Educational & Portfolio Project  
**Purpose:** Comprehensive demonstration of microservices architecture, system design patterns, and modern backend development practices

## 🙏 Acknowledgments

This project was built using amazing open-source technologies:

- **Spring Framework** - Spring Boot, Spring Cloud, Spring Security teams
- **Netflix OSS** - Eureka (Service Discovery)
- **Resilience4j** - Fault tolerance library
- **Apache Kafka** - Event streaming platform
- **Docker** - Containerization platform
- **Kubernetes** - Container orchestration
- **PostgreSQL** - Robust relational database
- **MongoDB** - Flexible document database
- **Redis** - High-performance caching
- All open-source contributors who make amazing tools freely available

---

## 📞 Support & Help

### Quick Help Resources

1. **Start Here:** Read [START_HERE.md](START_HERE.md) for absolute beginners
2. **Quick Start:** Follow [QUICK_START.md](QUICK_START.md) for fast setup
3. **Memory Issues:** See [DOCKER_MEMORY_FIX.md](DOCKER_MEMORY_FIX.md)
4. **API Testing:** Review [POSTMAN_SETUP_GUIDE.md](POSTMAN_SETUP_GUIDE.md)
5. **Troubleshooting:** Check the [Troubleshooting](#-troubleshooting) section above
6. **Verification:** Run through [VERIFICATION_SUMMARY.md](VERIFICATION_SUMMARY.md)

### Debug Checklist

When something doesn't work:
- ✅ Check if Docker Desktop is running with adequate memory (8GB+)
- ✅ Verify Eureka dashboard shows all services: http://localhost:8761
- ✅ Check service logs: `docker-compose logs -f [service-name]`
- ✅ Test health endpoints: `curl http://localhost:8080/actuator/health`
- ✅ Review inline code comments for explanations
- ✅ Check relevant documentation in the docs folder

### Service Health Dashboard URLs

After starting services, check these URLs:
- **Eureka:** http://localhost:8761 - See all registered services
- **API Gateway:** http://localhost:8080/actuator/health
- **Zipkin:** http://localhost:9411 - Distributed tracing
- **Prometheus:** http://localhost:9090 - Metrics
- **Grafana:** http://localhost:3000 - Dashboards (admin/admin)
- **Kafka UI:** http://localhost:8090 - Kafka topics

---

**Note**: This is a comprehensive learning project designed for education. Every service, pattern, and concept is implemented with detailed documentation and comments. Perfect for college students, bootcamp graduates, and developers learning backend development and microservices architecture.

**Learning Approach:** Start with minimal setup → Understand core concepts → Expand to full setup → Deploy to cloud

**Estimated Learning Time:** 
- Basic understanding: 1-2 weeks
- Intermediate mastery: 1 month  
- Advanced implementation: 2-3 months

