# Order Service

## Overview

The Order Service manages customer orders using the **Saga pattern** for distributed transactions across multiple microservices. It coordinates with Product Service (inventory), Payment Service (payments), and other services to ensure data consistency without traditional ACID transactions.

## Architecture Highlights

### Saga Pattern (Orchestration)

```
┌──────────────────────────────────────────┐
│      Saga Orchestrator                   │
│  (Centralized Coordination)              │
└───────────┬──────────────────────────────┘
            │
    ┌───────┼───────┬──────────┬──────────┐
    ↓       ↓       ↓          ↓          ↓
┌────────┐ │ ┌──────────┐ ┌─────────┐ ┌────────┐
│ Order  │ │ │ Product  │ │ Payment │ │Notific.│
│Service │ │ │ Service  │ │ Service │ │Service │
└────────┘ │ └──────────┘ └─────────┘ └────────┘
```

**Key Benefits**:
- **Distributed Transactions**: Maintains consistency across services
- **Compensation**: Automatic rollback on failure
- **Recovery**: Resume after crash
- **Audit Trail**: Complete transaction history
- **No 2PC**: Avoids distributed locking

### Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL 15
- **Service Discovery**: Eureka Client
- **Service Communication**: OpenFeign
- **Resilience**: Resilience4j (Circuit Breaker, Retry, Timeout)
- **Distributed Lock**: Redisson
- **Caching**: Redis
- **Messaging**: Kafka
- **Migration**: Flyway
- **Testing**: JUnit 5, Testcontainers

## Saga Pattern Implementation

### Create Order Saga

```
Step 1: CreateOrder
- Create order entity (PENDING)
- Calculate totals
- Save to database

Step 2: ReserveInventory
- Call Product Service
- Reserve stock for each item
- Store reservation IDs

Step 3: ProcessPayment
- Call Payment Service
- Charge customer
- Store transaction ID

Step 4: ConfirmOrder
- Update order status (CONFIRMED)
- Order ready for fulfillment
```

### Success Flow

```
CreateOrder ✓ → ReserveInventory ✓ → ProcessPayment ✓ → ConfirmOrder ✓
Result: Order CONFIRMED, ready to ship
```

### Failure Flow (Payment Declined)

```
CreateOrder ✓ → ReserveInventory ✓ → ProcessPayment ✗

Compensation (Reverse Order):
ProcessPayment ← ReleaseInventory ✓ ← CancelOrder ✓

Result: Order CANCELLED, inventory released, no charge
```

## API Endpoints

### Order Management

#### Create Order
```http
POST /api/orders
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "items": [
    {
      "productId": "prod-123",
      "productName": "Laptop",
      "sku": "LAP-001",
      "price": 1299.99,
      "quantity": 1
    }
  ],
  "shippingFee": 15.00,
  "taxAmount": 104.00,
  "discountAmount": 50.00,
  "paymentMethod": "CREDIT_CARD",
  "shippingAddress": "123 Main St, City, State 12345",
  "billingAddress": "123 Main St, City, State 12345",
  "notes": "Please deliver after 5 PM"
}

Response 201:
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 201,
  "message": "Order created successfully",
  "data": {
    "orderId": "order-abc-123",
    "orderNumber": "ORD-20240101-0001",
    "userId": 1,
    "status": "CONFIRMED",
    "paymentStatus": "CAPTURED",
    "items": [...],
    "totalAmount": 1299.99,
    "shippingFee": 15.00,
    "taxAmount": 104.00,
    "discountAmount": 50.00,
    "finalAmount": 1368.99,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

#### Get Order
```http
GET /api/orders/{orderId}
Authorization: Bearer {jwt_token}

Response 200: Order details
```

#### Get My Orders
```http
GET /api/orders/my?page=0&size=20
Authorization: Bearer {jwt_token}

Response 200: Paginated list of user's orders
```

#### Cancel Order
```http
PUT /api/orders/{orderId}/cancel
Authorization: Bearer {jwt_token}

Response 200: Cancelled order
```

## Saga Orchestration

### Saga Steps

#### 1. CreateOrderStep
**Execute**:
- Create Order entity with PENDING status
- Calculate totals (items + shipping + tax - discount)
- Save to database

**Compensate**:
- Mark order as CANCELLED
- Update cancelled_at timestamp

#### 2. ReserveInventoryStep
**Execute**:
- For each order item:
  - Call Product Service: `POST /api/products/reserve`
  - Reserve inventory
  - Store reservation ID

**Compensate**:
- For each item:
  - Call Product Service: `POST /api/products/release`
  - Release reserved inventory

#### 3. ProcessPaymentStep
**Execute**:
- Call Payment Service: `POST /api/payments/process`
- Charge customer's payment method
- Store transaction ID
- Update payment status: CAPTURED

**Compensate**:
- Call Payment Service: `POST /api/payments/refund`
- Refund the charge
- Update payment status: REFUNDED

#### 4. ConfirmOrderStep
**Execute**:
- Update order status: CONFIRMED
- Link saga ID to order
- Publish OrderConfirmedEvent (future)

**Compensate**:
- No action (order already cancelled in Step 1)

### Saga State Machine

```
STARTED → IN_PROGRESS → COMPLETED (Success)
              ↓
       COMPENSATING → COMPENSATED (Failure)
              ↓
           FAILED (Compensation failed)
