package com.ecommerce.payment.exception;

/**
 * Payment Processing Exception
 * 
 * <p>General payment processing failure.
 * Can be transient (retry) or permanent (don't retry).</p>
 */
public class PaymentProcessingException extends RuntimeException {
    
    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

