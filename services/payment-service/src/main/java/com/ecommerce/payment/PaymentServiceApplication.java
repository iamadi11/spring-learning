package com.ecommerce.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Payment Service Application
 * 
 * <p>Payment processing service demonstrating advanced resilience patterns
 * including Circuit Breaker, Bulkhead, Rate Limiting, and Retry strategies.</p>
 * 
 * <h2>Service Responsibilities:</h2>
 * <ul>
 *   <li>Payment Processing (Authorization, Capture)</li>
 *   <li>Refund Management</li>
 *   <li>Payment Gateway Integration</li>
 *   <li>Transaction Management</li>
 *   <li>Payment Method Management</li>
 *   <li>Fraud Detection Integration</li>
 * </ul>
 * 
 * <h2>Circuit Breaker Pattern:</h2>
 * <pre>
 * What is a Circuit Breaker?
 * - Prevents cascading failures in distributed systems
 * - Stops calling failing services temporarily
 * - Allows system to recover
 * - Fails fast instead of waiting for timeout
 * 
 * Electrical Analogy:
 * - Like circuit breaker in your home
 * - Too much current → breaker trips
 * - Stops electricity flow
 * - Prevents fire/damage
 * - Reset after problem fixed
 * 
 * Software Circuit Breaker:
 * ┌─────────────────────────────────────────┐
 * │         Circuit Breaker States          │
 * └─────────────────────────────────────────┘
 * 
 * CLOSED (Normal Operation):
 * ┌──────────┐
 * │  Request │──→ Payment Gateway ──→ Success
 * └──────────┘
 * - All requests pass through
 * - Track failures
 * - If failure rate > threshold → OPEN
 * 
 * OPEN (Service Down):
 * ┌──────────┐
 * │  Request │──X  Fail Fast (No call to gateway)
 * └──────────┘
 * - Reject requests immediately
 * - Don't wait for timeout
 * - Wait for recovery period
 * - After timeout → HALF_OPEN
 * 
 * HALF_OPEN (Testing Recovery):
 * ┌──────────┐
 * │  Request │──?→ Payment Gateway (Limited requests)
 * └──────────┘
 * - Allow limited requests
 * - Test if service recovered
 * - If successful → CLOSED
 * - If failed → OPEN
 * 
 * Example Flow:
 * 1. 10 requests to payment gateway
 * 2. 6 fail (60% failure rate > 50% threshold)
 * 3. Circuit OPENS
 * 4. Next 100 requests fail fast (no gateway calls)
 * 5. Wait 30 seconds
 * 6. Circuit → HALF_OPEN
 * 7. Try 3 requests
 * 8. All succeed → Circuit CLOSES
 * 9. Back to normal operation
 * </pre>
 * 
 * <h2>Why Circuit Breaker is Critical for Payments:</h2>
 * <pre>
 * Problem Without Circuit Breaker:
 * 
 * Payment Gateway Down:
 * 1. Order Service calls Payment Service
 * 2. Payment Service calls gateway (30s timeout)
 * 3. Gateway not responding
 * 4. Wait 30 seconds... timeout!
 * 5. Retry 3 times → 90 seconds total
 * 6. 1000 concurrent orders → 1000 threads blocked
 * 7. Thread pool exhausted
 * 8. Order Service cannot process ANY orders
 * 9. System DOWN!
 * 
 * With Circuit Breaker:
 * 1. First 5 requests fail → Circuit OPENS
 * 2. Next 995 requests fail fast (< 1ms)
 * 3. Threads not blocked
 * 4. System remains responsive
 * 5. Users get immediate error
 * 6. Can retry when gateway recovers
 * 7. System STABLE!
 * </pre>
 * 
 * <h2>Bulkhead Pattern:</h2>
 * <pre>
 * What is Bulkhead?
 * - Isolate resources for different operations
 * - Like bulkheads in a ship (compartments)
 * - One compartment floods → others safe
 * - Ship doesn't sink
 * 
 * Software Bulkhead:
 * ┌─────────────────────────────────────────┐
 * │     Thread Pool (100 threads)           │
 * ├─────────────────┬───────────────────────┤
 * │  Payment Pool   │  Refund Pool          │
 * │  (80 threads)   │  (20 threads)         │
 * └─────────────────┴───────────────────────┘
 * 
 * Without Bulkhead:
 * - Payment gateway slow → all 100 threads blocked
 * - Refunds cannot process
 * - Entire service down
 * 
 * With Bulkhead:
 * - Payment gateway slow → 80 threads blocked
 * - Refund still has 20 threads
 * - Refunds continue working
 * - Service partially available
 * 
 * Configuration:
 * - Payment processing: 80% of resources
 * - Refunds: 15% of resources
 * - Admin operations: 5% of resources
 * </pre>
 * 
 * <h2>Rate Limiting:</h2>
 * <pre>
 * Why Rate Limit Payments?
 * 1. Cost Control:
 *    - Payment gateways charge per transaction
 *    - Prevent abuse/DOS
 *    - Control costs
 * 
 * 2. Gateway Limits:
 *    - Stripe: 100 requests/second
 *    - PayPal: 50 requests/second
 *    - Exceed = banned/throttled
 * 
 * 3. Fraud Prevention:
 *    - Limit failed payment attempts
 *    - 5 failed attempts → block user
 *    - Prevent credit card testing
 * 
 * Rate Limiting Strategies:
 * 
 * 1. Token Bucket:
 *    - Bucket holds tokens
 *    - Request consumes token
 *    - Refill at fixed rate
 *    - Allows bursts
 * 
 * 2. Leaky Bucket:
 *    - Requests queue
 *    - Process at fixed rate
 *    - Smooths traffic
 * 
 * 3. Sliding Window:
 *    - Count requests in time window
 *    - Precise but expensive
 * 
 * Example (Token Bucket):
 * - Capacity: 100 tokens
 * - Refill: 10 tokens/second
 * - Burst: Can use all 100 immediately
 * - Sustained: 10 requests/second
 * 
 * Implementation:
 * @RateLimiter(name = "paymentGateway")
 * public PaymentResponse processPayment(...) {
 *     // Max 10 requests/second
 *     // Returns 429 Too Many Requests if exceeded
 * }
 * </pre>
 * 
 * <h2>Retry Strategy:</h2>
 * <pre>
 * When to Retry?
 * 
 * Transient Failures (Retry):
 * - Network timeout
 * - 503 Service Unavailable
 * - Connection reset
 * - Temporary gateway issues
 * 
 * Permanent Failures (Don't Retry):
 * - 400 Bad Request (invalid card)
 * - 401 Unauthorized (bad API key)
 * - 402 Payment Declined
 * - Insufficient funds
 * 
 * Retry Configuration:
 * - Max Attempts: 3
 * - Backoff: Exponential (1s, 2s, 4s)
 * - Retry On: IOException, TimeoutException
 * - Don't Retry On: PaymentDeclinedException
 * 
 * Example:
 * Attempt 1: Network timeout → Wait 1s
 * Attempt 2: Network timeout → Wait 2s
 * Attempt 3: Success!
 * Total time: 3 seconds
 * 
 * Without Retry:
 * Attempt 1: Network timeout → Fail
 * User experience: Payment failed (bad!)
 * 
 * With Smart Retry:
 * Attempt 1: Network timeout
 * Attempt 2: Success!
 * User experience: Payment successful (good!)
 * </pre>
 * 
 * <h2>Idempotency:</h2>
 * <pre>
 * Critical for Payment Processing!
 * 
 * Problem:
 * 1. User clicks "Pay" button
 * 2. Payment processes successfully
 * 3. Network timeout before response
 * 4. User clicks "Pay" again (didn't see success)
 * 5. Charged twice!
 * 
 * Solution: Idempotency Key
 * 
 * First Request:
 * POST /api/payments/process
 * Idempotency-Key: order-123
 * → Process payment
 * → Store: key=order-123, result=success
 * → Return: transaction-456
 * 
 * Retry Request (same key):
 * POST /api/payments/process
 * Idempotency-Key: order-123
 * → Check: key=order-123 exists
 * → Return cached result: transaction-456
 * → No duplicate charge!
 * 
 * Implementation:
 * - Use orderId as idempotency key
 * - Store in Redis (24 hour TTL)
 * - Check before processing
 * - Return cached result if exists
 * </pre>
 * 
 * <h2>Payment Gateway Integration:</h2>
 * <pre>
 * Multiple Gateway Support:
 * 1. Stripe (Primary)
 * 2. PayPal (Secondary)
 * 3. Braintree (Backup)
 * 
 * Strategy Pattern:
 * interface PaymentGateway {
 *     PaymentResult process(PaymentRequest);
 *     RefundResult refund(RefundRequest);
 * }
 * 
 * class StripeGateway implements PaymentGateway { ... }
 * class PayPalGateway implements PaymentGateway { ... }
 * 
 * Gateway Selection:
 * 1. User preference
 * 2. Transaction amount
 * 3. Geographic region
 * 4. Gateway availability
 * 
 * Failover:
 * Primary (Stripe) down → Failover to Secondary (PayPal)
 * </pre>
 * 
 * <h2>Transaction States:</h2>
 * <pre>
 * Payment Lifecycle:
 * 
 * PENDING → AUTHORIZED → CAPTURED → SETTLED
 *     ↓           ↓           ↓
 *  FAILED    CANCELLED   REFUNDED
 * 
 * PENDING: Payment initiated
 * AUTHORIZED: Funds reserved (hold)
 * CAPTURED: Funds transferred
 * SETTLED: Funds in account
 * REFUNDED: Money returned to customer
 * FAILED: Payment declined/error
 * CANCELLED: User/system cancelled
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableDiscoveryClient  // Register with Eureka
@EnableCaching  // Enable Redis caching
@EnableKafka  // Enable Kafka for events
@EnableAsync  // Enable async processing
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

