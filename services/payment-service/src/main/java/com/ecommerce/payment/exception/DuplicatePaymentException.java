package com.ecommerce.payment.exception;

/**
 * Duplicate Payment Exception
 * 
 * <p>Payment already processed for this order (idempotency).</p>
 */
public class DuplicatePaymentException extends RuntimeException {
    
    public DuplicatePaymentException(String message) {
        super(message);
    }
}

