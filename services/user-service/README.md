# User Service

## Overview

The User Service is responsible for managing extended user profile information beyond authentication data. It implements the **CQRS (Command Query Responsibility Segregation)** pattern with **PostgreSQL Primary-Replica Replication** for optimal scalability and performance.

## Architecture Highlights

### CQRS Pattern

```
┌──────────────────────────────────┐
│         User Service             │
├─────────────────┬────────────────┤
│  Command Side   │   Query Side   │
│  (Write Model)  │  (Read Model)  │
└────────┬────────┴────────┬───────┘
         │                  │
   ┌─────┴─────┐      ┌────┴────┐
   │  PRIMARY  │──────>│ REPLICA │
   │ Database  │ Sync  │Database │
   └───────────┘       └─────────┘
```

**Benefits**:
- **Scalability**: Read and write operations scale independently
- **Performance**: Optimized for read-heavy workloads (90% reads, 10% writes)
- **Flexibility**: Separate models for different use cases
- **Availability**: Read operations continue even if primary is temporarily unavailable

### Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL 15 with Primary-Replica Replication
- **Caching**: Redis for distributed caching
- **Messaging**: Kafka for event publishing
- **Security**: OAuth2 Resource Server (JWT validation)
- **Service Discovery**: Eureka Client
- **Migration**: Flyway
- **Testing**: JUnit 5, Testcontainers

## Features Implemented

### Core Features ✅
- ✅ **User Profile Management** - CRUD operations on extended user profiles
- ✅ **Address Management** - Multiple shipping/billing addresses per user
- ✅ **User Preferences** - Localization, notifications, display, privacy settings
- ✅ **CQRS Implementation** - Separate command and query operations
- ✅ **Database Routing** - Transaction-based routing to primary or replica
- ✅ **Caching Strategy** - Redis caching for read operations
- ✅ **REST API** - Comprehensive endpoints with validation
- ✅ **Profile Completion** - Automatic calculation of completion percentage

### Database Schema

```sql
user_profiles
- user_id (PK, from Auth Service)
- email, bio, avatar_url
- phone_number, phone_verified
- date_of_birth, gender
- profile_completion
- last_login
- created_at, updated_at

addresses
- id (PK)
- user_id (FK → user_profiles)
- type (SHIPPING, BILLING, BOTH)
- full_name, phone_number
- address_line1, address_line2
- city, state, postal_code, country
- is_default
- created_at, updated_at

user_preferences
- user_id (PK/FK → user_profiles)
- language, currency, timezone
- email_notifications, sms_notifications, push_notifications
- theme, items_per_page, product_view
- public_profile, searchable_profile, show_online_status
- created_at, updated_at
```

## API Endpoints

### User Profile Endpoints

#### Get Current User Profile
```http
GET /api/users/me
Authorization: Bearer {jwt_token}

Response 200:
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 200,
  "message": "Profile retrieved successfully",
  "data": {
    "userId": 123,
    "email": "user@example.com",
    "bio": "Software developer",
    "avatarUrl": "https://cdn.example.com/avatars/123.jpg",
    "phoneNumber": "+1234567890",
    "phoneVerified": true,
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "profileCompletion": 85,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-15T14:30:00"
  }
}
```

#### Update Current User Profile
```http
PUT /api/users/me
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "bio": "Updated bio text",
  "phoneNumber": "+1234567890",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE"
}

Response 200: Updated profile
```

#### Update Bio Only (Efficient)
```http
PATCH /api/users/me/bio
Authorization: Bearer {jwt_token}
Content-Type: text/plain

"My new biography text"

Response 200: Success message
```

#### Update Avatar URL
```http
PATCH /api/users/me/avatar
Authorization: Bearer {jwt_token}
Content-Type: text/plain

"https://cdn.example.com/avatars/123-new.jpg"

Response 200: Success message
```

#### Delete Current User Profile
```http
DELETE /api/users/me
Authorization: Bearer {jwt_token}

Response 200: Success message
```

### Admin Endpoints

#### Search Users (Paginated)
```http
GET /api/users?name=john&page=0&size=20&sort=createdAt,desc
Authorization: Bearer {jwt_token_with_admin_role}

Response 200:
{
  "content": [...],
  "totalElements": 1000,
  "totalPages": 50,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

#### Get Incomplete Profiles
```http
GET /api/users/incomplete?threshold=50&page=0&size=20
Authorization: Bearer {jwt_token_with_admin_role}

