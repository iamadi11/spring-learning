# Payment Service

## Overview

The Payment Service handles all payment processing for the e-commerce platform. It demonstrates **comprehensive resilience patterns** including Circuit Breaker, Retry, Rate Limiting, Bulkhead, and more.

## 🎯 Core Concepts Demonstrated

### 1. Circuit Breaker Pattern ⚡

**What**: Prevents cascading failures by stopping calls to failing services.

**Why**: 
- Payment gateways can become slow or unavailable
- Without circuit breaker: threads block waiting for timeout
- With circuit breaker: fail fast, preserve resources

**States**:
```
CLOSED → OPEN → HALF_OPEN → CLOSED
  ↑                             |
  └─────────────────────────────┘
```

- **CLOSED**: Normal operation, requests pass through
- **OPEN**: Too many failures, reject immediately
- **HALF_OPEN**: Testing if service recovered

**Configuration** (`application.yml`):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 10              # Last 10 requests
        failureRateThreshold: 50           # Open if 50% fail
        waitDurationInOpenState: 30s       # Wait before retry
        permittedNumberOfCallsInHalfOpenState: 3
```

**Code Example**:
```java
@CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
public CompletableFuture<Payment> processPayment(PaymentRequest request) {
    // Call payment gateway
}

// Fallback when circuit is OPEN
public CompletableFuture<Payment> processPaymentFallback(PaymentRequest request, Exception e) {
    // Return cached payment or queue for later processing
}
```

**Testing Circuit Breaker**:
1. Make 10 payment requests
2. Simulate 6 failures (60% failure rate > 50% threshold)
3. Circuit opens
4. Next requests fail immediately (< 1ms vs 15s timeout)
5. After 30 seconds, circuit → HALF_OPEN
6. Test with 3 requests
7. If successful → CLOSED, if failed → OPEN

### 2. Retry Pattern 🔄

**What**: Automatically retry failed operations.

**Why**:
- Network issues are often transient
- Retry can succeed where first attempt failed
- Better user experience (no visible error)

**Smart Retry**:
- Retry transient failures: `IOException`, `TimeoutException`
- Don't retry permanent failures: `PaymentDeclinedException`

**Exponential Backoff**:
```
Attempt 1: Fail → Wait 1s
Attempt 2: Fail → Wait 2s
Attempt 3: Fail → Wait 4s
Attempt 4: Give up
```

**Configuration**:
```yaml
resilience4j:
  retry:
    instances:
      paymentGateway:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
        ignoreExceptions:
          - com.ecommerce.payment.exception.PaymentDeclinedException
```

**Code Example**:
```java
@Retry(name = "paymentGateway")
public CompletableFuture<Payment> processPayment(PaymentRequest request) {
    // Will automatically retry on IOException
    // Won't retry on PaymentDeclinedException
}
```

### 3. Rate Limiting 🚦

**What**: Limit number of requests in a time window.

**Why**:
- Payment gateways have rate limits (Stripe: 100 req/s)
- Prevent cost overrun
- Fraud prevention (limit failed attempts)

**Token Bucket Algorithm**:
```
Bucket Capacity: 100 tokens
Refill Rate: 10 tokens/second

Request arrives → Consume 1 token
No tokens available → Wait or reject (429 Too Many Requests)
```

**Configuration**:
```yaml
resilience4j:
  ratelimiter:
    instances:
      paymentGateway:
        limitForPeriod: 10        # 10 requests
        limitRefreshPeriod: 1s    # per second
        timeoutDuration: 5s       # wait up to 5s for token
      
      userPaymentAttempts:
        limitForPeriod: 5         # 5 attempts
        limitRefreshPeriod: 1m    # per minute (fraud prevention)
        timeoutDuration: 0s       # fail immediately
