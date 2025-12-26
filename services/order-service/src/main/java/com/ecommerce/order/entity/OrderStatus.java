package com.ecommerce.order.entity;

/**
 * Order Status Enum
 * 
 * <p>Represents the lifecycle states of an order in the system.</p>
 * 
 * <h2>Order State Machine:</h2>
 * <pre>
 * ┌─────────┐
 * │ PENDING │ ← Initial state after order creation
 * └────┬────┘
 *      │
 *      ↓ Saga starts
 * ┌──────────────┐
 * │ IN_PROGRESS  │ ← Saga executing (reserving inventory, processing payment)
 * └───┬──────────┘
 *     │
 *     ├─→ Success Path ─────────────────┐
 *     │                                  ↓
 *     │                            ┌───────────┐
 *     │                            │ CONFIRMED │ ← All steps succeeded
 *     │                            └─────┬─────┘
 *     │                                  │
 *     │                                  ↓
 *     │                            ┌──────────┐
 *     │                            │ SHIPPING │ ← Order being shipped
 *     │                            └─────┬────┘
 *     │                                  │
 *     │                                  ↓
 *     │                            ┌───────────┐
 *     │                            │ DELIVERED │ ← Order delivered successfully
 *     │                            └───────────┘
 *     │
 *     └─→ Failure Path ─────────────────┐
 *                                        ↓
 *                                  ┌───────────┐
 *                                  │ CANCELLED │ ← Saga compensation executed
 *                                  └───────────┘
 * 
 * Status Transitions:
 * 
 * PENDING → IN_PROGRESS:
 *   - Saga starts execution
 *   - Begins reserving inventory
 * 
 * IN_PROGRESS → CONFIRMED:
 *   - Inventory reserved ✓
 *   - Payment processed ✓
 *   - All saga steps succeeded
 * 
 * IN_PROGRESS → CANCELLED:
 *   - Any saga step failed
 *   - Compensation executed
 *   - Order cannot be fulfilled
 * 
 * CONFIRMED → SHIPPING:
 *   - Warehouse picks and packs
 *   - Shipping label created
 *   - Carrier notified
 * 
 * SHIPPING → DELIVERED:
 *   - Package delivered
 *   - Signature obtained
 *   - Tracking updated
 * 
 * CONFIRMED/SHIPPING → CANCELLED:
 *   - Customer cancels
 *   - Refund processed
 *   - Inventory released
 * 
 * Any State → FAILED:
 *   - System error
 *   - Manual intervention needed
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum OrderStatus {
    
    /**
     * PENDING - Order created, awaiting saga execution
     * 
     * <p>Initial state when order is first created.
     * Order exists but no processing has started yet.</p>
     */
    PENDING,
    
    /**
     * IN_PROGRESS - Saga executing
     * 
     * <p>Order is being processed:
     * - Reserving inventory from Product Service
     * - Processing payment via Payment Service
     * - Sending notifications</p>
     */
    IN_PROGRESS,
    
    /**
     * CONFIRMED - Order confirmed, ready to ship
     * 
     * <p>All saga steps completed successfully:
     * - Inventory reserved
     * - Payment captured
     * - Customer notified
     * - Awaiting fulfillment</p>
     */
    CONFIRMED,
    
    /**
     * SHIPPING - Order being shipped
     * 
     * <p>Warehouse has picked, packed, and shipped:
     * - Tracking number assigned
     * - Carrier updated
     * - Customer notified of shipment</p>
     */
    SHIPPING,
    
    /**
     * DELIVERED - Order successfully delivered
     * 
     * <p>Final success state:
     * - Package delivered to customer
     * - Signature obtained (if required)
     * - Order complete</p>
     */
    DELIVERED,
    
    /**
     * CANCELLED - Order cancelled
     * 
     * <p>Order was cancelled due to:
     * - Saga failure (compensation executed)
     * - Customer cancellation
     * - System error
     * - Inventory unavailable
     * - Payment failure</p>
     */
    CANCELLED,
    
    /**
     * FAILED - Order failed (needs manual intervention)
     * 
     * <p>Severe failure requiring manual action:
     * - Compensation failed
     * - System error
     * - Data inconsistency
     * - Alerts operations team</p>
     */
    FAILED
}

