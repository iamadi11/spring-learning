package com.ecommerce.user.dto;

import com.ecommerce.user.entity.UserPreferences;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Preferences Request DTO
 * 
 * <p>Data Transfer Object for updating user preferences.
 * All fields optional for partial updates.</p>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferencesRequest {

    // Localization
    @Size(max = 10)
    private String language;

    @Size(max = 3)
    private String currency;

    @Size(max = 50)
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
}

