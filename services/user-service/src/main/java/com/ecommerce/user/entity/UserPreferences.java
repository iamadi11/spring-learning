package com.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User Preferences Entity
 * 
 * <p>Stores user preferences and settings for personalization.</p>
 * 
 * <h2>Preference Categories:</h2>
 * <pre>
 * Localization:
 * - Language (en, es, fr, de, etc.)
 * - Currency (USD, EUR, GBP, etc.)
 * - Timezone (America/New_York, Europe/London, etc.)
 * 
 * Notifications:
 * - Email notifications (order updates, promotions)
 * - SMS notifications (shipping updates)
 * - Push notifications (mobile app)
 * 
 * Display:
 * - Theme (light, dark, auto)
 * - Items per page (pagination)
 * - Default view (grid, list)
 * 
 * Privacy:
 * - Show profile publicly
 * - Allow search indexing
 * - Show activity status
 * </pre>
 * 
 * <h2>Default Values:</h2>
 * <pre>
 * New User Preferences:
 * - Language: Browser language or "en"
 * - Currency: Based on IP geolocation
 * - Timezone: Browser timezone
 * - All notifications: Enabled
 * - Theme: Auto (follow system)
 * </pre>
 * 
 * <h2>Relationship with UserProfile:</h2>
 * <pre>
 * UserProfile ←1────1→ UserPreferences
 * 
 * - One user has one preferences object
 * - One preferences object belongs to one user
 * - Foreign key: user_id references user_profiles(user_id)
 * - Cascade: Deleting user deletes preferences
 * - Shared primary key strategy
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA entity
@Table(name = "user_preferences")  // Table name
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@Builder  // Lombok: builder pattern
public class UserPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - same as user ID
     * 
     * <p>Uses shared primary key with UserProfile.
     * user_preferences.user_id = user_profiles.user_id</p>
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    // ============================
    // Localization Settings
    // ============================

    /**
     * Preferred language - ISO 639-1 code
     * 
     * <p>Examples: en, es, fr, de, it, pt, ja, zh</p>
     * <p>Used for UI language and email templates.</p>
     */
    @Column(length = 10, nullable = false)
    @Builder.Default
    private String language = "en";

    /**
     * Preferred currency - ISO 4217 code
     * 
     * <p>Examples: USD, EUR, GBP, JPY, CNY</p>
     * <p>Used for product prices and payments.</p>
     */
    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    /**
     * Timezone - IANA timezone identifier
     * 
     * <p>Examples: America/New_York, Europe/London, Asia/Tokyo</p>
     * <p>Used for displaying dates and scheduling.</p>
     */
    @Column(length = 50, nullable = false)
    @Builder.Default
    private String timezone = "UTC";

    // ============================
    // Notification Settings
    // ============================

    /**
     * Email notifications enabled
     * 
     * <p>Includes order confirmations, shipping updates, promotions.</p>
     */
    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;

    /**
     * Order update emails
     * 
     * <p>Specific to order status changes (confirmed, shipped, delivered).</p>
     */
    @Column(name = "email_order_updates", nullable = false)
    @Builder.Default
    private Boolean emailOrderUpdates = true;

    /**
     * Promotional emails
     * 
     * <p>Marketing emails about sales, new products, etc.</p>
     */
    @Column(name = "email_promotions", nullable = false)
    @Builder.Default
    private Boolean emailPromotions = true;

    /**
     * Newsletter subscription
     * 
     * <p>Regular newsletter emails.</p>
     */
    @Column(name = "email_newsletter", nullable = false)
    @Builder.Default
    private Boolean emailNewsletter = false;

    /**
     * SMS notifications enabled
     * 
     * <p>Text message notifications for critical updates.</p>
     */
    @Column(name = "sms_notifications", nullable = false)
    @Builder.Default
    private Boolean smsNotifications = false;

    /**
     * SMS shipping updates
     * 
     * <p>Text messages for shipping status.</p>
     */
    @Column(name = "sms_shipping_updates", nullable = false)
    @Builder.Default
    private Boolean smsShippingUpdates = false;

    /**
     * Push notifications enabled (mobile app)
     * 
     * <p>In-app notifications and OS notifications.</p>
     */
    @Column(name = "push_notifications", nullable = false)
    @Builder.Default
    private Boolean pushNotifications = true;

    // ============================
    // Display Settings
    // ============================

    /**
     * UI theme preference
     * 
     * <p>LIGHT: Light theme</p>
     * <p>DARK: Dark theme</p>
     * <p>AUTO: Follow system preference</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    @Builder.Default
    private Theme theme = Theme.AUTO;

    /**
     * Items per page for pagination
     * 
     * <p>Default: 20 items per page.</p>
     */
    @Column(name = "items_per_page", nullable = false)
    @Builder.Default
    private Integer itemsPerPage = 20;

    /**
     * Default product view
     * 
     * <p>GRID: Grid layout with images</p>
     * <p>LIST: List layout with details</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_view", length = 10, nullable = false)
    @Builder.Default
    private ProductView productView = ProductView.GRID;

    // ============================
    // Privacy Settings
    // ============================

    /**
     * Show profile publicly
     * 
     * <p>If true, profile visible to other users.</p>
     * <p>If false, profile is private.</p>
     */
    @Column(name = "public_profile", nullable = false)
    @Builder.Default
    private Boolean publicProfile = true;

    /**
     * Allow search engine indexing
     * 
     * <p>If true, profile can appear in search results.</p>
     */
    @Column(name = "searchable_profile", nullable = false)
    @Builder.Default
    private Boolean searchableProfile = true;

    /**
     * Show online status
     * 
     * <p>If true, show when user is active/online.</p>
     */
    @Column(name = "show_online_status", nullable = false)
    @Builder.Default
    private Boolean showOnlineStatus = true;

    // ============================
    // Timestamps
    // ============================

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ============================
    // Relationship
    // ============================

    /**
     * One-to-one relationship with UserProfile
     * 
     * <p>Shared primary key - user_id.</p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId  // Maps user_id as both primary key and foreign key
    @JoinColumn(name = "user_id")
    private UserProfile userProfile;

    // ============================
    // Enums
    // ============================

    /**
     * Theme preference
     */
    public enum Theme {
        LIGHT,  // Light theme
        DARK,   // Dark theme
        AUTO    // Follow system preference
    }

    /**
     * Product view preference
     */
    public enum ProductView {
        GRID,  // Grid layout with images
        LIST   // List layout with details
    }
}

