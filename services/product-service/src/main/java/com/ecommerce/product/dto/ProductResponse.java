package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Product Response DTO
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private String productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryId;
    private String sku;
    private String brand;
    private Integer stock;
    private Double weight;
    private String dimensions;
    private List<String> images;
    private Map<String, String> attributes;
    private List<String> tags;
    private Boolean active;
    private Boolean featured;
    private Double averageRating;
    private Integer reviewCount;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}

