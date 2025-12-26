package com.ecommerce.order.saga.createorder;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.ReleaseInventoryRequest;
import com.ecommerce.order.dto.ReserveInventoryRequest;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.saga.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reserve Inventory Step
 * 
 * <p>Step 2: Reserves product inventory via Product Service.</p>
 * 
 * <h2>Responsibilities:</h2>
 * <pre>
 * Forward (execute):
 * - For each order item:
 *   * Call Product Service to reserve inventory
 *   * Store reservation ID in context
 * - If any reservation fails → throw exception
 * 
 * Compensation (compensate):
 * - For each reserved item:
 *   * Call Product Service to release inventory
 *   * Use stored reservation IDs
 * - Must be idempotent (safe to retry)
 * </pre>
 * 
 * <h2>Idempotency:</h2>
 * <pre>
 * Product Service Implementation:
 * 
 * reserveInventory(orderId, productId, quantity):
 *   // Check if already reserved
 *   existing = reservations.findByOrderId(orderId, productId)
 *   if (existing != null):
 *     return existing  // Already reserved
 *   
 *   // Reserve new
 *   if (stock >= quantity):
 *     create reservation
 *     reduce stock
 *     return reservation
 *   else:
 *     throw InsufficientStockException
 * 
 * This ensures:
 * - Retry is safe (won't double-reserve)
 * - Uses orderId as idempotency key
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Component
public class ReserveInventoryStep implements SagaStep<CreateOrderContext> {

    private static final Logger logger = LoggerFactory.getLogger(ReserveInventoryStep.class);

    private final ProductServiceClient productClient;

    @Autowired
    public ReserveInventoryStep(ProductServiceClient productClient) {
        this.productClient = productClient;
    }

    @Override
    public void execute(CreateOrderContext context) throws Exception {
        logger.info("Executing ReserveInventoryStep for order: {}", 
            context.getOrder().getOrderId());

        // Reserve inventory for each item
        for (OrderItem item : context.getOrder().getItems()) {
            logger.debug("Reserving {} units of product: {}", 
                item.getQuantity(), item.getProductId());

            ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .orderId(context.getOrder().getOrderId())  // For idempotency
                .build();

            try {
                // Call Product Service
                productClient.reserveInventory(request);
                
                // Store reservation (orderId serves as reservation ID)
                context.addReservationId(item.getProductId());
                
                logger.debug("Reserved inventory for product: {}", item.getProductId());
                
            } catch (Exception e) {
                logger.error("Failed to reserve inventory for product: {}", 
                    item.getProductId(), e);
                throw new RuntimeException("Inventory reservation failed: " + e.getMessage(), e);
            }
        }

        logger.info("All inventory reserved for order: {}", context.getOrder().getOrderId());
    }

    @Override
    public void compensate(CreateOrderContext context) throws Exception {
        logger.info("Compensating ReserveInventoryStep for order: {}", 
            context.getOrder().getOrderId());

        // Release all reserved inventory
        for (OrderItem item : context.getOrder().getItems()) {
            logger.debug("Releasing {} units of product: {}", 
                item.getQuantity(), item.getProductId());

            ReleaseInventoryRequest request = ReleaseInventoryRequest.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .orderId(context.getOrder().getOrderId())  // For idempotency
                .build();

            try {
                // Call Product Service
                productClient.releaseInventory(request);
                
                logger.debug("Released inventory for product: {}", item.getProductId());
                
            } catch (Exception e) {
                // Log error but don't fail compensation
                // Compensation must succeed to maintain consistency
                logger.error("Failed to release inventory for product: {} - MANUAL INTERVENTION REQUIRED", 
                    item.getProductId(), e);
                
                // In production: Send alert to ops team
                // alertService.sendCriticalAlert(...)
            }
        }

        logger.info("Inventory release attempted for order: {}", context.getOrder().getOrderId());
    }

    @Override
    public String getStepName() {
        return "ReserveInventory";
    }
}

