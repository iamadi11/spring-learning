package com.ecommerce.order.saga.createorder;

import com.ecommerce.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Create Order Saga Context
 * 
 * <p>Carries shared data between saga steps.
 * Each step can read and modify context data.</p>
 * 
 * <h2>Context Pattern:</h2>
 * <pre>
 * Why Context?
 * - Steps need to share data
 * - Avoid global variables
 * - Enable step composition
 * - Support compensation
 * 
 * Example Flow:
 * 
 * Step 1 (CreateOrder):
 * - Creates order
 * - Stores order in context
 * 
 * Step 2 (ReserveInventory):
 * - Reads order from context
 * - Calls Product Service
 * - Stores reservation IDs in context
 * 
 * Step 3 (ProcessPayment):
 * - Reads order from context
 * - Calls Payment Service
 * - Stores transaction ID in context
 * 
 * Step 4 (ConfirmOrder):
 * - Reads all data from context
 * - Updates order status
 * - Marks order as CONFIRMED
 * 
 * Compensation:
 * - Each step reads context
 * - Uses stored IDs to undo operations
 * - Example: transactionId for refund
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderContext {

    /**
     * The order being created
     * 
     * <p>Created in Step 1, used by all subsequent steps.</p>
     */
    private Order order;

    /**
     * User ID placing the order
     */
    private Long userId;

    /**
     * Reservation IDs from Product Service
     * 
     * <p>Stored in Step 2, used for compensation in case of failure.</p>
     */
    @Builder.Default
    private List<String> reservationIds = new ArrayList<>();

    /**
     * Payment Transaction ID from Payment Service
     * 
     * <p>Stored in Step 3, used for refund in compensation.</p>
     */
    private String paymentTransactionId;

    /**
     * Saga ID
     */
    private String sagaId;

    /**
     * Add reservation ID
     * 
     * @param reservationId Reservation ID to add
     */
    public void addReservationId(String reservationId) {
        if (reservationIds == null) {
            reservationIds = new ArrayList<>();
        }
        reservationIds.add(reservationId);
    }
}

