package com.ecommerce.order.client;

import com.ecommerce.order.dto.PaymentResponse;
import com.ecommerce.order.dto.ProcessPaymentRequest;
import com.ecommerce.order.dto.RefundPaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Payment Service Client Fallback
 * 
 * @author E-commerce Platform Team
 */
@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceClientFallback.class);

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        logger.error("Payment Service unavailable - cannot process payment for order: {}", 
            request.getOrderId());
        throw new RuntimeException("Payment Service unavailable");
    }

    @Override
    public PaymentResponse refundPayment(RefundPaymentRequest request) {
        logger.error("Payment Service unavailable - cannot refund payment for order: {}", 
            request.getOrderId());
        throw new RuntimeException("Payment Service unavailable");
    }
}

