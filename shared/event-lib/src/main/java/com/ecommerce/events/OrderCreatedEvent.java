package com.ecommerce.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Created Event
 * 
 * <p>This event is published when a new order is created. It triggers downstream
 * processes like payment processing, inventory update, and notifications.</p>
 * 
 * <h2>Event-Driven Architecture:</h2>
 * <pre>
 * Order Service creates order
 *   ↓
 * Publishes OrderCreatedEvent to Kafka
 *   ↓
 * Multiple services consume the event:
 *   - Payment Service: Processes payment
 *   - Product Service: Updates inventory
 *   - Notification Service: Sends confirmation email
 * </pre>
 * 
 * <h2>Kafka Topic:</h2>
 * <pre>
 * Topic: order.created
 * Partitions: 3 (for parallel processing)
 * Replication: 2 (for fault tolerance)
 * </pre>
 * 
 * <h2>Event Flow:</h2>
 * <pre>
 * 1. Order Service:
 *    - User clicks "Place Order"
 *    - Create Order entity in database
 *    - Publish OrderCreatedEvent
 * 
 * 2. Payment Service:
 *    - Consumes OrderCreatedEvent
 *    - Processes payment
 *    - Publishes PaymentProcessedEvent or PaymentFailedEvent
 * 
 * 3. Product Service:
 *    - Consumes OrderCreatedEvent
 *    - Reserves inventory
 *    - Publishes InventoryReservedEvent
 * 
 * 4. Notification Service:
 *    - Consumes OrderCreatedEvent
 *    - Sends order confirmation email
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Required for deserialization
@AllArgsConstructor  // Convenient constructor
public class OrderCreatedEvent {
    
    /**
     * Unique identifier for the order
     * This ID is used across all services to track this specific order
     */
    private String orderId;
    
    /**
     * ID of the user who placed the order
     * Used to send notifications and update user's order history
     */
    private String userId;
    
    /**
     * List of items in the order
     * Each item contains product ID, quantity, and price
     */
    private List<OrderItem> items;
    
    /**
     * Total amount of the order
     * Sum of all item prices + taxes + shipping
     */
    private BigDecimal totalAmount;
    
    /**
     * Shipping address ID
     * Reference to the address where order should be delivered
     */
    private String shippingAddressId;
    
    /**
     * Payment method ID
     * Reference to the payment method (credit card, PayPal, etc.)
     */
    private String paymentMethodId;
    
    /**
     * Timestamp when the order was created
     * Used for tracking and SLA monitoring
     */
    private LocalDateTime createdAt;
    
    /**
     * Order Item - Represents a single item in the order
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        /**
         * Product ID
         */
        private String productId;
        
        /**
         * Quantity ordered
         */
        private Integer quantity;
        
        /**
         * Price per unit at the time of order
         * Stored to preserve historical pricing
         */
        private BigDecimal price;
    }
}

