package com.ecommerce.user.repository;

import com.ecommerce.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * User Profile Command Repository
 * 
 * <p>Repository for WRITE operations (Commands) on UserProfile.
 * All methods in this repository route to the PRIMARY database.</p>
 * 
 * <h2>CQRS Command Side:</h2>
 * <pre>
 * Commands = Write Operations:
 * - Create (save new entity)
 * - Update (modify existing entity)
 * - Delete (remove entity)
 * 
 * Characteristics:
 * - Routed to PRIMARY database
 * - Can modify data
 * - Returns void or entity
 * - Used with @Transactional(readOnly = false) - DEFAULT
 * 
 * Examples:
 * userProfileCommandRepository.save(profile);        // INSERT or UPDATE
 * userProfileCommandRepository.deleteById(id);       // DELETE
 * userProfileCommandRepository.updateBio(id, bio);   // UPDATE
 * </pre>
 * 
 * <h2>Why Separate Command Repository?</h2>
 * <pre>
 * Benefits:
 * 1. Clear Intent:
 *    - CommandRepository → Write operations
 *    - QueryRepository → Read operations
 *    - Immediately clear what operation does
 * 
 * 2. Routing:
 *    - Commands always go to primary
 *    - Queries always go to replica
 *    - No mixed reads/writes in one transaction
 * 
 * 3. Testing:
 *    - Can mock CommandRepository for write tests
 *    - Can mock QueryRepository for read tests
 *    - Clear separation in test setup
 * 
 * 4. Monitoring:
 *    - Track command vs query metrics separately
 *    - Monitor write load on primary
 *    - Monitor read load on replica
 * </pre>
 * 
 * <h2>Usage in Service:</h2>
 * <pre>
 * @Service
 * public class UserProfileService {
 *     
 *     @Autowired
 *     private UserProfileCommandRepository commandRepo;
 *     
 *     @Autowired
 *     private UserProfileQueryRepository queryRepo;
 *     
 *     // COMMAND: Create profile
 *     @Transactional(readOnly = false)  // Routes to PRIMARY
 *     public UserProfile createProfile(UserProfile profile) {
 *         return commandRepo.save(profile);  // Write to primary
 *     }
 *     
 *     // QUERY: Get profile
 *     @Transactional(readOnly = true)  // Routes to REPLICA
 *     public UserProfile getProfile(Long userId) {
 *         return queryRepo.findById(userId)  // Read from replica
 *             .orElseThrow();
 *     }
 * }
 * </pre>
 * 
 * <h2>Transaction Routing:</h2>
 * <pre>
 * Service Method:
 * @Transactional(readOnly = false)  // Mark as write transaction
 * public void updateProfile(...) {
 *     commandRepo.save(profile);    // Routes to primary
 * }
 * 
 * Flow:
 * 1. Service method called
 * 2. @Transactional starts transaction
 * 3. Read-only flag set to false
 * 4. commandRepo.save() called
 * 5. JPA requests connection
 * 6. RoutingDataSource.determineCurrentLookupKey() called
 * 7. Checks TransactionSynchronizationManager.isCurrentTransactionReadOnly()
 * 8. Returns "primary" (because readOnly = false)
 * 9. Connection acquired from primary datasource
 * 10. Query executes on primary database
 * 11. Changes replicated to replica
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository  // Spring Data repository
public interface UserProfileCommandRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Save UserProfile
     * 
     * <p>Inherited from JpaRepository. Performs INSERT or UPDATE.</p>
     * <p>Routes to PRIMARY database.</p>
     * 
     * @param profile UserProfile to save
     * @return Saved UserProfile with generated ID
     */
    // save(UserProfile profile) - inherited

    /**
     * Delete UserProfile by ID
     * 
     * <p>Inherited from JpaRepository. Performs DELETE.</p>
     * <p>Routes to PRIMARY database.</p>
     * <p>Cascades to delete addresses and preferences.</p>
     * 
     * @param userId User ID
     */
    // deleteById(Long userId) - inherited

    /**
     * Update Bio
     * 
     * <p>Custom update query for updating just the bio field.</p>
     * <p>More efficient than loading entire entity and saving.</p>
     * 
     * <p><b>@Modifying:</b> Indicates this is a modifying query (UPDATE/DELETE)</p>
     * <p><b>@Query:</b> Custom JPQL query</p>
     * 
     * @param userId User ID
     * @param bio New bio text
     * @return Number of rows updated
     */
    @Modifying  // Required for UPDATE/DELETE queries
    @Query("UPDATE UserProfile u SET u.bio = :bio WHERE u.userId = :userId")
    int updateBio(@Param("userId") Long userId, @Param("bio") String bio);

    /**
     * Update Avatar URL
     * 
     * <p>Custom update for avatar URL only.</p>
     * 
     * @param userId User ID
     * @param avatarUrl New avatar URL
     * @return Number of rows updated
     */
    @Modifying
    @Query("UPDATE UserProfile u SET u.avatarUrl = :avatarUrl WHERE u.userId = :userId")
    int updateAvatarUrl(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

    /**
     * Update Phone Number
     * 
     * <p>Custom update for phone number and verification status.</p>
     * 
     * @param userId User ID
     * @param phoneNumber New phone number
     * @param phoneVerified Verification status
     * @return Number of rows updated
     */
    @Modifying
    @Query("UPDATE UserProfile u SET u.phoneNumber = :phoneNumber, u.phoneVerified = :phoneVerified WHERE u.userId = :userId")
    int updatePhoneNumber(
        @Param("userId") Long userId, 
        @Param("phoneNumber") String phoneNumber,
        @Param("phoneVerified") Boolean phoneVerified
    );

    /**
     * Update Profile Completion
     * 
     * <p>Updates the profile completion percentage.</p>
     * 
     * @param userId User ID
     * @param completion Completion percentage (0-100)
     * @return Number of rows updated
     */
    @Modifying
    @Query("UPDATE UserProfile u SET u.profileCompletion = :completion WHERE u.userId = :userId")
    int updateProfileCompletion(@Param("userId") Long userId, @Param("completion") Integer completion);

    /**
     * Delete by User ID
     * 
     * <p>Custom delete query (alternative to deleteById).</p>
     * <p>Cascades to delete related entities.</p>
     * 
     * @param userId User ID
     * @return Number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM UserProfile u WHERE u.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}

