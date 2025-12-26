package com.ecommerce.payment.entity;

/**
 * Transaction Type Enum
 * 
 * @author E-commerce Platform Team
 */
public enum TransactionType {
    AUTHORIZATION,  // Reserve funds
    CAPTURE,        // Transfer funds
    REFUND,         // Return funds
    VOID            // Cancel authorization
}

