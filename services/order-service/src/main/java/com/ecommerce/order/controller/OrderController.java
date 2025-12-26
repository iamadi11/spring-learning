package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Order Controller
 * 
 * <p>REST API for order management.</p>
 * 
 * @author E-commerce Platform Team
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Order operations with Saga pattern")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create Order
     */
    @PostMapping
    @Operation(summary = "Create order", description = "Creates a new order using Saga pattern")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId = Long.parseLong(authentication.getName());
        logger.info("Creating order for user: {}", userId);

        Order order = orderService.createOrder(request, userId);
        OrderResponse response = toResponse(order);

        ApiResponse<OrderResponse> apiResponse = ApiResponse.success(
            "Order created successfully",
            response
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Get Order by ID
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Retrieves order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable String orderId,
            HttpServletRequest httpRequest) {

        logger.debug("Fetching order: {}", orderId);

        Order order = orderService.getOrder(orderId);
        OrderResponse response = toResponse(order);

        ApiResponse<OrderResponse> apiResponse = ApiResponse.success(
            "Order retrieved successfully",
            response
        );

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get My Orders
     */
    @GetMapping("/my")
    @Operation(summary = "Get my orders", description = "Retrieves orders for current user")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        logger.debug("Fetching orders for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.getUserOrders(userId, pageable);
        Page<OrderResponse> response = orders.map(this::toResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancel Order
     */
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String orderId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId = Long.parseLong(authentication.getName());
        logger.info("Cancelling order: {} by user: {}", orderId, userId);

        Order order = orderService.cancelOrder(orderId, userId);
        OrderResponse response = toResponse(order);

        ApiResponse<OrderResponse> apiResponse = ApiResponse.success(
            "Order cancelled successfully",
            response
        );

        return ResponseEntity.ok(apiResponse);
    }

    // ============= Helper Methods =============

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .orderNumber(order.getOrderNumber())
            .userId(order.getUserId())
            .status(order.getStatus())
            .paymentStatus(order.getPaymentStatus())
            .items(order.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList()))
            .totalAmount(order.getTotalAmount())
            .shippingFee(order.getShippingFee())
            .taxAmount(order.getTaxAmount())
            .discountAmount(order.getDiscountAmount())
            .finalAmount(order.getFinalAmount())
            .paymentMethod(order.getPaymentMethod())
            .paymentTransactionId(order.getPaymentTransactionId())
            .shippingAddress(order.getShippingAddress())
            .billingAddress(order.getBillingAddress())
            .notes(order.getNotes())
            .trackingNumber(order.getTrackingNumber())
            .carrier(order.getCarrier())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .cancelledAt(order.getCancelledAt())
            .shippedAt(order.getShippedAt())
            .deliveredAt(order.getDeliveredAt())
            .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
            .id(item.getId())
            .productId(item.getProductId())
            .productName(item.getProductName())
            .sku(item.getSku())
            .price(item.getPrice())
            .quantity(item.getQuantity())
            .subtotal(item.getSubtotal())
            .productImageUrl(item.getProductImageUrl())
            .build();
    }
}

