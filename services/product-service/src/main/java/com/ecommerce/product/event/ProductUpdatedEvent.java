package com.ecommerce.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product Updated Event
 * 
 * <p>Event emitted when product details are updated.</p>
 * 
 * @author E-commerce Platform Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductUpdatedEvent extends BaseEvent {

    private String name;
    private String description;
    private BigDecimal price;
    private String categoryId;
    private String brand;
    private Double weight;
    private String dimensions;
    private List<String> images;
    private Map<String, String> attributes;
    private List<String> tags;
    private Boolean active;
    private Boolean featured;
}

