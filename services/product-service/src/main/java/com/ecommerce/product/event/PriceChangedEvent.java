package com.ecommerce.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Price Changed Event
 * 
 * <p>Event emitted when product price is changed.
 * Tracks both old and new prices for audit trail.</p>
 * 
 * <h2>Use Case Example:</h2>
 * <pre>
 * Scenario: Holiday sale - reduce laptop price
 * 
 * Event:
 * {
 *   "aggregateId": "prod-laptop-001",
 *   "version": 5,
 *   "oldPrice": 2499.99,
 *   "newPrice": 1999.99,
 *   "reason": "Black Friday Sale",
 *   "timestamp": "2024-11-25T00:00:00Z"
 * }
 * 
 * Benefits:
 * - Know exact price at any time
 * - Track price history
 * - Audit trail for compliance
 * - Analytics on pricing strategies
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PriceChangedEvent extends BaseEvent {

    /**
     * Old price before change
     */
    private BigDecimal oldPrice;

    /**
     * New price after change
     */
    private BigDecimal newPrice;

    /**
     * Reason for price change (optional)
     */
    private String reason;
}

