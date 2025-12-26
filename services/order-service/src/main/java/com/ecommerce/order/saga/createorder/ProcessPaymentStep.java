package com.ecommerce.order.saga.createorder;

import com.ecommerce.order.client.PaymentServiceClient;
import com.ecommerce.order.dto.PaymentResponse;
import com.ecommerce.order.dto.ProcessPaymentRequest;
import com.ecommerce.order.dto.RefundPaymentRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Process Payment Step
 * 
 * <p>Step 3: Processes payment via Payment Service.</p>
 * 
 * <h2>Responsibilities:</h2>
 * <pre>
 * Forward (execute):
 * - Call Payment Service to charge customer
 * - Store transaction ID in context and order
 * - Update order payment status to CAPTURED
 * 
 * Compensation (compensate):
 * - Call Payment Service to refund
 * - Use stored transaction ID
 * - Update order payment status to REFUNDED
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Component
public class ProcessPaymentStep implements SagaStep<CreateOrderContext> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessPaymentStep.class);

    private final PaymentServiceClient paymentClient;
    private final OrderRepository orderRepository;

    @Autowired
    public ProcessPaymentStep(
            PaymentServiceClient paymentClient,
            OrderRepository orderRepository) {
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
    }

    @Override
    public void execute(CreateOrderContext context) throws Exception {
        logger.info("Executing ProcessPaymentStep for order: {}", 
            context.getOrder().getOrderId());

        Order order = context.getOrder();

        // Build payment request
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .amount(order.getFinalAmount())
            .paymentMethod(order.getPaymentMethod())
            .currency("USD")
            .build();

        try {
            // Call Payment Service
            PaymentResponse response = paymentClient.processPayment(request);

            // Check response
            if (!"SUCCESS".equals(response.getStatus())) {
                throw new RuntimeException("Payment failed: " + response.getMessage());
            }

            // Store transaction ID
            String transactionId = response.getTransactionId();
            context.setPaymentTransactionId(transactionId);
            
            // Update order
            order.setPaymentTransactionId(transactionId);
            order.setPaymentStatus(PaymentStatus.CAPTURED);
            orderRepository.save(order);

            logger.info("Payment processed successfully for order: {}, transactionId: {}", 
                order.getOrderId(), transactionId);

        } catch (Exception e) {
            logger.error("Payment processing failed for order: {}", order.getOrderId(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void compensate(CreateOrderContext context) throws Exception {
        logger.info("Compensating ProcessPaymentStep for order: {}", 
            context.getOrder().getOrderId());

        Order order = context.getOrder();
        String transactionId = context.getPaymentTransactionId();

        if (transactionId == null) {
            logger.warn("No transaction ID found - payment may not have been processed");
            return;
        }

        // Build refund request
        RefundPaymentRequest request = RefundPaymentRequest.builder()
            .orderId(order.getOrderId())
            .transactionId(transactionId)
            .amount(order.getFinalAmount())
            .reason("Order cancelled - saga compensation")
            .build();

        try {
            // Call Payment Service
            PaymentResponse response = paymentClient.refundPayment(request);

            // Update order
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);

            logger.info("Payment refunded successfully for order: {}, transactionId: {}", 
                order.getOrderId(), transactionId);

        } catch (Exception e) {
            // Critical: Refund failed!
            logger.error("Payment refund failed for order: {} - MANUAL INTERVENTION REQUIRED", 
                order.getOrderId(), e);
            
            // In production: Send critical alert
            // alertService.sendCriticalAlert(...)
            
            throw e;  // Re-throw to mark saga as FAILED
        }
    }

    @Override
    public String getStepName() {
        return "ProcessPayment";
    }
}

