# Product Service

## Overview

The Product Service is the catalog management system implementing **Event Sourcing** pattern with **MongoDB** for scalable, auditable product data management.

## Architecture Highlights

### Event Sourcing Pattern

```
┌──────────────────────────────────────────┐
│         Product Service                  │
├─────────────────┬────────────────────────┤
│  Command Side   │      Query Side        │
│  (Write Model)  │    (Read Model)        │
└────────┬────────┴────────┬───────────────┘
         │                  │
   ┌─────┴─────┐      ┌────┴────┐
   │   Event   │      │ Product │
   │   Store   │      │  View   │
   └───────────┘      └─────────┘
      MongoDB           MongoDB
   (Immutable Log)   (Optimized)
```

**Key Benefits**:
- **Complete Audit Trail**: Every state change recorded as event
- **Time Travel**: Reconstruct state at any point in time
- **Event Replay**: Rebuild projections from scratch
- **Debugging**: See exact sequence of operations
- **Analytics**: Business intelligence from event stream

### Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: MongoDB (Event Store + Projections)
- **Caching**: Redis for read optimization
- **Messaging**: Kafka for event publishing
- **Security**: OAuth2 Resource Server (JWT validation)
- **Service Discovery**: Eureka Client
- **Testing**: JUnit 5, Embedded MongoDB

## Event Sourcing Implementation

### Core Concepts

#### 1. Events

Events are immutable facts representing state changes:

- **ProductCreatedEvent**: Product added to catalog
- **ProductUpdatedEvent**: Product details modified
- **PriceChangedEvent**: Price adjustment with reason
- **StockChangedEvent**: Inventory update
- **ProductDeletedEvent**: Product removal (soft delete)

#### 2. Event Store

MongoDB collection storing ALL events chronologically:

```javascript
// Collection: product_events
{
  "_id": "evt-abc123",
  "aggregateId": "prod-laptop-001",
  "eventType": "ProductCreatedEvent",
  "version": 1,
  "timestamp": ISODate("2024-01-01T10:00:00Z"),
  "userId": "admin-001",
  "name": "MacBook Pro 16",
  "price": NumberDecimal("2499.99"),
  "categoryId": "cat-electronics",
  "sku": "MBP16-2024",
  "stock": 50,
  ...
}
```

**Characteristics**:
- Append-only (never update/delete)
- Indexed by (aggregateId, version) - unique
- Ordered by timestamp
- Immutable audit log

#### 3. Aggregate (Product)

Domain object that:
- Validates business rules
- Applies events to build state
- Emits new events for changes

#### 4. Projection (ProductView)

Current state optimized for queries:

```javascript
// Collection: products
{
  "_id": "prod-laptop-001",
  "name": "MacBook Pro 16",
  "description": "High-performance laptop",
  "price": NumberDecimal("2499.99"),
  "categoryId": "cat-electronics",
  "sku": "MBP16-2024",
  "stock": 50,
  "active": true,
  "featured": false,
  "version": 5,  // Current event version
  "createdAt": ISODate("2024-01-01T10:00:00Z"),
  "updatedAt": ISODate("2024-01-15T14:30:00Z")
}
```

### Event Flow

#### Write Operation (Command)

```
1. User: "Create Product"
   ↓
2. Validate: SKU unique, price positive, etc.
   ↓
3. Create Event: ProductCreatedEvent
   ↓
4. Save to Event Store (MongoDB)
   ↓
5. Apply Event: Build Product aggregate
   ↓
6. Save Projection: Current state
   ↓
7. Publish to Kafka: Notify other services
   ↓
8. Return: Product created
```

#### Read Operation (Query)

```
1. User: "Get Product"
   ↓
2. Check Redis Cache
   ↓
3. If MISS: Query projection (products collection)
   ↓
4. Store in cache (TTL: 1 hour)
   ↓
5. Return: Product details
```

#### Event Replay (Rebuild)

```
1. Load Events: All events for product ID
   ↓
2. Create Empty Aggregate
   ↓
3. Apply Events in Order:
   - ProductCreatedEvent → Initialize
   - PriceChangedEvent → Update price
   - StockChangedEvent → Update stock
   - ...
   ↓
4. Result: Current state reconstructed
```

