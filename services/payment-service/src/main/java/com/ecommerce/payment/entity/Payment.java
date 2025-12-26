package com.ecommerce.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Entity
 * 
 * <p>Represents a payment transaction in the system.
 * Includes comprehensive tracking for resilience, idempotency, and audit.</p>
 * 
 * <h2>Why Track Everything?</h2>
 * <pre>
 * 1. Idempotency:
 *    - orderId as idempotency key
 *    - Prevent duplicate charges
 *    - Safe retries
 * 
 * 2. Audit Trail:
 *    - Who paid what, when
 *    - Gateway used
 *    - Response codes
 *    - Troubleshooting
 * 
 * 3. Reconciliation:
 *    - Match with gateway records
 *    - Identify discrepancies
 *    - Financial reporting
 * 
 * 4. Fraud Detection:
 *    - Track failed attempts
 *    - IP addresses
 *    - Pattern analysis
 * 
 * 5. Customer Service:
 *    - Transaction details
 *    - Refund information
 *    - Dispute resolution
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /**
     * Order ID - Idempotency Key
     * 
     * <p>CRITICAL for preventing duplicate charges.
     * Same orderId = same payment, even if retried.</p>
     */
    @Column(name = "order_id", nullable = false, unique = true, length = 36)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    /**
     * Gateway Used
     * 
     * <p>Which payment gateway processed this: STRIPE, PAYPAL, etc.</p>
     */
    @Column(name = "gateway", length = 50)
    private String gateway;

    /**
     * Gateway Transaction ID
     * 
     * <p>ID from payment gateway for reconciliation.</p>
     */
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    /**
     * Gateway Response
     * 
     * <p>Full response from gateway (JSON).
     * For debugging and audit.</p>
     */
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    /**
     * Gateway Response Code
     * 
     * <p>Success/error code from gateway.</p>
     */
    @Column(name = "gateway_response_code", length = 50)
    private String gatewayResponseCode;

    /**
     * Failure Reason
     * 
     * <p>Why payment failed: insufficient_funds, invalid_card, etc.</p>
     */
    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    /**
     * Retry Count
     * 
     * <p>How many times this payment was retried.
     * For monitoring retry patterns.</p>
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Refund Amount
     * 
     * <p>Total amount refunded (can be partial).</p>
     */
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /**
     * Refunded At
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /**
     * Metadata
     * 
     * <p>Additional info: IP address, user agent, device, etc.
     * For fraud detection.</p>
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return status == PaymentStatus.CAPTURED || status == PaymentStatus.SETTLED;
    }

    /**
     * Check if payment can be refunded
     */
    public boolean isRefundable() {
        return (status == PaymentStatus.CAPTURED || status == PaymentStatus.SETTLED) &&
               (refundAmount == null || refundAmount.compareTo(amount) < 0);
    }

    /**
     * Increment retry count
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}

