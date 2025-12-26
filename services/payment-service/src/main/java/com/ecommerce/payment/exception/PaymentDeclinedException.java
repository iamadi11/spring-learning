package com.ecommerce.payment.exception;

import lombok.Getter;

/**
 * Payment Declined Exception
 * 
 * <p>Payment declined by bank/gateway.
 * This is a PERMANENT failure - should NOT retry.</p>
 * 
 * <h2>Common Decline Reasons:</h2>
 * <ul>
 *   <li>insufficient_funds</li>
 *   <li>invalid_card</li>
 *   <li>expired_card</li>
 *   <li>incorrect_cvc</li>
 *   <li>card_declined (generic)</li>
 * </ul>
 */
@Getter
public class PaymentDeclinedException extends RuntimeException {
    
    private final String declineCode;
    private final String responseCode;
    
    public PaymentDeclinedException(String message, String declineCode, String responseCode) {
        super(message);
        this.declineCode = declineCode;
        this.responseCode = responseCode;
    }
}

