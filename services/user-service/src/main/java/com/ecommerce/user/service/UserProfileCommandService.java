package com.ecommerce.user.service;

import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserProfileCommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Profile Command Service
 * 
 * <p>Handles all WRITE operations (Commands) for user profiles.
 * All methods route to PRIMARY database.</p>
 * 
 * <h2>CQRS Command Service Responsibilities:</h2>
 * <pre>
 * Commands = State-Changing Operations:
 * 1. Create - Add new user profile
 * 2. Update - Modify existing profile
 * 3. Delete - Remove profile
 * 4. Partial Updates - Update specific fields
 * 
 * Key Characteristics:
 * - All operations use @Transactional(readOnly = false) - DEFAULT
 * - Routes to PRIMARY database
 * - Evicts cache after modifications
 * - Publishes events to Kafka (future implementation)
 * - Returns void or modified entity
 * </pre>
 * 
 * <h2>Transaction Management:</h2>
 * <pre>
 * @Transactional Behavior:
 * 
 * Default (readOnly = false):
 * - Opens write transaction
 * - Routes to primary database
 * - Can read and write
 * - Commits on success, rolls back on exception
 * 
 * Example Flow:
 * 1. Client calls updateProfile()
 * 2. @Transactional starts transaction (readOnly = false)
 * 3. RoutingDataSource routes to PRIMARY
 * 4. Service loads entity from primary
 * 5. Service modifies entity
 * 6. Service saves entity
 * 7. Changes committed to primary
 * 8. Primary replicates to replica
 * 9. Cache evicted
 * 10. Success response returned
 * 
 * On Exception:
 * 1. Exception thrown in service
 * 2. Transaction rolled back (changes not committed)
 * 3. Cache not evicted (no changes made)
 * 4. Exception propagated to controller
 * 5. Error response returned
 * </pre>
 * 
 * <h2>Cache Invalidation:</h2>
 * <pre>
 * Why Evict Cache?
 * - After update, cache contains stale data
 * - Must invalidate cache to force fresh read
 * - Next query will fetch from database (replica)
 * - Result stored in cache for future reads
 * 
 * Cache Eviction Flow:
 * 1. updateProfile(userId, updates)
 * 2. Update entity in PRIMARY database
 * 3. @CacheEvict removes cache entry
 * 4. Cache key: user:profile:{userId}
 * 5. Next getProfile(userId) misses cache
 * 6. Queries REPLICA database
 * 7. Stores result in cache
 * 
 * Multiple Cache Keys:
 * @Caching(evict = {
 *     @CacheEvict(value = "userProfiles", key = "#userId"),
 *     @CacheEvict(value = "usersByEmail", key = "#email")
 * })
 * - Evicts multiple cache entries
 * - Ensures all cached representations cleared
 * </pre>
 * 
 * <h2>Event Publishing (Future):</h2>
 * <pre>
 * After Command Execution:
 * 1. Command completes successfully
 * 2. Publish event to Kafka:
 *    - UserCreatedEvent
 *    - UserUpdatedEvent
 *    - UserDeletedEvent
 * 3. Other services consume events:
 *    - Order Service: Update user info in orders
 *    - Notification Service: Send welcome email
 *    - Analytics Service: Track user activity
 * 
 * Event-Driven Benefits:
 * - Loose coupling between services
 * - Async processing
 * - Event sourcing for audit trail
 * - Easy to add new consumers
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service  // Spring service component
public class UserProfileCommandService {

    // Logger for debugging and audit trail
    private static final Logger logger = LoggerFactory.getLogger(UserProfileCommandService.class);

    // Command repository for write operations
    private final UserProfileCommandRepository commandRepository;

