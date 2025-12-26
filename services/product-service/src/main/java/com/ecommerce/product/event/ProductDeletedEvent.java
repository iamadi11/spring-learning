package com.ecommerce.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Product Deleted Event
 * 
 * <p>Event emitted when product is deleted (soft delete).</p>
 * 
 * @author E-commerce Platform Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductDeletedEvent extends BaseEvent {

    /**
     * Reason for deletion
     */
    private String reason;

    /**
     * Is this a soft delete (mark inactive) or hard delete?
     */
    private Boolean softDelete;
}