```

**Code Example**:
```java
@RateLimiter(name = "paymentGateway")
public CompletableFuture<Payment> processPayment(PaymentRequest request) {
    // Max 10 requests/second
    // Returns 429 if exceeded
}
```

### 4. Bulkhead Pattern 🚢

**What**: Isolate resources to prevent complete system failure.

**Analogy**: Ship compartments - one floods, others stay dry.

**Why**:
- Payment processing slow → threads blocked
- Without bulkhead: all threads blocked, entire service down
- With bulkhead: payment threads blocked, refunds still work

**Thread Pool Isolation**:
```
Total: 100 threads
├── Payment Pool: 80 threads
├── Refund Pool: 20 threads
└── Admin Pool: 5 threads

Payment gateway slow → 80 threads blocked
Refunds still work with 20 threads!
```

**Configuration**:
```yaml
resilience4j:
  bulkhead:
    instances:
      paymentGateway:
        maxConcurrentCalls: 50    # Max 50 concurrent payment calls
        maxWaitDuration: 10s
  
  thread-pool-bulkhead:
    instances:
      paymentProcessing:
        maxThreadPoolSize: 80
        coreThreadPoolSize: 40
```

**Code Example**:
```java
@Bulkhead(name = "paymentGateway", type = Bulkhead.Type.SEMAPHORE)
public CompletableFuture<Payment> processPayment(PaymentRequest request) {
    // Limited to 50 concurrent calls
}
```

### 5. Time Limiter ⏱️

**What**: Enforce timeout on operations.

**Why**:
- Payment gateway can be very slow
- Don't wait forever (thread exhaustion)
- Fail fast, free up resources

**Configuration**:
```yaml
resilience4j:
  timelimiter:
    instances:
      paymentGateway:
        timeoutDuration: 15s        # Fail if > 15 seconds
        cancelRunningFuture: true
```

**Code Example**:
```java
@TimeLimiter(name = "paymentGateway")
public CompletableFuture<Payment> processPayment(PaymentRequest request) {
    // Will timeout after 15 seconds
}
```

### 6. Idempotency 🔑

**What**: Same request = same result (no duplicate charges).

**Critical for Payments**:
```
Scenario without idempotency:
1. User clicks "Pay"
2. Payment processes successfully
3. Network timeout (no response received)
4. User clicks "Pay" again
5. Charged twice! 💸💸

Scenario with idempotency:
1. User clicks "Pay" (orderId: 123)
2. Payment processes successfully
3. Network timeout
4. User clicks "Pay" again (same orderId: 123)
5. Check: orderId 123 already processed
6. Return existing payment (no duplicate charge) ✅
```

**Implementation**:
```java
// STEP 1: Check if order already paid
Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
if (existingPayment.isPresent() && existingPayment.get().isSuccessful()) {
    throw new DuplicatePaymentException("Payment already processed");
}

// STEP 2: Process payment
// ...

// orderId is UNIQUE in database - prevents duplicates
```

**Database**:
```sql
CREATE TABLE payments (
    transaction_id VARCHAR(100) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,  -- Idempotency key
    ...
);
```

## 🏗️ Architecture

### Resilience Pattern Stack

```
HTTP Request
    ↓
[Rate Limiter] ← Limit requests to 10/second
    ↓
[Circuit Breaker] ← Fail fast if gateway down
    ↓
[Time Limiter] ← Enforce 15s timeout
    ↓
[Bulkhead] ← Isolate thread pool
    ↓
[Retry] ← Retry transient failures
    ↓
[Service Method] ← Business logic
    ↓
[Payment Gateway] ← External API call
```

### Database Schema

```sql
payments
├── transaction_id (PK)        -- Gateway transaction ID
├── order_id (UNIQUE)          -- Idempotency key
├── user_id                    -- User reference
├── amount, currency           -- Payment details
├── payment_method             -- Card, PayPal, etc.
├── status                     -- PENDING, CAPTURED, FAILED, etc.
├── gateway                    -- STRIPE, PAYPAL
├── gateway_transaction_id     -- For reconciliation
├── gateway_response           -- Full response (JSON)
├── gateway_response_code      -- Success/error code
├── failure_reason             -- Why it failed
├── retry_count               -- How many retries
├── refund_amount             -- Partial/full refund
├── metadata                  -- IP, user agent (fraud detection)
└── created_at, updated_at    -- Audit trail
```

## 📊 Payment Lifecycle

```
User Action: Click "Pay"
    ↓
