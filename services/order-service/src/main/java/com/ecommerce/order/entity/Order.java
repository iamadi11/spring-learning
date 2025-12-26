package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity
 * 
 * <p>Represents a customer order in the e-commerce system.
 * Central entity in the Saga pattern for distributed transactions.</p>
 * 
 * <h2>Order Lifecycle:</h2>
 * <pre>
 * 1. Customer places order → Order created (PENDING)
 * 2. Saga starts → Status: IN_PROGRESS
 * 3. Reserve inventory → Product Service called
 * 4. Process payment → Payment Service called
 * 5. Confirm order → Status: CONFIRMED
 * 6. Ship order → Status: SHIPPING
 * 7. Deliver order → Status: DELIVERED
 * 
 * On Failure:
 * - Any step fails → Start compensation
 * - Release inventory
 * - Refund payment
 * - Status: CANCELLED
 * </pre>
 * 
 * <h2>Database Schema:</h2>
 * <pre>
 * Table: orders
 * ┌─────────────────┬──────────────┬────────────┐
 * │ Column          │ Type         │ Constraint │
 * ├─────────────────┼──────────────┼────────────┤
 * │ order_id        │ VARCHAR(36)  │ PK         │
 * │ user_id         │ BIGINT       │ FK         │
 * │ order_number    │ VARCHAR(50)  │ UNIQUE     │
 * │ status          │ VARCHAR(20)  │            │
 * │ payment_status  │ VARCHAR(20)  │            │
 * │ total_amount    │ DECIMAL      │            │
 * │ shipping_fee    │ DECIMAL      │            │
 * │ tax_amount      │ DECIMAL      │            │
 * │ discount_amount │ DECIMAL      │            │
 * │ final_amount    │ DECIMAL      │            │
 * │ shipping_address│ JSON/TEXT    │            │
 * │ billing_address │ JSON/TEXT    │            │
 * │ notes           │ TEXT         │            │
 * │ saga_id         │ VARCHAR(36)  │ FK         │
 * │ created_at      │ TIMESTAMP    │            │
 * │ updated_at      │ TIMESTAMP    │            │
 * └─────────────────┴──────────────┴────────────┘
 * 
 * Table: order_items
 * ┌──────────────┬──────────────┬────────────┐
 * │ Column       │ Type         │ Constraint │
 * ├──────────────┼──────────────┼────────────┤
 * │ id           │ BIGSERIAL    │ PK         │
 * │ order_id     │ VARCHAR(36)  │ FK         │
 * │ product_id   │ VARCHAR(36)  │            │
 * │ product_name │ VARCHAR(200) │            │
 * │ sku          │ VARCHAR(50)  │            │
 * │ price        │ DECIMAL      │            │
 * │ quantity     │ INTEGER      │            │
 * │ subtotal     │ DECIMAL      │            │
 * └──────────────┴──────────────┴────────────┘
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@Builder  // Lombok: builder pattern
@Entity  // JPA entity
@Table(name = "orders")  // Map to orders table
public class Order {

    /**
     * Order ID - unique identifier
     * 
     * <p>Generated as UUID.</p>
     */
    @Id
    @Column(name = "order_id", length = 36)
    private String orderId;

    /**
     * User ID - customer who placed the order
     * 
     * <p>Reference to User Service.</p>
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Order Number - human-readable order number
     * 
     * <p>Format: ORD-20240101-0001
     * Unique, sequential, customer-facing.</p>
     */
    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    /**
     * Order Status
     * 
     * <p>Tracks order lifecycle from PENDING to DELIVERED/CANCELLED.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /**
     * Payment Status
     * 
     * <p>Tracks payment processing state.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    /**
     * Order Items - products in this order
     * 
     * <p>One-to-Many relationship with OrderItem.
     * Cascade all operations to items.</p>
     */
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Total Amount - sum of all item subtotals
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Shipping Fee
     */
    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    /**
     * Tax Amount
     */
    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    /**
     * Discount Amount
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * Final Amount - amount to charge customer
     * 
     * <p>finalAmount = totalAmount + shippingFee + taxAmount - discountAmount</p>
     */
    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    /**
     * Payment Method
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /**
     * Payment Transaction ID from Payment Service
     */
    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;

    /**
     * Shipping Address (JSON or separate table)
     */
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    /**
     * Billing Address
     */
    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    /**
     * Customer Notes
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Tracking Number (once shipped)
     */
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    /**
     * Carrier (e.g., FedEx, UPS, DHL)
     */
    @Column(name = "carrier", length = 50)
    private String carrier;

    /**
     * Saga ID - links to SagaExecution
     * 
     * <p>Tracks the saga that processed this order.</p>
     */
    @Column(name = "saga_id", length = 36)
    private String sagaId;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Cancelled At timestamp
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Shipped At timestamp
     */
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    /**
     * Delivered At timestamp
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // ============= Helper Methods =============

    /**
     * Add item to order
     * 
     * @param item OrderItem to add
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Remove item from order
     * 
     * @param item OrderItem to remove
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    /**
     * Calculate and set total amounts
     */
    public void calculateTotals() {
        // Sum of all item subtotals
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Final amount = total + shipping + tax - discount
        this.finalAmount = this.totalAmount
            .add(this.shippingFee != null ? this.shippingFee : BigDecimal.ZERO)
            .add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO)
            .subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
    }

    /**
     * Check if order can be cancelled
     * 
     * @return true if cancellable
     */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.IN_PROGRESS || 
               status == OrderStatus.CONFIRMED;
    }

    /**
     * Check if order is in terminal state
     * 
     * @return true if terminal (DELIVERED, CANCELLED, FAILED)
     */
    public boolean isTerminal() {
        return status == OrderStatus.DELIVERED || 
               status == OrderStatus.CANCELLED || 
               status == OrderStatus.FAILED;
    }
}

