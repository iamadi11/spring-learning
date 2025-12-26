package com.ecommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Order Item Entity
 * 
 * <p>Represents a single line item in an order.
 * Stores snapshot of product data at time of order.</p>
 * 
 * <h2>Why Store Product Snapshot?</h2>
 * <pre>
 * Problem:
 * - Product price changes after order placed
 * - Product name changes
 * - Product deleted
 * - Need historical accuracy
 * 
 * Solution: Store snapshot at order time
 * 
 * Example:
 * Order Date: 2024-01-01
 * Product: "Laptop" at $1000
 * 
 * Later:
 * Product price changes to $1200
 * But order still shows $1000 (correct!)
 * 
 * Benefits:
 * - Historical accuracy
 * - No foreign key dependency on Product
 * - Works even if product deleted
 * - Audit trail for pricing
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent Order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Product ID - reference to Product Service
     * 
     * <p>Not a foreign key - just a reference.
     * Product may be deleted but order item persists.</p>
     */
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /**
     * Product Name (snapshot at order time)
     */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /**
     * SKU (snapshot at order time)
     */
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    /**
     * Unit Price (snapshot at order time)
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Quantity ordered
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Subtotal (price × quantity)
     */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Product Image URL (snapshot)
     */
    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    /**
     * Calculate subtotal
     */
    public void calculateSubtotal() {
        if (price != null && quantity != null) {
            this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}

