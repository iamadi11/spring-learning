package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Response DTO
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private String orderId;
    private String orderNumber;
    private Long userId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    
    private List<OrderItemResponse> items;
    
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    
    private String paymentMethod;
    private String paymentTransactionId;
    
    private String shippingAddress;
    private String billingAddress;
    private String notes;
    
    private String trackingNumber;
    private String carrier;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
}