PENDING ← Create payment record
    ↓
[Fraud Check]
    ↓
[Call Payment Gateway]
    ↓
    ├─ Success → AUTHORIZED → CAPTURED → SETTLED
    │
    ├─ Transient Failure (network) → RETRY
    │      ↓
    │   [Retry 1] → Wait 1s
    │      ↓
    │   [Retry 2] → Success → CAPTURED
    │
    └─ Permanent Failure (card declined) → DECLINED
           ↓
       Save failure reason
           ↓
       Don't retry
```

## 🚀 API Endpoints

### 1. Process Payment

**POST** `/api/payments/process`

**Request**:
```json
{
  "orderId": "order-123",
  "userId": 1,
  "amount": 99.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "cardNumber": "4242424242424242",
  "cardHolderName": "John Doe",
  "expiryMonth": 12,
  "expiryYear": 2025,
  "cvv": "123",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "deviceId": "device-123"
}
```

**Response** (Success):
```json
{
  "transactionId": "stripe_abc123",
  "orderId": "order-123",
  "status": "CAPTURED",
  "amount": 99.99,
  "gateway": "STRIPE",
  "createdAt": "2024-01-01T12:00:00"
}
```

**Response** (Declined):
```json
{
  "transactionId": "stripe_xyz789",
  "orderId": "order-123",
  "status": "DECLINED",
  "failureReason": "insufficient_funds",
  "gatewayResponseCode": "DECLINED"
}
```

### 2. Get Payment by Order ID

**GET** `/api/payments/order/{orderId}`

### 3. Refund Payment

**POST** `/api/payments/refund`

**Request**:
```json
{
  "transactionId": "stripe_abc123",
  "amount": 99.99,
  "reason": "Customer requested refund"
}
```

## 🔧 Configuration

### application.yml

All resilience patterns configured in `application.yml`:

- **Circuit Breaker**: Failure thresholds, wait durations
- **Retry**: Max attempts, backoff strategy
- **Rate Limiter**: Request limits per time window
- **Bulkhead**: Thread pool sizes
- **Time Limiter**: Timeout durations

### Payment Gateway

Mock Stripe gateway (`StripeGatewaySimulator`) simulates:
- 80% success rate
- 10% transient failures (network timeout) → should retry
- 10% permanent failures (card declined) → don't retry
- Random latency (100-500ms normal, 2-10s slow)

## 🧪 Testing Resilience Patterns

### Test Circuit Breaker

```bash
# Make 10 requests, simulate high failure rate
for i in {1..10}; do
  curl -X POST http://localhost:8085/api/payments/process \
    -H "Content-Type: application/json" \
    -d '{"orderId":"test-'$i'", ...}'
done

# Check circuit breaker status
curl http://localhost:8085/actuator/circuitbreakers

# Circuit should be OPEN
# Next requests fail fast (< 1ms)
```

### Test Retry

```bash
# Gateway will simulate transient failure on first attempt
# Should automatically retry and succeed

curl -X POST http://localhost:8085/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{"orderId":"test-retry", ...}'

# Check logs:
# - Attempt 1: IOException (network timeout)
# - Wait 1 second
# - Attempt 2: Success
```

### Test Rate Limiting

```bash
# Make 20 requests rapidly (limit is 10/second)
for i in {1..20}; do
  curl -X POST http://localhost:8085/api/payments/process &
done

# First 10: Success (200)
# Next 10: Too Many Requests (429)
```

### Test Idempotency

```bash
# Process payment
curl -X POST http://localhost:8085/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-123", "amount": 99.99, ...}'