## API Endpoints

### Product Management

#### Create Product
```http
POST /api/products
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "MacBook Pro 16",
  "description": "High-performance laptop for professionals",
  "price": 2499.99,
  "categoryId": "cat-electronics",
  "sku": "MBP16-2024",
  "brand": "Apple",
  "stock": 50,
  "weight": 2.0,
  "dimensions": "35.79 x 24.81 x 1.62",
  "images": [
    "https://cdn.example.com/products/mbp16-1.jpg"
  ],
  "attributes": {
    "color": "Space Gray",
    "memory": "32GB",
    "storage": "1TB SSD"
  },
  "tags": ["laptop", "apple", "professional"],
  "active": true,
  "featured": false
}

Response 201:
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 201,
  "message": "Product created successfully",
  "data": {
    "productId": "prod-laptop-001",
    "name": "MacBook Pro 16",
    ...
    "version": 1
  }
}
```

#### Get Product
```http
GET /api/products/{id}

Response 200: Product details
```

#### Update Product
```http
PUT /api/products/{id}
Authorization: Bearer {jwt_token}

{
  "name": "Updated Name",
  "description": "Updated description",
  ...
}

Response 200: Updated product
```

#### Change Price
```http
PATCH /api/products/{id}/price?price=1999.99&reason=Holiday%20Sale
Authorization: Bearer {jwt_token}

Response 200: Updated product with new price
```

#### Change Stock
```http
PATCH /api/products/{id}/stock?stock=100&reason=Restock
Authorization: Bearer {jwt_token}

Response 200: Updated product with new stock
```

#### Delete Product
```http
DELETE /api/products/{id}?reason=Discontinued
Authorization: Bearer {jwt_token}

Response 200: Product deleted (soft delete - marked inactive)
```

### Query Endpoints

#### Get Products by Category
```http
GET /api/products/category/{categoryId}?page=0&size=20

Response 200: Paginated products
```

#### Get Featured Products
```http
GET /api/products/featured?page=0&size=20

Response 200: Paginated featured products
```

#### Search Products
```http
GET /api/products/search?q=laptop&page=0&size=20

Response 200: Search results
```

### Event Sourcing Endpoints

#### Get Product Event History
```http
GET /api/products/{id}/history

Response 200:
{
  "data": [
    {
      "eventId": "evt-001",
      "eventType": "ProductCreatedEvent",
      "version": 1,
      "timestamp": "2024-01-01T10:00:00Z",
      "userId": "admin-001",
      "name": "MacBook Pro 16",
      "price": 2499.99,
      ...
    },
    {
      "eventId": "evt-002",
      "eventType": "PriceChangedEvent",
      "version": 2,
      "timestamp": "2024-01-05T14:30:00Z",
      "userId": "admin-001",
      "oldPrice": 2499.99,
      "newPrice": 1999.99,
      "reason": "Holiday Sale"
    },
    ...
  ]
}
```

#### Rebuild Product from Events
```http
POST /api/products/{id}/rebuild

Response 200: Reconstructed product from events
```

## MongoDB Configuration

### Collections

#### 1. product_events (Event Store)

```javascript
// Indexes
{ aggregateId: 1, version: 1 }  // Unique - ensures version monotonicity
{ aggregateId: 1, timestamp: -1 }  // Chronological queries
{ eventType: 1, timestamp: -1 }  // Analytics by event type
```

#### 2. products (Projection)

```javascript
// Indexes
{ _id: 1 }  // Primary key
{ categoryId: 1, createdAt: -1 }  // Category listings
{ sku: 1 }  // Unique - SKU lookup
{ name: "text", description: "text" }  // Full-text search
{ price: 1 }  // Price range queries
{ stock: 1 }  // Inventory queries
{ active: 1, featured: 1 }  // Featured products
```

#### 3. categories (Hierarchy)

```javascript
// Indexes
{ _id: 1 }
{ slug: 1 }  // Unique - SEO-friendly URLs
{ parentId: 1 }  // Hierarchical queries
```

