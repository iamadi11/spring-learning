package com.ecommerce.payment.gateway;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.RefundRequest;
import com.ecommerce.payment.dto.RefundResponse;
import com.ecommerce.payment.exception.PaymentDeclinedException;
import com.ecommerce.payment.exception.PaymentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Stripe Gateway Simulator
 * 
 * <p>Mock implementation of Stripe payment gateway for testing resilience patterns.</p>
 * 
 * <h2>Simulated Behaviors:</h2>
 * <pre>
 * 1. Success (80% chance)
 * 2. Transient Failure - Network timeout (10% chance)
 *    → Should RETRY
 * 3. Permanent Failure - Card declined (10% chance)
 *    → Should NOT retry
 * 4. Slow Response - Random delay
 *    → Circuit breaker should detect
 * </pre>
 * 
 * <h2>Testing Resilience Patterns:</h2>
 * <pre>
 * Circuit Breaker:
 * - Simulate high failure rate
 * - Watch circuit open
 * - Observe fail-fast behavior
 * 
 * Retry:
 * - Throw IOException (transient)
 * - Verify automatic retry
 * - Check exponential backoff
 * 
 * Rate Limiter:
 * - Make many requests
 * - Observe throttling
 * - Check 429 responses
 * 
 * Bulkhead:
 * - Simulate slow processing
 * - Fill thread pool
 * - Verify isolation
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Component
public class StripeGatewaySimulator implements PaymentGateway {

    // Random for simulating failures
    private final Random random = new Random();
    
    // Failure probabilities
    private static final double TRANSIENT_FAILURE_RATE = 0.10;  // 10%
    private static final double PERMANENT_FAILURE_RATE = 0.10;  // 10%
    private static final double SLOW_RESPONSE_RATE = 0.20;     // 20%

    /**
     * Process payment with simulated behaviors
     */
    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Stripe: Processing payment for order: {}, amount: {}", 
                 request.getOrderId(), request.getAmount());

        try {
            // Simulate network latency
            simulateLatency();

            // Simulate various failure scenarios
            simulateFailures();

            // Success path
            String transactionId = "stripe_" + UUID.randomUUID().toString();
            
            log.info("Stripe: Payment successful - transaction: {}", transactionId);
            
            return PaymentResponse.builder()
                .transactionId(transactionId)
                .success(true)
                .gateway("STRIPE")
                .responseCode("SUCCESS")
                .message("Payment processed successfully")
                .timestamp(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Stripe: Payment failed for order: {} - {}", 
                     request.getOrderId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Authorize payment (reserve funds)
     */
    @Override
    public PaymentResponse authorizePayment(PaymentRequest request) {
        log.info("Stripe: Authorizing payment for order: {}", request.getOrderId());
        
        try {
            simulateLatency();
            simulateFailures();
            
            String authorizationId = "auth_" + UUID.randomUUID().toString();
            
            return PaymentResponse.builder()
                .transactionId(authorizationId)
                .success(true)
                .gateway("STRIPE")
                .responseCode("AUTHORIZED")
                .message("Payment authorized successfully")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Stripe: Authorization failed - {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Capture payment (transfer funds)
     */
    @Override
    public PaymentResponse capturePayment(String transactionId, BigDecimal amount) {
        log.info("Stripe: Capturing payment - transaction: {}, amount: {}", 
                 transactionId, amount);
        
        try {
            simulateLatency();
            
            String captureId = "capture_" + UUID.randomUUID().toString();
            
            return PaymentResponse.builder()
                .transactionId(captureId)
                .success(true)
                .gateway("STRIPE")
                .responseCode("CAPTURED")
                .message("Payment captured successfully")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Stripe: Capture failed - {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Refund payment
     */
    @Override
    public RefundResponse refundPayment(RefundRequest request) {
        log.info("Stripe: Processing refund for transaction: {}, amount: {}", 
                 request.getTransactionId(), request.getAmount());
        
        try {
            simulateLatency();
            // Refunds usually more reliable
            
            String refundId = "refund_" + UUID.randomUUID().toString();
            
            return RefundResponse.builder()
                .refundId(refundId)
                .success(true)
                .gateway("STRIPE")
                .message("Refund processed successfully")
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Stripe: Refund failed - {}", e.getMessage());
            throw new PaymentProcessingException("Refund failed: " + e.getMessage());
        }
    }

    /**
     * Get gateway name
     */
    @Override
    public String getGatewayName() {
        return "STRIPE";
    }

    /**
     * Check availability
     */
    @Override
    public boolean isAvailable() {
        // In real implementation: check API status
        return true;
    }

    /**
     * Simulate network latency and slow responses
     */
    private void simulateLatency() {
        try {
            // Normal latency: 100-500ms
            int baseDelay = 100 + random.nextInt(400);
            
            // Occasionally slow (> 10s triggers circuit breaker)
            if (random.nextDouble() < SLOW_RESPONSE_RATE) {
                baseDelay = 2000 + random.nextInt(8000);  // 2-10 seconds
                log.warn("Stripe: Simulating slow response - {}ms", baseDelay);
            }
            
            Thread.sleep(baseDelay);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentProcessingException("Processing interrupted");
        }
    }

    /**
     * Simulate various failure scenarios
     */
    private void simulateFailures() {
        double rand = random.nextDouble();
        
        // Transient failure (should retry)
        if (rand < TRANSIENT_FAILURE_RATE) {
            log.warn("Stripe: Simulating transient failure (network timeout)");
            throw new PaymentProcessingException(
                "Network timeout - connection to payment gateway failed",
                new IOException("Connection timeout"));
        }
        
        // Permanent failure (should not retry)
        if (rand < TRANSIENT_FAILURE_RATE + PERMANENT_FAILURE_RATE) {
            log.warn("Stripe: Simulating permanent failure (card declined)");
            throw new PaymentDeclinedException(
                "Card declined",
                "insufficient_funds",
                "DECLINED");
        }
        
        // Success - no exception thrown
    }
}

