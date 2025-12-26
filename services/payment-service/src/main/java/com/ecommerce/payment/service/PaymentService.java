package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.DuplicatePaymentException;
import com.ecommerce.payment.exception.PaymentDeclinedException;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.gateway.PaymentGateway;
import com.ecommerce.payment.repository.PaymentRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Payment Service
 * 
 * <p>Core payment processing service demonstrating ALL resilience patterns.</p>
 * 
 * <h2>Applied Patterns:</h2>
 * <pre>
 * 1. @CircuitBreaker:
 *    - Protects against cascading failures
 *    - Opens after 50% failure rate
 *    - Falls back to alternative behavior
 * 
 * 2. @Retry:
 *    - Retries transient failures
 *    - Exponential backoff (1s, 2s, 4s)
 *    - Max 3 attempts
 * 
 * 3. @RateLimiter:
 *    - Controls request rate to gateway
 *    - Prevents cost overrun
 *    - Token bucket algorithm
 * 
 * 4. @Bulkhead:
 *    - Isolates thread pools
 *    - Prevents resource exhaustion
 *    - Payment vs Refund isolation
 * 
 * 5. @TimeLimiter:
 *    - Enforces timeout
 *    - Fails fast if too slow
 *    - Prevents thread blocking
 * 
 * 6. @Cacheable:
 *    - Caches idempotency checks
 *    - Redis-based distributed cache
 *    - 24-hour TTL
 * </pre>
 * 
 * <h2>Order of Execution:</h2>
 * <pre>
 * Request → RateLimiter → CircuitBreaker → TimeLimiter → Bulkhead → Retry → Method
 * 
 * Example Flow (Success):
 * 1. RateLimiter: Check token available → Pass
 * 2. CircuitBreaker: Check state CLOSED → Pass
 * 3. TimeLimiter: Start timer (15s)
 * 4. Bulkhead: Acquire thread from pool
 * 5. Retry: Attempt 1
 * 6. Method: Process payment
 * 7. Gateway: Call Stripe → Success
 * 8. Return: Payment successful
 * 
 * Example Flow (Transient Failure):
 * 1-4. Same as above
 * 5. Retry: Attempt 1 → IOException
 * 6. Wait 1 second (exponential backoff)
 * 7. Retry: Attempt 2 → Success
 * 8. Return: Payment successful
 * 
 * Example Flow (Circuit Open):
 * 1. RateLimiter: Pass
 * 2. CircuitBreaker: State OPEN → Fail Fast
 * 3. Fallback: Return cached/alternative response
 * 4. No gateway call (saves time/resources)
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    /**
     * Process payment with ALL resilience patterns
     * 
     * <h2>Resilience Annotations:</h2>
     * <pre>
     * @CircuitBreaker: Protect against gateway failures
     * @Retry: Retry transient failures
     * @RateLimiter: Limit requests to gateway
     * @Bulkhead: Isolate payment processing threads
     * @TimeLimiter: Enforce 15s timeout
     * </pre>
     */
    @Transactional
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentGateway")
    @RateLimiter(name = "paymentGateway")
    @Bulkhead(name = "paymentGateway", type = Bulkhead.Type.SEMAPHORE)
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<Payment> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Processing payment for order: {}, amount: {}", 
                     request.getOrderId(), request.getAmount());

            // STEP 1: Idempotency Check
            // Prevent duplicate charges if request retried
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
            if (existingPayment.isPresent()) {
                Payment existing = existingPayment.get();
                if (existing.isSuccessful()) {
                    log.warn("Payment already processed for order: {} - returning existing payment", 
                             request.getOrderId());
                    throw new DuplicatePaymentException(
                        "Payment already processed for order: " + request.getOrderId());
                }
                // If previous attempt failed, continue with new attempt
                log.info("Previous payment attempt failed - retrying for order: {}", request.getOrderId());
            }

            // STEP 2: Fraud Check
            validateFraudRules(request);

            // STEP 3: Create pending payment record
            Payment payment = Payment.builder()
                .transactionId(null)  // Will be set after gateway response
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .gateway(paymentGateway.getGatewayName())
                .retryCount(0)
                .metadata(buildMetadata(request))
                .build();

            try {
                // STEP 4: Call payment gateway (with all resilience patterns)
                PaymentResponse gatewayResponse = paymentGateway.processPayment(request);

                // STEP 5: Update payment with gateway response
                payment.setTransactionId(gatewayResponse.getTransactionId());
                payment.setStatus(PaymentStatus.CAPTURED);
                payment.setGatewayTransactionId(gatewayResponse.getTransactionId());
                payment.setGatewayResponseCode(gatewayResponse.getResponseCode());
                payment.setGatewayResponse(gatewayResponse.getMessage());

                // STEP 6: Save successful payment
                payment = paymentRepository.save(payment);
                
                log.info("Payment processed successfully - transaction: {}", 
                         payment.getTransactionId());

                return payment;

            } catch (PaymentDeclinedException e) {
                // PERMANENT FAILURE - Don't retry
                log.error("Payment declined for order: {} - {}", request.getOrderId(), e.getMessage());
                
                payment.setStatus(PaymentStatus.DECLINED);
                payment.setFailureReason(e.getDeclineCode());
                payment.setGatewayResponseCode(e.getResponseCode());
                paymentRepository.save(payment);
                
                throw e;

            } catch (Exception e) {
                // TRANSIENT FAILURE - Will retry (if configured)
                log.error("Payment processing failed for order: {} - {}", 
                          request.getOrderId(), e.getMessage());
                
                payment.incrementRetry();
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(e.getMessage());
                paymentRepository.save(payment);
                
                throw new PaymentProcessingException("Payment processing failed", e);
            }
        });
    }

    /**
     * Fallback method when Circuit Breaker is OPEN
     * 
     * <p>Called when:
     * - Circuit breaker is open (too many failures)
     * - All retry attempts exhausted
     * - Timeout exceeded
     * </p>
     * 
     * <h2>Fallback Strategies:</h2>
     * <pre>
     * 1. Return cached payment (if exists)
     * 2. Queue for later processing
     * 3. Try alternative gateway
     * 4. Return graceful error
     * </pre>
     */
    public CompletableFuture<Payment> processPaymentFallback(PaymentRequest request, Exception e) {
        return CompletableFuture.supplyAsync(() -> {
            log.warn("Payment gateway unavailable - executing fallback for order: {}", 
                     request.getOrderId());

            // Check if payment already processed (idempotency)
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
            if (existingPayment.isPresent() && existingPayment.get().isSuccessful()) {
                log.info("Returning cached successful payment for order: {}", request.getOrderId());
                return existingPayment.get();
            }

            // Create failed payment record
            Payment failedPayment = Payment.builder()
                .transactionId("fallback_" + request.getOrderId())
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.FAILED)
                .gateway(paymentGateway.getGatewayName())
                .failureReason("Payment gateway temporarily unavailable - " + e.getMessage())
                .metadata(buildMetadata(request))
                .build();

            paymentRepository.save(failedPayment);

            throw new PaymentProcessingException(
                "Payment service temporarily unavailable. Please try again later.");
        });
    }

    /**
     * Refund payment with resilience patterns
     */
    @Transactional
    @CircuitBreaker(name = "refundGateway", fallbackMethod = "refundPaymentFallback")
    @Retry(name = "refundGateway")
    @RateLimiter(name = "refundGateway")
    @Bulkhead(name = "refundGateway", type = Bulkhead.Type.SEMAPHORE)
    public RefundResponse refundPayment(RefundRequest request) {
        log.info("Processing refund for transaction: {}, amount: {}", 
                 request.getTransactionId(), request.getAmount());

        // Find original payment
        Payment payment = paymentRepository.findById(request.getTransactionId())
            .orElseThrow(() -> new PaymentProcessingException("Payment not found"));

        // Validate refund
        if (!payment.isRefundable()) {
            throw new PaymentProcessingException("Payment cannot be refunded");
        }

        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new PaymentProcessingException("Refund amount exceeds payment amount");
        }

        try {
            // Call gateway
            RefundResponse gatewayResponse = paymentGateway.refundPayment(request);

            // Update payment
            payment.setRefundAmount(request.getAmount());
            payment.setRefundedAt(LocalDateTime.now());
            
            if (request.getAmount().compareTo(payment.getAmount()) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }

            paymentRepository.save(payment);

            log.info("Refund processed successfully - refund ID: {}", gatewayResponse.getRefundId());
            return gatewayResponse;

        } catch (Exception e) {
            log.error("Refund failed for transaction: {} - {}", 
                     request.getTransactionId(), e.getMessage());
            throw new PaymentProcessingException("Refund processing failed", e);
        }
    }

    /**
     * Refund fallback
     */
    public RefundResponse refundPaymentFallback(RefundRequest request, Exception e) {
        log.warn("Refund gateway unavailable - executing fallback for transaction: {}", 
                 request.getTransactionId());

        // Queue for later processing
        // In production: add to message queue

        return RefundResponse.builder()
            .refundId("pending_" + request.getTransactionId())
            .success(false)
            .gateway(paymentGateway.getGatewayName())
            .message("Refund queued for processing - gateway temporarily unavailable")
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Get payment by order ID (with caching)
     */
    @Cacheable(value = "payments", key = "#orderId")
    public Optional<Payment> getPaymentByOrderId(String orderId) {
        log.debug("Fetching payment for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Validate fraud rules
     */
    private void validateFraudRules(PaymentRequest request) {
        // Check daily limit
        LocalDateTime dayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        BigDecimal dailyTotal = paymentRepository.sumAmountByUserAndDateAfter(
            request.getUserId(), dayStart);

        BigDecimal dailyLimit = new BigDecimal("50000.00");
        if (dailyTotal.add(request.getAmount()).compareTo(dailyLimit) > 0) {
            throw new PaymentProcessingException("Daily transaction limit exceeded");
        }

        // Check failed attempts (last 30 minutes)
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        long failedAttempts = paymentRepository.countByUserIdAndStatusAndCreatedAtAfter(
            request.getUserId(), PaymentStatus.DECLINED, thirtyMinutesAgo);

        if (failedAttempts >= 5) {
            throw new PaymentProcessingException(
                "Too many failed payment attempts. Account temporarily locked.");
        }
    }

    /**
     * Build metadata JSON
     */
    private String buildMetadata(PaymentRequest request) {
        return String.format(
            "{\"ipAddress\":\"%s\",\"userAgent\":\"%s\",\"deviceId\":\"%s\"}",
            request.getIpAddress(),
            request.getUserAgent(),
            request.getDeviceId()
        );
    }
}