### Sharding Strategy (Production)

**Shard Key**: `{ categoryId: 1, _id: 1 }`

**Benefits**:
- Good data distribution across shards
- Category-based queries hit single shard
- Prevents hot spots
- Scalable to millions of products

**Setup**:
```javascript
sh.enableSharding("ecommerce_product_db")
sh.shardCollection(
  "ecommerce_product_db.products",
  { categoryId: 1, _id: 1 }
)
```

## Running the Service

### Prerequisites
- Java 21
- MongoDB 7.0+
- Redis
- Kafka (optional, for events)

### Local Development

1. **Start MongoDB**:
```bash
docker run -d \
  --name mongodb-product \
  -p 27017:27017 \
  -e MONGO_INITDB_DATABASE=ecommerce_product_db \
  mongo:7.0
```

2. **Start Redis**:
```bash
docker run -d \
  --name redis-product \
  -p 6379:6379 \
  redis:alpine
```

3. **Run Service**:
```bash
./gradlew :services:product-service:bootRun
```

4. **Verify**:
```bash
curl http://localhost:8083/actuator/health
```

## Configuration

### application.yml

```yaml
server:
  port: 8083

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/ecommerce_product_db
      auto-index-creation: true
  
  redis:
    host: localhost
    port: 6379
  
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour

app:
  event-sourcing:
    enabled: true
    snapshot-frequency: 100  # Create snapshot every 100 events
```

## Caching Strategy

### Redis Cache Configuration

```
products:
- Key: products::{productId}
- TTL: 1 hour
- Eviction: On product update

categoryProducts:
- Key: categoryProducts::{categoryId}_{page}
- TTL: 30 minutes
- Eviction: On category update

productSearch:
- Key: productSearch::{query}_{page}
- TTL: 15 minutes
- Eviction: On product updates
```

## Event Publishing

### Kafka Topics

- **product-events**: All product-related events
  - ProductCreatedEvent
  - ProductUpdatedEvent
  - PriceChangedEvent
  - StockChangedEvent
  - ProductDeletedEvent

### Consumers

- **Order Service**: Update product info in orders
- **Search Service**: Index in Elasticsearch
- **Notification Service**: Alert admins of low stock
- **Analytics Service**: Track catalog changes

## Testing

### Unit Tests
```bash
./gradlew :services:product-service:test --tests "*Test"
```

### Integration Tests
```bash
./gradlew :services:product-service:test --tests "*IntegrationTest"
```

### Event Replay Tests
```bash
# Test event replay functionality
./gradlew :services:product-service:test --tests "*EventReplayTest"
```

## Performance Metrics

### Expected Performance
- **Cache Hit Rate**: 95%+
- **Read Latency**: < 1ms (cache hit), < 30ms (cache miss)
- **Write Latency**: < 150ms (includes event save + projection update)
- **Throughput**: 5,000+ req/sec (reads), 500+ req/sec (writes)

### MongoDB Performance
- **Event Store Writes**: < 50ms
- **Projection Reads**: < 20ms
- **Event Replay (100 events)**: < 500ms

## Learning Concepts

### Event Sourcing
- Append-only event log
- Event replay for state reconstruction
- Complete audit trail
- Time-travel queries

### CQRS
- Separate read and write models
- Optimized for different access patterns
- Independent scaling

### MongoDB
- Document-oriented storage
- Sharding for horizontal scaling
- Text search capabilities
- Flexible schema

### Domain-Driven Design
- Aggregates and aggregate roots
- Domain events
- Business logic encapsulation

## Next Steps

### Pending Implementation
- [ ] Kafka event publishing
- [ ] Snapshot optimization
- [ ] Category management APIs
- [ ] Product variants support
- [ ] Image upload endpoint
- [ ] Elasticsearch integration for advanced search
- [ ] Comprehensive tests
- [ ] API documentation (Swagger/OpenAPI)

## Contributors

E-commerce Platform Team

## License

This is a learning project for understanding Event Sourcing and CQRS patterns in microservices.

