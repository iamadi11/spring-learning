# Getting Started with E-commerce Microservices Platform

Welcome to the comprehensive e-commerce microservices learning project! This guide will help you get started quickly.

## 🚀 Quick Start (5 minutes)

### Prerequisites
- Java 21 installed
- Docker and Docker Compose installed
- Terminal/Command prompt

### Step 1: Start Infrastructure Services

```bash
# Navigate to project directory
cd "/Users/adityaraj/Desktop/My Projects/backend-learning"

# Start all infrastructure (PostgreSQL, MongoDB, Redis, Kafka, etc.)
cd docker
docker-compose up -d

# Verify all containers are running (should see 10 containers)
docker-compose ps
```

**Expected Output:**
```
NAME                      STATUS    PORTS
ecommerce-postgres        Up        0.0.0.0:5432->5432/tcp
ecommerce-mongodb         Up        0.0.0.0:27017->27017/tcp
ecommerce-redis           Up        0.0.0.0:6379->6379/tcp
ecommerce-kafka           Up        0.0.0.0:9092->9092/tcp
ecommerce-zipkin          Up        0.0.0.0:9411->9411/tcp
...
```

### Step 2: Build the Project

```bash
# Return to project root
cd ..

# Build all modules
./gradlew clean build

# This will:
# - Compile all Java code
# - Run tests
# - Create JAR files
```

### Step 3: Start Core Services

**Terminal 1** - Service Discovery:
```bash
./gradlew :infrastructure:service-discovery:bootRun
```
Wait for: "Started EurekaServerApplication"
Dashboard: http://localhost:8761

**Terminal 2** - Config Server:
```bash
./gradlew :infrastructure:config-server:bootRun
```
Wait for: "Started ConfigServerApplication"

**Terminal 3** - API Gateway:
```bash
./gradlew :infrastructure:api-gateway:bootRun
```
Wait for: "Started GatewayApplication"
Gateway: http://localhost:8080

### Step 4: Verify Setup

Open your browser and check:

1. **Eureka Dashboard**: http://localhost:8761
   - Should show registered services

2. **API Gateway Health**: http://localhost:8080/actuator/health
   - Should return `{"status":"UP"}`

3. **Zipkin**: http://localhost:9411
   - Distributed tracing UI

4. **Kafka UI**: http://localhost:8090
   - View Kafka topics and messages

5. **Prometheus**: http://localhost:9090
   - Metrics monitoring

6. **Grafana**: http://localhost:3000
   - Login: admin/admin
   - Visualization dashboards

## 📁 Project Structure

```
ecommerce-microservices/
├── infrastructure/              # ✅ COMPLETED
│   ├── api-gateway/            # Port 8080
│   ├── service-discovery/      # Port 8761
│   └── config-server/          # Port 8888
│
├── services/                    # 🔄 IN PROGRESS
│   ├── auth-service/           # Port 9001 (partially complete)
│   ├── user-service/           # Port 9002 (to be built)
│   ├── product-service/        # Port 9003 (to be built)
│   ├── order-service/          # Port 9004 (to be built)
│   ├── payment-service/        # Port 9005 (to be built)
│   ├── notification-service/   # Port 9006 (to be built)
│   └── review-service/         # Port 9007 (to be built)
│
├── shared/                      # ✅ COMPLETED
│   ├── common-lib/             # Shared utilities
│   └── event-lib/              # Event definitions
│
└── docker/                      # ✅ COMPLETED
    ├── docker-compose.yml      # All infrastructure services
    └── init-scripts/           # Database initialization
```

## 🎯 What's Been Implemented

### ✅ Phase 1: Infrastructure (COMPLETE)
- Multi-module Gradle project
- Service Discovery (Eureka)
- Config Server
- API Gateway with routing, circuit breaker, rate limiting
- Shared libraries
- Docker Compose with PostgreSQL, MongoDB, Redis, Kafka, Zipkin, Prometheus, Grafana
- Comprehensive README

### 🔄 Phase 2: Auth Service (20% COMPLETE)
- Build configuration
- Application class with full documentation
- Configuration (application.yml)
- User entity (comprehensive example)

**What's Missing**:
- Other entities (Role, Permission, ApiKey, Tenant)
- Repositories
- Services (AuthService, JwtTokenService, etc.)
- Controllers
- Database migrations (Flyway)
- Tests

See `IMPLEMENTATION_STATUS.md` for complete details.

## 📚 Key Learning Files

These files are extensively documented to help you understand concepts:

1. **`infrastructure/service-discovery/src/main/java/.../EurekaServerApplication.java`**
   - Service Discovery explained
   - How Eureka works

2. **`infrastructure/config-server/src/main/java/.../ConfigServerApplication.java`**
   - Centralized configuration
   - Git vs. File system backends

3. **`infrastructure/api-gateway/src/main/java/.../GatewayApplication.java`**
   - API Gateway patterns
   - Routing, circuit breaker, rate limiting

4. **`infrastructure/api-gateway/src/main/java/.../config/GatewayConfig.java`**
   - Programmatic route configuration
   - Predicates and filters

5. **`services/auth-service/src/main/java/.../AuthServiceApplication.java`**
   - OAuth2 flows explained
   - JWT structure
   - 2FA implementation
   - Multi-tenancy

6. **`services/auth-service/src/main/java/.../entity/User.java`**
   - JPA annotations explained
   - Security best practices
   - Multi-tenancy
   - OAuth2 integration

7. **`shared/common-lib/src/main/java/.../dto/ApiResponse.java`**
   - Standard response wrapper
   - REST API best practices

