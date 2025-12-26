package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.50", message = "Minimum amount is $0.50")
    @DecimalMax(value = "10000.00", message = "Maximum amount is $10,000")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (USD, EUR, etc.)")
    private String currency;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Card details (in production: use tokenization!)
    private String cardNumber;
    private String cardHolderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cvv;

    // Metadata for fraud detection
    private String ipAddress;
    private String userAgent;
    private String deviceId;
}