Response 200: Page of profiles with completion < 50%
```

## CQRS in Action

### Command Operations (Write)

All write operations use `UserProfileCommandService`:

```java
@Transactional  // readOnly = false (default)
public UserProfile updateProfile(Long userId, UserProfile updates) {
    // Routes to PRIMARY database
    // Evicts Redis cache
    // Publishes event to Kafka
}
```

**Flow**:
1. Controller receives PUT /api/users/me
2. Calls CommandService.updateProfile()
3. @Transactional starts transaction (readOnly = false)
4. RoutingDataSource detects readOnly = false
5. Routes to PRIMARY database
6. Update executed
7. Cache evicted (@CacheEvict)
8. Event published to Kafka (TODO)
9. Changes replicated to replica
10. Response returned

### Query Operations (Read)

All read operations use `UserProfileQueryService`:

```java
@Transactional(readOnly = true)  // Routes to REPLICA
@Cacheable(value = "userProfiles", key = "#userId")
public Optional<UserProfile> getProfile(Long userId) {
    // Routes to REPLICA database
    // Uses Redis cache
}
```

**Flow**:
1. Controller receives GET /api/users/me
2. Calls QueryService.getProfile()
3. @Cacheable checks Redis cache
4. If cache HIT: return from Redis (< 1ms)
5. If cache MISS:
   - @Transactional(readOnly = true) starts transaction
   - RoutingDataSource detects readOnly = true
   - Routes to REPLICA database
   - Query executed on replica
   - Result stored in Redis
6. Response returned

## Configuration

### Application Properties

```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  
  # CQRS Datasource Configuration
  datasource:
    primary:
      jdbc-url: jdbc:postgresql://localhost:5432/ecommerce_user_db
      username: postgres
      password: postgres
      hikari:
        maximum-pool-size: 10  # Writes
    
    replica:
      jdbc-url: jdbc:postgresql://localhost:5432/ecommerce_user_db
      username: postgres
      password: postgres
      hikari:
        maximum-pool-size: 20  # Reads (2x primary for read-heavy workload)
  
  # Redis Caching
  redis:
    host: localhost
    port: 6379
  
  cache:
    type: redis
    redis:
      time-to-live: 900000  # 15 minutes
  
  # Kafka
  kafka:
    bootstrap-servers: localhost:9092
```

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 15
- Redis
- Kafka (optional for events)

### Local Development

1. **Start PostgreSQL**:
```bash
docker run -d \
  --name postgres-user \
  -p 5432:5432 \
  -e POSTGRES_DB=ecommerce_user_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine
```

2. **Start Redis**:
```bash
docker run -d \
  --name redis-user \
  -p 6379:6379 \
  redis:alpine
```

3. **Run Service**:
```bash
./gradlew :services:user-service:bootRun
```

4. **Verify**:
```bash
curl http://localhost:8082/actuator/health
```

### Database Setup

Flyway automatically runs migrations on startup:
- V1__Initial_Schema.sql creates tables
- Triggers created for updated_at timestamps
- Constraints and checks added

## Caching Strategy

### Cache Configuration

```
userProfiles:
- Key: user:profile:{userId}
- TTL: 15 minutes
- Eviction: On update/delete

userProfilesByEmail:
- Key: user:email:{email}
- TTL: 15 minutes
- Eviction: On email change

userSearchResults:
- Key: users:search:{query}:{page}_{size}
- TTL: 5 minutes
- Eviction: On any profile update
```

### Cache Flow

```
Request → Redis Cache
            ↓ (MISS)
        Replica DB
            ↓
        Store in Cache
            ↓
        Response
```

## Testing

### Unit Tests
```bash
./gradlew :services:user-service:test --tests "*Test"
```

### Integration Tests
```bash
./gradlew :services:user-service:test --tests "*IntegrationTest"
```

## Performance Metrics

### Expected Performance
- **Cache Hit Rate**: 99%+
- **Read Latency**: < 1ms (cache hit), < 50ms (cache miss)
- **Write Latency**: < 100ms
- **Throughput**: 10,000+ req/sec (reads), 1,000+ req/sec (writes)

### Connection Pools
- **Primary**: 10 connections (write operations)
- **Replica**: 20 connections (read operations)
- **Ratio**: 1:2 (optimized for read-heavy workload)

## Event Publishing (TODO)

### Events to Publish
- `UserCreatedEvent` - When profile created
- `UserUpdatedEvent` - When profile updated
- `UserDeletedEvent` - When profile deleted

### Kafka Topics
- `user-events` - All user-related events

## Monitoring

### Metrics Endpoints
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

### Key Metrics
- Database connection pool utilization
- Cache hit rate
- Request latency
- Error rate

## Learning Concepts

### CQRS Pattern
- Separation of read and write models
- Independent scaling
- Performance optimization

### Database Replication
- Primary-replica streaming replication
- Write-Ahead Log (WAL)
- Eventual consistency

### Spring Data JPA
- Custom queries with @Query
- Join fetch for N+1 prevention
- Pagination and sorting

### Caching
- Redis distributed cache
- Cache-aside pattern
- Cache eviction strategies

### Transaction Management
- @Transactional(readOnly = true/false)
- Transaction-based routing
- ACID guarantees

## Next Steps

### Pending Implementation
- [ ] MapStruct for DTO mapping
- [ ] Event publishing to Kafka
- [ ] Security configuration (JWT validation)
- [ ] Comprehensive tests
- [ ] API documentation (Swagger/OpenAPI)
- [ ] Avatar upload endpoint
- [ ] Address management endpoints
- [ ] Preferences management endpoints

## Contributors

E-commerce Platform Team

## License

This is a learning project for understanding Spring Boot microservices architecture.

