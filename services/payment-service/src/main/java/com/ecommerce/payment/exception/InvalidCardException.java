package com.ecommerce.payment.exception;

/**
 * Invalid Card Exception
 * 
 * <p>Card details are invalid.
 * PERMANENT failure - do NOT retry.</p>
 */
public class InvalidCardException extends RuntimeException {
    
    public InvalidCardException(String message) {
        super(message);
    }
}

