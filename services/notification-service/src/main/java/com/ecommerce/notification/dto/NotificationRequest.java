package com.ecommerce.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Notification Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotNull
    private Long userId;

    private String email;
    
    private String phone;
    
    private String deviceToken;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}