# Response: 201 Created, transaction: stripe_abc123

# Retry same order (simulate network timeout + retry)
curl -X POST http://localhost:8085/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{"orderId":"order-123", "amount": 99.99, ...}'

# Response: 400 Duplicate Payment
# No duplicate charge!
```

## 📈 Monitoring

### Actuator Endpoints

```bash
# Health check
GET /actuator/health

# Circuit breaker status
GET /actuator/circuitbreakers
GET /actuator/circuitbreakerevents

# Rate limiters
GET /actuator/ratelimiters

# Bulkheads
GET /actuator/bulkheads

# Metrics (Prometheus format)
GET /actuator/prometheus
```

### Key Metrics

- `resilience4j.circuitbreaker.calls{name="paymentGateway"}`
- `resilience4j.circuitbreaker.state{name="paymentGateway"}`
- `resilience4j.retry.calls{name="paymentGateway"}`
- `resilience4j.ratelimiter.available.permissions{name="paymentGateway"}`
- `resilience4j.bulkhead.available.concurrent.calls{name="paymentGateway"}`

## 🎓 Learning Points

### 1. Why ALL These Patterns?

Each pattern solves a specific problem:

| Pattern | Problem | Solution |
|---------|---------|----------|
| Circuit Breaker | Gateway down, threads blocking | Fail fast, preserve resources |
| Retry | Transient failures | Automatic retry with backoff |
| Rate Limiter | Too many requests | Throttle to gateway limits |
| Bulkhead | Resource exhaustion | Isolate thread pools |
| Time Limiter | Slow responses | Enforce timeout |
| Idempotency | Duplicate charges | Safe retries |

### 2. Real-World Scenarios

**Scenario 1: Payment Gateway Outage**
```
Without Resilience:
- Gateway down
- All requests wait 30s for timeout
- 1000 requests = 1000 threads blocked
- Service down!

With Resilience:
- Circuit breaker opens after 5 failures
- Next 995 requests fail fast (< 1ms)
- Bulkhead isolates payment threads
- Refunds still work
- Service stable!
```

**Scenario 2: Transient Network Issues**
```
Without Retry:
- Network timeout
- Payment fails
- User sees error
- Bad experience

With Retry:
- Network timeout on attempt 1
- Wait 1 second
- Retry succeeds
- User sees success
- Good experience!
```

**Scenario 3: Accidental Double-Click**
```
Without Idempotency:
- User clicks "Pay" twice
- Both process
- Charged twice
- Customer angry!

With Idempotency:
- Both requests have same orderId
- First processes
- Second checks: already paid
- Return existing payment
- Single charge!
```

### 3. Production Considerations

**Security**:
- Never store raw card numbers
- Use payment gateway tokenization
- PCI DSS compliance
- Encrypt sensitive data

**Monitoring**:
- Track circuit breaker state changes
- Alert on high failure rates
- Monitor retry counts
- Track rate limit rejections

**Testing**:
- Unit tests for each pattern
- Integration tests with Testcontainers
- Load tests to verify resilience
- Chaos engineering (simulate failures)

## 🏃 Running the Service

### Prerequisites

- PostgreSQL running on port 5432
- Redis running on port 6379
- Eureka Server running on port 8761

### Build & Run

```bash
# Build
./gradlew :services:payment-service:build

# Run
./gradlew :services:payment-service:bootRun

# Or with Docker
docker-compose up payment-service
```

### Verify

```bash
# Health check
curl http://localhost:8085/api/payments/health

# Actuator
curl http://localhost:8085/actuator/health
```

## 📚 Further Reading

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Stripe API Best Practices](https://stripe.com/docs/api)
- [PCI DSS Compliance](https://www.pcisecuritystandards.org/)

---

**Next Service**: Notification Service (Phase 7) - Multithreading & WebSocket

