package com.ecommerce.order.saga.createorder;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create Order Step
 * 
 * <p>Step 1: Creates the order entity and persists to database.</p>
 * 
 * <h2>Responsibilities:</h2>
 * <pre>
 * Forward (execute):
 * - Create Order entity with PENDING status
 * - Calculate totals
 * - Save to database
 * - Store order in context
 * 
 * Compensation (compensate):
 * - Mark order as CANCELLED
 * - Update cancelled timestamp
 * - Save to database
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Component
public class CreateOrderStep implements SagaStep<CreateOrderContext> {

    private static final Logger logger = LoggerFactory.getLogger(CreateOrderStep.class);

    private final OrderRepository orderRepository;

    @Autowired
    public CreateOrderStep(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void execute(CreateOrderContext context) throws Exception {
        logger.info("Executing CreateOrderStep for order: {}", context.getOrder().getOrderId());

        Order order = context.getOrder();
        
        // Set initial status
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        
        // Calculate totals
        order.calculateTotals();
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Update context
        context.setOrder(savedOrder);
        
        logger.info("Order created successfully: {}", savedOrder.getOrderId());
    }

    @Override
    public void compensate(CreateOrderContext context) throws Exception {
        logger.info("Compensating CreateOrderStep for order: {}", context.getOrder().getOrderId());

        Order order = context.getOrder();
        
        // Mark as cancelled
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setCancelledAt(java.time.LocalDateTime.now());
        
        // Save updated order
        orderRepository.save(order);
        
        logger.info("Order cancelled: {}", order.getOrderId());
    }

    @Override
    public String getStepName() {
        return "CreateOrder";
    }
}

