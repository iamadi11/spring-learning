package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Create Order Request DTO
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Shipping fee is required")
    private BigDecimal shippingFee;

    private BigDecimal taxAmount;
    private BigDecimal discountAmount;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    @NotNull(message = "Shipping address is required")
    private String shippingAddress;

    private String billingAddress;
    private String notes;
}

