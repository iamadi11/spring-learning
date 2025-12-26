package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Order Repository
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * Find by order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find orders by user
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find orders by status
     */
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /**
     * Find orders by user and status
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(
        Long userId, OrderStatus status, Pageable pageable);

    /**
     * Find pending orders older than threshold (for auto-cancellation)
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < ?1")
    List<Order> findPendingOrdersOlderThan(LocalDateTime threshold);

    /**
     * Count orders by user
     */
    long countByUserId(Long userId);

    /**
     * Check if order number exists
     */
    boolean existsByOrderNumber(String orderNumber);
}

