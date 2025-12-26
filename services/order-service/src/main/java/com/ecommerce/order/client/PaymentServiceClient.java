package com.ecommerce.order.client;

import com.ecommerce.order.dto.PaymentResponse;
import com.ecommerce.order.dto.ProcessPaymentRequest;
import com.ecommerce.order.dto.RefundPaymentRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Payment Service Client
 * 
 * <p>Feign client for communicating with Payment Service.
 * Used by saga steps to process and refund payments.</p>
 * 
 * @author E-commerce Platform Team
 */
@FeignClient(
    name = "payment-service",
    fallback = PaymentServiceClientFallback.class
)
public interface PaymentServiceClient {

    /**
     * Process Payment
     * 
     * <p>Processes payment for an order.
     * Charges the customer's payment method.</p>
     * 
     * <p><b>Idempotent:</b> Uses orderId to prevent double-charging</p>
     * 
     * @param request Payment request
     * @return Payment response with transaction ID
     */
    @PostMapping("/api/payments/process")
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    PaymentResponse processPayment(@RequestBody ProcessPaymentRequest request);

    /**
     * Refund Payment
     * 
     * <p>Refunds a previously processed payment.
     * Called during saga compensation.</p>
     * 
     * <p><b>Idempotent:</b> Safe to call multiple times</p>
     * 
     * @param request Refund request
     * @return Refund response
     */
    @PostMapping("/api/payments/refund")
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    PaymentResponse refundPayment(@RequestBody RefundPaymentRequest request);
}