8. **`shared/event-lib/src/main/java/.../events/OrderCreatedEvent.java`**
   - Event-driven architecture
   - Kafka event structure

## 🔧 Development Workflow

### Adding a New Service

Follow this pattern (using Auth Service as example):

1. **Create Directory Structure**:
   ```
   services/your-service/
   ├── build.gradle
   └── src/
       ├── main/
       │   ├── java/com/ecommerce/yourservice/
       │   │   ├── YourServiceApplication.java
       │   │   ├── config/
       │   │   ├── entity/ (or document/)
       │   │   ├── repository/
       │   │   ├── service/
       │   │   ├── controller/
       │   │   └── dto/
       │   └── resources/
       │       ├── application.yml
       │       └── db/migration/ (if using PostgreSQL)
       └── test/
   ```

2. **Add Dependencies** (in build.gradle):
   ```gradle
   dependencies {
       implementation 'org.springframework.boot:spring-boot-starter-web'
       implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
       // Add service-specific dependencies
   }
   ```

3. **Configure Application** (application.yml):
   ```yaml
   spring:
     application:
       name: your-service
   server:
     port: 90XX
   eureka:
     client:
       serviceUrl:
         defaultZone: http://localhost:8761/eureka/
   ```

4. **Implement Entities/Documents**:
   - Follow `User.java` pattern
   - Add comprehensive Javadoc
   - Explain every field and annotation

5. **Create Repositories**:
   ```java
   public interface YourRepository extends JpaRepository<YourEntity, String> {
       // Custom query methods
   }
   ```

6. **Implement Services**:
   ```java
   @Service
   @Transactional
   public class YourService {
       // Business logic with detailed comments
   }
   ```

7. **Create Controllers**:
   ```java
   @RestController
   @RequestMapping("/api/your-resource")
   public class YourController {
       // REST endpoints
   }
   ```

8. **Write Tests**:
   - Unit tests (Mockito)
   - Integration tests (Testcontainers)

9. **Run and Test**:
   ```bash
   ./gradlew :services:your-service:bootRun
   ```

## 🧪 Testing Your Changes

### Unit Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :services:auth-service:test

# Run with coverage
./gradlew test jacocoTestReport
```

### Integration Tests
```bash
# Tests use Testcontainers (requires Docker)
./gradlew integrationTest
```

### Manual Testing
```bash
# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "name": "Test User"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!"
  }'
```

## 📖 Documentation Files

1. **README.md** - Project overview and setup
2. **IMPLEMENTATION_STATUS.md** - Detailed progress tracking
3. **GETTING_STARTED.md** - This file
4. **Architecture Plan** - In `.cursor/plans/` directory

## 🎓 Learning Path

### Week 1-2: Infrastructure
- ✅ Understand Service Discovery
- ✅ Learn Config Server
- ✅ Study API Gateway patterns

### Week 3-4: Auth Service
- ✅ Learn OAuth2 and JWT
- 🔄 Implement remaining Auth Service components
- 🔄 Add social login
- 🔄 Implement 2FA

### Week 5: User Service
- Learn CQRS pattern
- Implement PostgreSQL replication
- Add Redis caching

### Week 6-7: Product Service
- Learn Event Sourcing
- Implement MongoDB sharding
- Add search functionality

### Week 8-9: Order Service
- Learn Saga pattern
- Implement distributed transactions
- Add Kafka events

### Week 10: Payment Service
- Learn Circuit Breaker
- Implement resilience patterns
- Add retry logic

### Week 11: Notification Service
- Learn multithreading
- Implement CompletableFuture
- Add WebSocket

### Week 12: Review Service
- Learn gRPC
- Implement Protocol Buffers
- Add REST + gRPC dual interface

### Week 13-20: Advanced Topics
- System design patterns integration
- Observability setup
- Testing
- Deployment

## 🐛 Troubleshooting

### Service Won't Start
```bash
# Check if port is already in use
lsof -i :9001  # Replace with your service port

# Check Eureka registration
curl http://localhost:8761/eureka/apps
```

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Test connection
docker exec -it ecommerce-postgres psql -U postgres -d auth_db

# Check logs
docker logs ecommerce-postgres
```

### Kafka Issues
```bash
# Check Kafka topics
docker exec -it ecommerce-kafka kafka-topics --list --bootstrap-server localhost:9092

# View consumer groups
docker exec -it ecommerce-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

## 📞 Need Help?

1. Check inline code comments (every file is heavily documented)
2. Review `IMPLEMENTATION_STATUS.md` for detailed component list
3. Study the implemented examples
4. Check Docker logs: `docker-compose logs -f {service-name}`
5. Verify Eureka dashboard: http://localhost:8761

## 🎉 Next Steps

1. **Complete Auth Service**:
   - Add missing entities, repositories, services
   - Implement OAuth2 flows
   - Add tests

2. **Start User Service**:
   - Follow Auth Service pattern
   - Implement CQRS
   - Add caching

3. **Continue Systematically**:
   - One service at a time
   - Test thoroughly
   - Document as you go

4. **Integrate Services**:
   - Test service-to-service communication
   - Verify Kafka events
   - Check distributed tracing

## 🔗 Useful Links

- Spring Boot Docs: https://spring.io/projects/spring-boot
- Spring Cloud Docs: https://spring.io/projects/spring-cloud
- OAuth2 Spec: https://oauth.net/2/
- JWT Spec: https://jwt.io/
- Kafka Docs: https://kafka.apache.org/documentation/
- gRPC Docs: https://grpc.io/docs/

---

**Happy Learning! You're building production-grade microservices! 🚀**

Remember: This is a learning journey. Take time to understand each concept. Every line of code is documented to help you learn.