```

### Recovery Mechanism

**On Application Restart**:
1. Load incomplete sagas: `status IN ('IN_PROGRESS', 'COMPENSATING')`
2. Check last completed step
3. Resume from next step
4. Continue or compensate
5. Update final state

**Example**:
```
Saga crashed at Step 3/4
Database: currentStep = 2 (Steps 1, 2 completed)
Recovery: Execute Step 3, then Step 4
```

## Resilience Patterns

### Circuit Breaker

```yaml
Product Service:
- Sliding Window: 10 requests
- Failure Threshold: 50%
- Wait Duration: 10 seconds
- Half-Open: 3 test requests

States:
CLOSED → OPEN (50% failures) → HALF_OPEN (after 10s) → CLOSED (if successful)
```

### Retry Strategy

```yaml
Max Attempts: 3
Initial Backoff: 1 second
Multiplier: 2 (exponential)
Backoff Sequence: 1s, 2s, 4s
```

### Timeout

```yaml
Product Service: 10 seconds
Payment Service: 15 seconds
```

## Database Schema

### Orders Table
```sql
orders (
  order_id VARCHAR(36) PK,
  user_id BIGINT,
  order_number VARCHAR(50) UNIQUE,
  status VARCHAR(20),
  payment_status VARCHAR(20),
  total_amount DECIMAL(10,2),
  shipping_fee DECIMAL(10,2),
  tax_amount DECIMAL(10,2),
  discount_amount DECIMAL(10,2),
  final_amount DECIMAL(10,2),
  payment_method VARCHAR(50),
  payment_transaction_id VARCHAR(100),
  shipping_address TEXT,
  billing_address TEXT,
  notes TEXT,
  tracking_number VARCHAR(100),
  carrier VARCHAR(50),
  saga_id VARCHAR(36),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  cancelled_at TIMESTAMP,
  shipped_at TIMESTAMP,
  delivered_at TIMESTAMP
)
```

### Order Items Table
```sql
order_items (
  id BIGSERIAL PK,
  order_id VARCHAR(36) FK,
  product_id VARCHAR(36),
  product_name VARCHAR(200),
  sku VARCHAR(50),
  price DECIMAL(10,2),
  quantity INTEGER,
  subtotal DECIMAL(10,2),
  product_image_url VARCHAR(500)
)
```

### Saga Executions Table
```sql
saga_executions (
  saga_id VARCHAR(36) PK,
  saga_type VARCHAR(100),
  status VARCHAR(20),
  current_step INTEGER,
  total_steps INTEGER,
  order_id VARCHAR(36) FK,
  payload TEXT,
  error_message TEXT,
  retry_count INTEGER,
  last_error_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  completed_at TIMESTAMP
)
```

## Configuration

### application.yml

```yaml
server:
  port: 8084

spring:
  application:
    name: order-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_order_db
    username: postgres
    password: postgres
  
  kafka:
    bootstrap-servers: localhost:9092

feign:
  circuitbreaker:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      productService:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      paymentService:
        failureRateThreshold: 50
        waitDurationInOpenState: 10s

app:
  saga:
    retry:
      max-attempts: 5
      initial-backoff: 1000
      multiplier: 2
```

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 15
- Redis
- Kafka
- Eureka Server running
- Product Service running
- Payment Service running

### Local Development

1. **Start PostgreSQL**:
```bash
docker run -d \
  --name postgres-order \
  -p 5432:5432 \
  -e POSTGRES_DB=ecommerce_order_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine
```

2. **Start Redis**:
```bash
docker run -d \
  --name redis-order \
  -p 6379:6379 \
  redis:alpine
```

3. **Run Service**:
```bash
./gradlew :services:order-service:bootRun
```

4. **Verify**:
```bash
curl http://localhost:8084/actuator/health
```

## Testing

### Unit Tests
```bash
./gradlew :services:order-service:test --tests "*Test"
```

### Integration Tests
```bash
./gradlew :services:order-service:test --tests "*IntegrationTest"
```

## Monitoring

### Saga Execution Queries

**Active Sagas**:
```sql
SELECT * FROM saga_executions 
WHERE status IN ('IN_PROGRESS', 'COMPENSATING');
```

**Failed Sagas**:
```sql
SELECT * FROM saga_executions 
WHERE status = 'FAILED';
```

**Success Rate**:
```sql
SELECT 
  saga_type,
  COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed,
  COUNT(CASE WHEN status = 'COMPENSATED' THEN 1 END) as compensated,
  COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed
FROM saga_executions
GROUP BY saga_type;
```

## Learning Concepts

### Saga Pattern
- **Orchestration** vs Choreography
- **Compensation** logic
- **Idempotency** for retries
- **State persistence** for recovery

### Distributed Transactions
- Why 2PC doesn't scale
- Eventual consistency
- Compensation strategies
- Failure handling

### Resilience Patterns
- **Circuit Breaker**: Prevent cascading failures
- **Retry**: Handle transient errors
- **Timeout**: Fail fast
- **Bulkhead**: Isolate resources

### Service Communication
- **Feign Clients**: Declarative REST clients
- **Service Discovery**: Dynamic service lookup
- **Load Balancing**: Distribute requests
- **Fallback**: Graceful degradation

## Next Steps

### Pending Features
- [ ] Saga recovery scheduler
- [ ] Kafka event publishing
- [ ] Order tracking updates
- [ ] Shipping integration
- [ ] Order analytics
- [ ] Comprehensive tests

## Contributors

E-commerce Platform Team

## License

This is a learning project for understanding Saga pattern and distributed transactions in microservices.