    /**
     * Constructor with dependency injection
     * 
     * @param commandRepository Repository for write operations
     */
    @Autowired
    public UserProfileCommandService(UserProfileCommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    /**
     * Create User Profile
     * 
     * <p>Creates a new user profile after successful registration in Auth Service.</p>
     * 
     * <p><b>Flow:</b></p>
     * <pre>
     * 1. Auth Service creates user account
     * 2. Auth Service publishes UserRegisteredEvent
     * 3. User Service consumes event
     * 4. User Service calls createProfile()
     * 5. Profile created in PRIMARY database
     * 6. UserCreatedEvent published
     * </pre>
     * 
     * <p><b>Validation:</b></p>
     * <ul>
     *   <li>User ID must not already exist</li>
     *   <li>Email must be unique</li>
     * </ul>
     * 
     * @param profile UserProfile to create (with userId from Auth Service)
     * @return Created UserProfile
     * @throws RuntimeException if profile already exists
     */
    @Transactional  // readOnly = false (default) - routes to PRIMARY
    public UserProfile createProfile(UserProfile profile) {
        // Log creation attempt
        logger.info("Creating user profile for userId: {}", profile.getUserId());
        
        // Validate: Check if profile already exists
        if (commandRepository.existsById(profile.getUserId())) {
            logger.error("Profile already exists for userId: {}", profile.getUserId());
            throw new RuntimeException("User profile already exists");
        }
        
        // Set initial values
        profile.setPhoneVerified(false);
        profile.setProfileCompletion(calculateProfileCompletion(profile));
        
        // Save to PRIMARY database
        // @Transactional ensures this is atomic
        // If exception occurs, transaction is rolled back
        UserProfile savedProfile = commandRepository.save(profile);
        
        // Log success
        logger.info("User profile created successfully for userId: {}", savedProfile.getUserId());
        
        // TODO: Publish UserCreatedEvent to Kafka
        // kafkaTemplate.send("user-events", new UserCreatedEvent(savedProfile));
        
        return savedProfile;
    }

    /**
     * Update User Profile
     * 
     * <p>Updates user profile information.</p>
     * 
     * <p><b>Cache Eviction:</b></p>
     * <ul>
     *   <li>@CacheEvict removes cached profile</li>
     *   <li>Next read will fetch fresh data from replica</li>
     *   <li>Cache key: "userProfiles::userId"</li>
     * </ul>
     * 
     * @param userId User ID
     * @param updates UserProfile with updated fields
     * @return Updated UserProfile
     * @throws RuntimeException if profile not found
     */
    @Transactional  // Routes to PRIMARY
    @CacheEvict(value = "userProfiles", key = "#userId")  // Evict cache after update
    public UserProfile updateProfile(Long userId, UserProfile updates) {
        // Log update attempt
        logger.info("Updating user profile for userId: {}", userId);
        
        // Load existing profile from PRIMARY database
        // Even though this is a read, we're in a write transaction
        // So it routes to PRIMARY (ensures read-your-writes consistency)
        UserProfile existingProfile = commandRepository.findById(userId)
            .orElseThrow(() -> {
                logger.error("Profile not found for userId: {}", userId);
                return new RuntimeException("User profile not found");
            });
        
        // Update fields (only non-null values from updates)
        if (updates.getBio() != null) {
            existingProfile.setBio(updates.getBio());
        }
        if (updates.getPhoneNumber() != null) {
            existingProfile.setPhoneNumber(updates.getPhoneNumber());
        }
        if (updates.getDateOfBirth() != null) {
            existingProfile.setDateOfBirth(updates.getDateOfBirth());
        }
        if (updates.getGender() != null) {
            existingProfile.setGender(updates.getGender());
        }
        
        // Recalculate profile completion
        existingProfile.setProfileCompletion(calculateProfileCompletion(existingProfile));
        
        // Save to PRIMARY database
        // updatedAt timestamp automatically updated by @UpdateTimestamp
        UserProfile savedProfile = commandRepository.save(existingProfile);
        
        // Log success
        logger.info("User profile updated successfully for userId: {}", userId);
        
        // Cache eviction happens automatically due to @CacheEvict
        
        // TODO: Publish UserUpdatedEvent to Kafka
        // kafkaTemplate.send("user-events", new UserUpdatedEvent(savedProfile));
        
        return savedProfile;
    }

    /**
     * Update Bio
     * 
     * <p>Efficient update of just the bio field using custom query.</p>
     * <p>Doesn't load entire entity - more efficient for single field update.</p>
     * 
     * @param userId User ID
     * @param bio New bio text
     * @throws RuntimeException if update fails
     */
    @Transactional  // Routes to PRIMARY
    @CacheEvict(value = "userProfiles", key = "#userId")  // Evict cache
    public void updateBio(Long userId, String bio) {
        logger.info("Updating bio for userId: {}", userId);
        
        // Execute custom update query
        // More efficient than loading entire entity
        int rowsUpdated = commandRepository.updateBio(userId, bio);
        
        // Verify update succeeded
        if (rowsUpdated == 0) {
            logger.error("Failed to update bio for userId: {} - profile not found", userId);
            throw new RuntimeException("User profile not found");
        }
        
        logger.info("Bio updated successfully for userId: {}", userId);
    }

    /**
     * Update Avatar URL
     * 
     * <p>Updates user's avatar/profile picture URL.</p>
     * 
     * @param userId User ID
     * @param avatarUrl New avatar URL (from CDN/S3)
     * @throws RuntimeException if update fails
     */
    @Transactional  // Routes to PRIMARY
    @CacheEvict(value = "userProfiles", key = "#userId")  // Evict cache
    public void updateAvatarUrl(Long userId, String avatarUrl) {
        logger.info("Updating avatar for userId: {}", userId);
        
        int rowsUpdated = commandRepository.updateAvatarUrl(userId, avatarUrl);
        
        if (rowsUpdated == 0) {
            logger.error("Failed to update avatar for userId: {} - profile not found", userId);
            throw new RuntimeException("User profile not found");
        }
        
        logger.info("Avatar updated successfully for userId: {}", userId);
    }

    /**
     * Update Phone Number
     * 
     * <p>Updates phone number and verification status.</p>
     * 
     * @param userId User ID
     * @param phoneNumber New phone number
     * @param phoneVerified Verification status
     * @throws RuntimeException if update fails
     */
    @Transactional  // Routes to PRIMARY
    @CacheEvict(value = "userProfiles", key = "#userId")  // Evict cache
    public void updatePhoneNumber(Long userId, String phoneNumber, boolean phoneVerified) {
        logger.info("Updating phone number for userId: {}", userId);
        
        int rowsUpdated = commandRepository.updatePhoneNumber(userId, phoneNumber, phoneVerified);
        
        if (rowsUpdated == 0) {
            logger.error("Failed to update phone for userId: {} - profile not found", userId);
            throw new RuntimeException("User profile not found");
        }
        
        logger.info("Phone number updated successfully for userId: {}", userId);
    }

    /**
     * Delete User Profile
     * 
     * <p>Soft or hard delete of user profile.</p>
     * <p>Cascades to delete addresses and preferences.</p>
     * 
     * @param userId User ID
     * @throws RuntimeException if profile not found
     */
    @Transactional  // Routes to PRIMARY
    @CacheEvict(value = "userProfiles", key = "#userId")  // Evict cache
    public void deleteProfile(Long userId) {
        logger.info("Deleting user profile for userId: {}", userId);
        
        // Verify profile exists
        if (!commandRepository.existsById(userId)) {
            logger.error("Cannot delete - profile not found for userId: {}", userId);
            throw new RuntimeException("User profile not found");
        }
        
        // Delete from PRIMARY database
        // Cascades to delete addresses and preferences (CascadeType.ALL)
        commandRepository.deleteById(userId);
        
        logger.info("User profile deleted successfully for userId: {}", userId);
        
        // TODO: Publish UserDeletedEvent to Kafka
        // kafkaTemplate.send("user-events", new UserDeletedEvent(userId));
    }

    /**
     * Calculate Profile Completion Percentage
     * 
     * <p>Calculates how complete the profile is based on filled fields.</p>
     * <p>Used to prompt users to complete their profile.</p>
     * 
     * <p><b>Scoring:</b></p>
     * <ul>
     *   <li>Email: 10% (always present)</li>
     *   <li>Bio: 15%</li>
     *   <li>Avatar: 15%</li>
     *   <li>Phone (verified): 20%</li>
     *   <li>Date of Birth: 15%</li>
     *   <li>Gender: 10%</li>
     *   <li>At least one address: 15%</li>
     * </ul>
     * 
     * @param profile UserProfile to calculate completion for
     * @return Completion percentage (0-100)
     */
    private int calculateProfileCompletion(UserProfile profile) {
        int completion = 0;
        
        // Email always present (10%)
        if (profile.getEmail() != null) {
            completion += 10;
        }
        
        // Bio (15%)
        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            completion += 15;
        }
        
        // Avatar (15%)
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
            completion += 15;
        }
        
        // Phone verified (20%)
        if (profile.getPhoneNumber() != null && profile.getPhoneVerified()) {
            completion += 20;
        }
        
        // Date of birth (15%)
        if (profile.getDateOfBirth() != null) {
            completion += 15;
        }
        
        // Gender (10%)
        if (profile.getGender() != null) {
            completion += 10;
        }
        
        // At least one address (15%)
        if (profile.getAddresses() != null && !profile.getAddresses().isEmpty()) {
            completion += 15;
        }
        
        return completion;
    }
}

