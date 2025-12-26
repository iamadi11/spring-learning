package com.ecommerce.payment.entity;

/**
 * Payment Status Enum
 * 
 * <p>Represents the lifecycle states of a payment transaction.</p>
 * 
 * @author E-commerce Platform Team
 */
public enum PaymentStatus {
    /**
     * PENDING - Payment initiated but not processed
     */
    PENDING,
    
    /**
     * AUTHORIZED - Funds reserved (hold placed on card)
     */
    AUTHORIZED,
    
    /**
     * CAPTURED - Funds transferred to merchant
     */
    CAPTURED,
    
    /**
     * SETTLED - Funds deposited in merchant account
     */
    SETTLED,
    
    /**
     * FAILED - Payment processing failed
     */
    FAILED,
    
    /**
     * DECLINED - Payment declined by bank/gateway
     */
    DECLINED,
    
    /**
     * REFUNDED - Money returned to customer
     */
    REFUNDED,
    
    /**
     * PARTIALLY_REFUNDED - Part of payment refunded
     */
    PARTIALLY_REFUNDED,
    
    /**
     * CANCELLED - Payment cancelled before capture
     */
    CANCELLED
}

