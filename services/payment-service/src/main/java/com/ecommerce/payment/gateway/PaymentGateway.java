package com.ecommerce.payment.gateway;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.RefundRequest;
import com.ecommerce.payment.dto.RefundResponse;

/**
 * Payment Gateway Interface
 * 
 * <p>Strategy pattern for supporting multiple payment gateways.</p>
 * 
 * <h2>Why Strategy Pattern?</h2>
 * <pre>
 * 1. Multiple Gateways:
 *    - Stripe (primary)
 *    - PayPal (secondary)
 *    - Custom gateway (backup)
 * 
 * 2. Easy Switching:
 *    - Change gateway without code changes
 *    - Configuration-based selection
 * 
 * 3. Failover:
 *    - Primary down → try secondary
 *    - Automatic fallback
 * 
 * 4. Testing:
 *    - Mock implementation
 *    - Integration tests
 * 
 * Usage:
 * PaymentGateway gateway = gatewayFactory.getGateway("stripe");
 * PaymentResponse response = gateway.processPayment(request);
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
public interface PaymentGateway {

    /**
     * Process payment (authorize + capture)
     * 
     * @param request Payment details
     * @return Payment response
     * @throws PaymentProcessingException if payment fails
     */
    PaymentResponse processPayment(PaymentRequest request);

    /**
     * Authorize payment (reserve funds)
     * 
     * @param request Payment details
     * @return Authorization response
     */
    PaymentResponse authorizePayment(PaymentRequest request);

    /**
     * Capture payment (transfer funds)
     * 
     * @param transactionId Previous authorization ID
     * @param amount Amount to capture
     * @return Capture response
     */
    PaymentResponse capturePayment(String transactionId, java.math.BigDecimal amount);

    /**
     * Refund payment
     * 
     * @param request Refund details
     * @return Refund response
     */
    RefundResponse refundPayment(RefundRequest request);

    /**
     * Get gateway name
     * 
     * @return Gateway identifier (stripe, paypal, etc.)
     */
    String getGatewayName();

    /**
     * Check if gateway is available
     * 
     * @return true if operational
     */
    boolean isAvailable();
}

