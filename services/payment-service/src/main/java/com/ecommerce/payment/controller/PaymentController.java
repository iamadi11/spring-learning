package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.RefundRequest;
import com.ecommerce.payment.dto.RefundResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Payment Controller
 * 
 * <p>REST API endpoints for payment processing.</p>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process payment
     * 
     * <p>POST /api/payments/process</p>
     * 
     * <h2>Idempotency:</h2>
     * <p>Safe to retry with same orderId - won't charge twice.</p>
     * 
     * <h2>Resilience:</h2>
     * <ul>
     *   <li>Circuit Breaker: Fails fast if gateway down</li>
     *   <li>Retry: Automatic retry on transient failures</li>
     *   <li>Rate Limit: 10 requests/second</li>
     *   <li>Timeout: 15 seconds max</li>
     * </ul>
     */
    @PostMapping("/process")
    public CompletableFuture<ResponseEntity<Payment>> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        
        log.info("Received payment request for order: {}", request.getOrderId());

        return paymentService.processPayment(request)
            .thenApply(payment -> {
                log.info("Payment processed successfully: {}", payment.getTransactionId());
                return ResponseEntity.status(HttpStatus.CREATED).body(payment);
            })
            .exceptionally(ex -> {
                log.error("Payment processing failed: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
            });
    }

    /**
     * Get payment by order ID
     * 
     * <p>GET /api/payments/order/{orderId}</p>
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        log.info("Fetching payment for order: {}", orderId);

        return paymentService.getPaymentByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get payment by transaction ID
     * 
     * <p>GET /api/payments/{transactionId}</p>
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String transactionId) {
        log.info("Fetching payment: {}", transactionId);

        return paymentService.getPaymentByOrderId(transactionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Refund payment
     * 
     * <p>POST /api/payments/refund</p>
     * 
     * <h2>Resilience:</h2>
     * <ul>
     *   <li>Circuit Breaker: Separate from payment gateway</li>
     *   <li>Retry: 3 attempts with exponential backoff</li>
     *   <li>Rate Limit: 5 requests/second</li>
     * </ul>
     */
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundPayment(
            @Valid @RequestBody RefundRequest request) {
        
        log.info("Received refund request for transaction: {}", request.getTransactionId());

        try {
            RefundResponse response = paymentService.refundPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Health check endpoint
     * 
     * <p>GET /api/payments/health</p>
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running");
    }
}

