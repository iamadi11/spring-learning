package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment Repository
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Find by order ID (Idempotency check)
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Check if order already paid
     */
    boolean existsByOrderId(String orderId);

    /**
     * Find user payments
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find by status
     */
    Page<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    /**
     * Find failed payments for analysis
     */
    List<Payment> findByStatusAndCreatedAtAfter(PaymentStatus status, LocalDateTime after);

    /**
     * Calculate total amount by user (fraud detection)
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.userId = ?1 AND p.status = 'CAPTURED' " +
           "AND p.createdAt >= ?2")
    BigDecimal sumAmountByUserAndDateAfter(Long userId, LocalDateTime after);

    /**
     * Count failed attempts by user (fraud detection)
     */
    long countByUserIdAndStatusAndCreatedAtAfter(
        Long userId, PaymentStatus status, LocalDateTime after);
}

