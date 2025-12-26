package com.ecommerce.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Stock Changed Event
 * 
 * <p>Event emitted when product stock quantity changes.</p>
 * 
 * @author E-commerce Platform Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StockChangedEvent extends BaseEvent {

    /**
     * Old stock quantity
     */
    private Integer oldStock;

    /**
     * New stock quantity
     */
    private Integer newStock;

    /**
     * Change amount (can be negative for decrease)
     */
    private Integer changeAmount;

    /**
     * Reason for stock change
     * Examples: "RESTOCK", "SALE", "ADJUSTMENT", "DAMAGE", "RETURN"
     */
    private String reason;
}

