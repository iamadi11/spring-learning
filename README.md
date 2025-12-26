# E-commerce Microservices Platform

A comprehensive, production-grade e-commerce platform built with **Spring Boot microservices architecture**. This project demonstrates all major backend concepts, system design patterns, authentication strategies, multithreading, and distributed systems principles.

**✅ 100% COMPLETE** - All 13 phases implemented with full deployment infrastructure and comprehensive documentation!

## 📚 Complete Documentation (10,000+ lines)

This project includes comprehensive guides for every aspect:

### Core Guides
- **[README.md](README.md)** - Quick start guide (this file)
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Complete system architecture
- **[LEARNING_PATH.md](LEARNING_PATH.md)** - Week-by-week learning guide for beginners

### Technical Guides
- **[AUTH_GUIDE.md](AUTH_GUIDE.md)** - OAuth2, JWT, Social Login, 2FA comprehensive guide
- **[MULTITHREADING_GUIDE.md](MULTITHREADING_GUIDE.md)** - Threading & concurrency guide
- **[SYSTEM_DESIGN_PATTERNS.md](SYSTEM_DESIGN_PATTERNS.md)** - 20+ design patterns explained
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - All 80+ API endpoints documented
- **[DATABASE_SCHEMA.md](DATABASE_SCHEMA.md)** - PostgreSQL & MongoDB schemas

### Operations Guides
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Complete production deployment guide
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Comprehensive testing strategies
- **[POSTMAN_GUIDE.md](POSTMAN_GUIDE.md)** - API testing with Postman collection

## 🏗️ Architecture Overview

This platform consists of **10 microservices** implementing enterprise-grade patterns:

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

#### Option 1: Minimal Setup (Recommended for < 8GB Docker RAM)
**Perfect for:** Testing, learning, low-resource systems  
**Memory:** 2-3 GB | **Time:** 8-10 minutes | **Services:** 5 core + 2 DBs

```bash
./start-minimal.sh
```

#### Option 2: Full Setup (Requires 12-16GB Docker RAM)
**Perfect for:** Full-stack development, production simulation  
**Memory:** 12-16 GB | **Time:** 10-15 minutes | **Services:** All 21 containers

```bash
./start-local.sh
```

#### Option 3: Cloud Deployment (Zero local resources!)
**Perfect for:** Production, 24/7 availability  
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
ecommerce-microservices/
├── infrastructure/                    # Infrastructure services
│   ├── api-gateway/                   # API Gateway
│   ├── service-discovery/             # Eureka Server
│   └── config-server/                 # Config Server
├── services/                          # Business services
│   ├── auth-service/                  # Authentication & Authorization
│   ├── user-service/                  # User Management
│   ├── product-service/               # Product Catalog
│   ├── order-service/                 # Order Management
│   ├── payment-service/               # Payment Processing
│   ├── notification-service/          # Notifications
│   └── review-service/                # Reviews & Ratings
├── shared/                            # Shared libraries
│   ├── common-lib/                    # Common utilities
│   └── event-lib/                     # Event definitions
├── docker/                            # Docker configurations
│   ├── docker-compose.yml
│   ├── init-scripts/
│   └── prometheus/
├── build.gradle                       # Root build file
├── settings.gradle                    # Module definitions
└── README.md                          # This file
```

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

### Service Not Registering with Eureka
- Check if Eureka Server is running: `http://localhost:8761`
- Verify `eureka.client.serviceUrl.defaultZone` in service config
- Check service logs for connection errors

### Database Connection Issues
- Verify Docker containers are running: `docker-compose ps`
- Check database credentials in service configuration
- Test connection: `docker exec -it ecommerce-postgres psql -U postgres`

### Kafka Connection Issues
- Check Zookeeper is running
- Verify Kafka is started after Zookeeper
- Check Kafka topics: `docker exec -it ecommerce-kafka kafka-topics --list --bootstrap-server localhost:9092`

## 🚀 Production Deployment

### Docker
Build all services as Docker images:
```bash
./gradlew bootBuildImage
```

### Kubernetes
Deploy to Kubernetes cluster:
```bash
kubectl apply -f k8s/
```

### CI/CD
- GitHub Actions workflow in `.github/workflows/`
- Automated testing and deployment
- Blue-green deployment strategy

## 📝 License

This project is created for educational purposes to learn Spring Boot and microservices architecture.

## 👨‍💻 Author

E-commerce Platform Team - Learning Spring Boot Microservices

## 🙏 Acknowledgments

- Spring Boot and Spring Cloud teams
- Netflix OSS (Eureka, Hystrix)
- Resilience4j
- Apache Kafka
- All open-source contributors

---

**Note**: This is a comprehensive learning project. Every line of code is documented to explain concepts clearly. Perfect for college students and developers learning backend development.

## 📞 Support

For issues or questions:
- Check documentation files in the project
- Review inline code comments
- Check Eureka dashboard for service status
- View logs: `docker-compose logs -f {service-name}`

