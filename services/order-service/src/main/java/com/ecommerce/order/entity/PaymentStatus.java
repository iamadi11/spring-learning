package com.ecommerce.order.entity;

/**
 * Payment Status Enum
 * 
 * @author E-commerce Platform Team
 */
public enum PaymentStatus {
    PENDING,      // Payment not yet processed
    AUTHORIZED,   // Payment authorized (hold on funds)
    CAPTURED,     // Payment captured (funds transferred)
    FAILED,       // Payment failed
    REFUNDED      // Payment refunded
}

