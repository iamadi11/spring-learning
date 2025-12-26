package com.ecommerce.order.saga.createorder;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Confirm Order Step
 * 
 * <p>Step 4 (Final): Confirms the order after all operations succeed.</p>
 * 
 * <h2>Responsibilities:</h2>
 * <pre>
 * Forward (execute):
 * - Update order status to CONFIRMED
 * - Set saga ID
 * - Save to database
 * - Publish OrderConfirmedEvent (future)
 * 
 * Compensation (compensate):
 * - No compensation needed
 * - Order already marked CANCELLED in CreateOrderStep
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Component
public class ConfirmOrderStep implements SagaStep<CreateOrderContext> {

    private static final Logger logger = LoggerFactory.getLogger(ConfirmOrderStep.class);

    private final OrderRepository orderRepository;

    @Autowired
    public ConfirmOrderStep(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void execute(CreateOrderContext context) throws Exception {
        logger.info("Executing ConfirmOrderStep for order: {}", 
            context.getOrder().getOrderId());

        Order order = context.getOrder();

        // Update status to CONFIRMED
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSagaId(context.getSagaId());

        // Save order
        orderRepository.save(order);

        logger.info("Order confirmed successfully: {}", order.getOrderId());

        // TODO: Publish OrderConfirmedEvent to Kafka
        // OrderConfirmedEvent event = new OrderConfirmedEvent(order);
        // kafkaTemplate.send("order-events", event);
    }

    @Override
    public void compensate(CreateOrderContext context) throws Exception {
        // No compensation needed - order already cancelled in CreateOrderStep
        logger.info("ConfirmOrderStep compensation - no action needed for order: {}", 
            context.getOrder().getOrderId());
    }

    @Override
    public String getStepName() {
        return "ConfirmOrder";
    }
}

