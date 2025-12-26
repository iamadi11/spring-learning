package com.ecommerce.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User Profile Entity
 * 
 * <p>Stores extended user profile information beyond authentication data.
 * Works in conjunction with User entity from Auth Service.</p>
 * 
 * <h2>Separation of Concerns:</h2>
 * <pre>
 * Auth Service - User Entity:
 * - Authentication data (username, password, roles)
 * - Security-related fields
 * - Access control
 * - JWT token generation
 * 
 * User Service - UserProfile Entity:
 * - Profile information (bio, avatar, phone)
 * - Personal details (date of birth, gender)
 * - Addresses
 * - Preferences
 * - Activity tracking
 * 
 * Why Separate?
 * 1. Single Responsibility Principle
 * 2. Independent scaling (auth vs profile queries)
 * 3. Different data access patterns
 * 4. Security isolation (sensitive auth data separate)
 * </pre>
 * 
 * <h2>CQRS in User Profile:</h2>
 * <pre>
 * Commands (Write Operations):
 * - Create profile
 * - Update profile
 * - Upload avatar
 * - Delete profile
 * → Routed to PRIMARY database
 * → @Transactional(readOnly = false)
 * 
 * Queries (Read Operations):
 * - Get profile by ID
 * - Search profiles
 * - Get profile with addresses
 * - List users (pagination)
 * → Routed to REPLICA database
 * → @Transactional(readOnly = true)
 * 
 * Example:
 * Command: userService.updateProfile(id, updates)
 *   → PRIMARY DB
 *   → Changes replicated to replica
 * 
 * Query: userService.findById(id)
 *   → REPLICA DB
 *   → Fast, no write contention
 * </pre>
 * 
 * <h2>Relationships:</h2>
 * <pre>
 * UserProfile ←1────*→ Address
 * - One user has many addresses
 * - Cascade: ALL (delete user → delete addresses)
 * - Fetch: LAZY (load addresses only when accessed)
 * - Orphan removal: true (remove address from list → delete from DB)
 * 
 * UserProfile ←1────1→ UserPreferences
 * - One user has one preferences object
 * - Cascade: ALL (delete user → delete preferences)
 * - Fetch: LAZY (load preferences only when needed)
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA entity
@Table(name = "user_profiles",  // Table name
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),  // Fast lookup by user ID
           @Index(name = "idx_email", columnList = "email"),  // Fast lookup by email
           @Index(name = "idx_phone", columnList = "phone_number")  // Fast lookup by phone
       })
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@Builder  // Lombok: builder pattern
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key - matches user ID from Auth Service
     * 
     * <p>This is the same ID as the User entity in Auth Service.
     * No auto-generation - ID comes from Auth Service.</p>
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * Email - duplicated from Auth Service for convenience
     * 
     * <p>Allows queries without calling Auth Service.
     * Kept in sync via events from Auth Service.</p>
     */
    @Email(message = "Email should be valid")
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * User biography/about me
     * 
     * <p>Free-text description written by user.</p>
     */
    @Column(length = 1000)
    private String bio;

    /**
     * Avatar image URL
     * 
     * <p>URL to profile picture (stored in CDN or object storage).</p>
     */
    @Column(length = 500)
    private String avatarUrl;

    /**
     * Phone number with country code
     * 
     * <p>Format: +1234567890</p>
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * Date of birth
     * 
     * <p>Used for age verification and birthday promotions.</p>
     */
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Gender
     * 
     * <p>Optional field for personalization.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    /**
     * Account verification status
     * 
     * <p>Phone and email verification tracking.</p>
     */
    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    /**
     * Profile completion percentage
     * 
     * <p>Calculated based on filled fields.
     * Used to prompt users to complete their profile.</p>
     */
    @Column(name = "profile_completion")
    private Integer profileCompletion = 0;

    /**
     * Last login timestamp
     * 
     * <p>Tracks user activity. Updated from Auth Service.</p>
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Account creation timestamp
     * 
     * <p>Automatically set when entity is persisted.</p>
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     * 
     * <p>Automatically updated when entity is modified.</p>
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Addresses (One-to-Many)
     * 
     * <p>List of shipping and billing addresses.</p>
     * 
     * <p><b>Cascade ALL:</b> Delete user → delete all addresses</p>
     * <p><b>Orphan Removal:</b> Remove address from list → delete from DB</p>
     * <p><b>Fetch LAZY:</b> Addresses loaded only when accessed</p>
     */
    @OneToMany(mappedBy = "userProfile", 
               cascade = CascadeType.ALL, 
               orphanRemoval = true, 
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    /**
     * User preferences (One-to-One)
     * 
     * <p>Settings like language, currency, notifications.</p>
     * 
     * <p><b>Cascade ALL:</b> Delete user → delete preferences</p>
     * <p><b>Fetch LAZY:</b> Preferences loaded only when accessed</p>
     */
    @OneToOne(mappedBy = "userProfile", 
              cascade = CascadeType.ALL, 
              orphanRemoval = true, 
              fetch = FetchType.LAZY)
    private UserPreferences preferences;

    /**
     * Helper method to add address
     * 
     * <p>Maintains bidirectional relationship.</p>
     */
    public void addAddress(Address address) {
        addresses.add(address);
        address.setUserProfile(this);
    }

    /**
     * Helper method to remove address
     * 
     * <p>Maintains bidirectional relationship.
     * Address will be deleted from database due to orphan removal.</p>
     */
    public void removeAddress(Address address) {
        addresses.remove(address);
        address.setUserProfile(null);
    }

    /**
     * Gender enum
     */
    public enum Gender {
        MALE,
        FEMALE,
        OTHER,
        PREFER_NOT_TO_SAY
    }
}

