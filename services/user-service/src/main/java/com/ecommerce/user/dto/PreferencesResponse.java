package com.ecommerce.user.dto;

import com.ecommerce.user.entity.UserPreferences;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Preferences Response DTO
 * 
 * <p>Data Transfer Object for returning user preferences in API responses.</p>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferencesResponse {

    // Localization
    private String language;
    private String currency;
    private String timezone;

    // Email Notifications
    private Boolean emailNotifications;
    private Boolean emailOrderUpdates;
    private Boolean emailPromotions;
    private Boolean emailNewsletter;

    // SMS Notifications
    private Boolean smsNotifications;
    private Boolean smsShippingUpdates;

    // Push Notifications
    private Boolean pushNotifications;

    // Display Settings
    private UserPreferences.Theme theme;
    private Integer itemsPerPage;
    private UserPreferences.ProductView productView;

    // Privacy Settings
    private Boolean publicProfile;
    private Boolean searchableProfile;
    private Boolean showOnlineStatus;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

