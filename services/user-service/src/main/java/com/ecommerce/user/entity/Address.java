package com.ecommerce.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Address Entity
 * 
 * <p>Stores shipping and billing addresses for users.
 * Users can have multiple addresses with one marked as default.</p>
 * 
 * <h2>Address Types:</h2>
 * <pre>
 * SHIPPING:
 * - Where orders are delivered
 * - Can have multiple shipping addresses
 * - User selects during checkout
 * 
 * BILLING:
 * - Associated with payment methods
 * - Used for invoices
 * - May differ from shipping address
 * 
 * BOTH:
 * - Same address for shipping and billing
 * - Most common case
 * </pre>
 * 
 * <h2>Default Address Logic:</h2>
 * <pre>
 * - User can set one default shipping address
 * - User can set one default billing address
 * - Setting new default clears previous default
 * 
 * Example:
 * User has 3 addresses:
 * 1. Home (default shipping, default billing)
 * 2. Office (shipping only)
 * 3. Parent's house (shipping only)
 * 
 * User sets Office as default shipping:
 * → Home loses default shipping flag
 * → Home keeps default billing flag
 * → Office becomes default shipping
 * </pre>
 * 
 * <h2>Relationship with UserProfile:</h2>
 * <pre>
 * UserProfile ←1────*→ Address
 * 
 * - One user can have many addresses
 * - One address belongs to one user
 * - Foreign key: user_id references user_profiles(user_id)
 * - Cascade: Deleting user deletes all addresses
 * - Orphan removal: Removing address from list deletes it
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA entity
@Table(name = "addresses",  // Table name
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),  // Fast lookup by user
           @Index(name = "idx_default", columnList = "is_default")  // Fast lookup for defaults
       })
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@Builder  // Lombok: builder pattern
public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - auto-generated
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Address type - SHIPPING, BILLING, or BOTH
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType type;

    /**
     * Full name for this address
     * 
     * <p>May differ from user's name (e.g., "John's Office", "Company Receiving").</p>
     */
    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Phone number for delivery contact
     */
    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Address line 1 - street address
     */
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200)
    @Column(name = "address_line1", nullable = false, length = 200)
    private String addressLine1;

    /**
     * Address line 2 - apartment, suite, unit (optional)
     */
    @Size(max = 200)
    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    /**
     * City
     */
    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    /**
     * State or province
     */
    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String state;

    /**
     * Postal code or ZIP code
     */
    @NotBlank(message = "Postal code is required")
    @Size(max = 20)
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    /**
     * Country
     */
    @NotBlank(message = "Country is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String country;

    /**
     * Default address flag
     * 
     * <p>Only one address per type should be default.</p>
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

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

    /**
     * Many-to-one relationship with UserProfile
     * 
     * <p>Each address belongs to one user.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile userProfile;

    /**
     * Address Type enum
     */
    public enum AddressType {
        SHIPPING,   // Shipping address only
        BILLING,    // Billing address only
        BOTH        // Both shipping and billing
    }

    /**
     * Get formatted address string
     * 
     * <p>Returns address in standard format for display.</p>
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isEmpty()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}

