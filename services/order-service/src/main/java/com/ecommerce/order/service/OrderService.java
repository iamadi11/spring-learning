package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaOrchestrator;
import com.ecommerce.order.saga.createorder.CreateOrderContext;
import com.ecommerce.order.saga.createorder.CreateOrderSaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Service
 * 
 * <p>Business logic for order management.
 * Orchestrates saga execution for order creation.</p>
 * 
 * @author E-commerce Platform Team
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final SagaOrchestrator sagaOrchestrator;
    private final CreateOrderSaga createOrderSaga;

    @Autowired
    public OrderService(
            OrderRepository orderRepository,
            SagaOrchestrator sagaOrchestrator,
            CreateOrderSaga createOrderSaga) {
        this.orderRepository = orderRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.createOrderSaga = createOrderSaga;
    }

    /**
     * Create Order
     * 
     * <p>Creates an order using the Saga pattern for distributed transaction.</p>
     * 
     * @param request Order request
     * @param userId User placing the order
     * @return Created order
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request, Long userId) {
        logger.info("Creating order for user: {}", userId);

        // Generate order ID
        String orderId = UUID.randomUUID().toString();

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Build order entity
        Order order = Order.builder()
            .orderId(orderId)
            .userId(userId)
            .orderNumber(orderNumber)
            .status(OrderStatus.PENDING)
            .paymentStatus(PaymentStatus.PENDING)
            .shippingFee(request.getShippingFee())
            .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
            .discountAmount(request.getDiscountAmount())
            .paymentMethod(request.getPaymentMethod())
            .shippingAddress(request.getShippingAddress())
            .billingAddress(request.getBillingAddress())
            .notes(request.getNotes())
            .build();

        // Add order items
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                .productId(itemRequest.getProductId())
                .productName(itemRequest.getProductName())
                .sku(itemRequest.getSku())
                .price(itemRequest.getPrice())
                .quantity(itemRequest.getQuantity())
                .productImageUrl(itemRequest.getProductImageUrl())
                .build();
            
            item.calculateSubtotal();
            order.addItem(item);
        }

        // Calculate totals
        order.calculateTotals();

        // Update order status to IN_PROGRESS
        order.setStatus(OrderStatus.IN_PROGRESS);

        // Create saga context
        CreateOrderContext context = CreateOrderContext.builder()
            .order(order)
            .userId(userId)
            .build();

        // Execute saga
        SagaExecution execution = sagaOrchestrator.execute(createOrderSaga, context, orderId);

        // Update saga ID in context
        context.setSagaId(execution.getSagaId());

        // Check saga result
        if (execution.getStatus() == SagaStatus.COMPLETED) {
            logger.info("Order created successfully: {}", orderId);
            return context.getOrder();
        } else {
            logger.error("Order creation failed: {}. Status: {}", orderId, execution.getStatus());
            throw new RuntimeException("Order creation failed: " + execution.getErrorMessage());
        }
    }

    /**
     * Get Order by ID
     * 
     * @param orderId Order ID
     * @return Order
     */
    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    /**
     * Get Order by Order Number
     * 
     * @param orderNumber Order number
     * @return Order
     */
    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }

    /**
     * Get User Orders
     * 
     * @param userId User ID
     * @param pageable Pagination
     * @return Page of orders
     */
    @Transactional(readOnly = true)
    public Page<Order> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get Orders by Status
     * 
     * @param status Order status
     * @param pageable Pagination
     * @return Page of orders
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * Cancel Order
     * 
     * <p>Cancels an order if it's in a cancellable state.</p>
     * 
     * @param orderId Order ID
     * @param userId User ID (for authorization)
     * @return Updated order
     */
    @Transactional
    public Order cancelOrder(String orderId, Long userId) {
        logger.info("Cancelling order: {} by user: {}", orderId, userId);

        Order order = getOrder(orderId);

        // Verify user owns the order
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Order does not belong to user");
        }

        // Check if cancellable
        if (!order.isCancellable()) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        // TODO: Implement CancelOrderSaga
        // - Release inventory
        // - Refund payment
        // - Update order status

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * Generate Order Number
     * 
     * <p>Generates a unique order number in format: ORD-YYYYMMDD-NNNN</p>
     * 
     * @return Order number
     */
    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        String orderNumber = "ORD-" + datePrefix + "-" + randomSuffix;

        // Ensure uniqueness
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
            orderNumber = "ORD-" + datePrefix + "-" + randomSuffix;
        }

        return orderNumber;
    }
}

