package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Order Item Response DTO
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private String productId;
    private String productName;
    private String sku;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private String productImageUrl;
}

