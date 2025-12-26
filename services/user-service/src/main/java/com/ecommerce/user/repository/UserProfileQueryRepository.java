package com.ecommerce.user.repository;

import com.ecommerce.user.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Profile Query Repository
 * 
 * <p>Repository for READ operations (Queries) on UserProfile.
 * All methods in this repository route to the REPLICA database.</p>
 * 
 * <h2>CQRS Query Side:</h2>
 * <pre>
 * Queries = Read Operations:
 * - Find by ID
 * - Find by email
 * - Search profiles
 * - List with pagination
 * - Count
 * 
 * Characteristics:
 * - Routed to REPLICA database
 * - Read-only (no modifications)
 * - Returns entity or collection
 * - Used with @Transactional(readOnly = true)
 * 
 * Examples:
 * queryRepo.findById(id);                    // SELECT by ID
 * queryRepo.findByEmail(email);              // SELECT by email
 * queryRepo.searchProfiles(query, pageable); // SELECT with WHERE
 * </pre>
 * 
 * <h2>Caching Strategy:</h2>
 * <pre>
 * Query results are cached in Redis:
 * 
 * 1. findById(userId):
 *    Cache key: user:profile:{userId}
 *    TTL: 15 minutes
 *    Eviction: On update/delete
 * 
 * 2. findByEmail(email):
 *    Cache key: user:profile:email:{email}
 *    TTL: 15 minutes
 *    Eviction: On email change
 * 
 * 3. searchProfiles(query):
 *    Cache key: user:search:{query}:{page}
 *    TTL: 5 minutes
 *    Eviction: On any profile update
 * 
 * Cache Flow:
 * 1. Check Redis cache
 * 2. If hit: Return cached data
 * 3. If miss: Query replica database
 * 4. Store result in cache
 * 5. Return data
 * </pre>
 * 
 * <h2>Join Fetch Optimization:</h2>
 * <pre>
 * N+1 Problem:
 * Without join fetch:
 *   SELECT * FROM user_profiles;           // 1 query
 *   For each profile:
 *     SELECT * FROM addresses WHERE...;    // N queries
 *   Total: 1 + N queries (N+1 problem)
 * 
 * With join fetch:
 *   SELECT u, a FROM user_profiles u
 *   LEFT JOIN FETCH u.addresses a;         // 1 query
 *   Total: 1 query (solved!)
 * 
 * Usage:
 * @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.addresses WHERE u.userId = :id")
 * Optional<UserProfile> findByIdWithAddresses(@Param("id") Long id);
 * </pre>
 * 
 * <h2>Pagination:</h2>
 * <pre>
 * Spring Data Pagination:
 * 
 * // Create pageable
 * Pageable pageable = PageRequest.of(
 *     0,                               // Page number (0-indexed)
 *     20,                              // Page size
 *     Sort.by("createdAt").descending() // Sort
 * );
 * 
 * // Query with pagination
 * Page<UserProfile> page = queryRepo.findAll(pageable);
 * 
 * // Page info
 * page.getTotalElements();  // Total count
 * page.getTotalPages();     // Total pages
 * page.getContent();        // Current page data
 * page.hasNext();           // Has next page?
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository  // Spring Data repository
public interface UserProfileQueryRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Find by ID
     * 
     * <p>Inherited from JpaRepository.</p>
     * <p>Routes to REPLICA database.</p>
     * <p>Cached in Redis.</p>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile if found
     */
    // findById(Long userId) - inherited

    /**
     * Find by Email
     * 
     * <p>Find user profile by email address.</p>
     * <p>Routes to REPLICA database.</p>
     * 
     * @param email Email address
     * @return Optional containing UserProfile if found
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * Find by ID with Addresses (Join Fetch)
     * 
     * <p>Loads profile with addresses in single query (avoids N+1).</p>
     * <p>LEFT JOIN FETCH loads addresses eagerly.</p>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile with addresses
     */
    @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.addresses WHERE u.userId = :userId")
    Optional<UserProfile> findByIdWithAddresses(@Param("userId") Long userId);

    /**
     * Find by ID with Preferences (Join Fetch)
     * 
     * <p>Loads profile with preferences in single query.</p>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile with preferences
     */
    @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.preferences WHERE u.userId = :userId")
    Optional<UserProfile> findByIdWithPreferences(@Param("userId") Long userId);

    /**
     * Find by ID with All Relations (Join Fetch)
     * 
     * <p>Loads profile with addresses and preferences in single query.</p>
     * <p>Use when you need complete profile data.</p>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile with all relations
     */
    @Query("SELECT DISTINCT u FROM UserProfile u " +
           "LEFT JOIN FETCH u.addresses " +
           "LEFT JOIN FETCH u.preferences " +
           "WHERE u.userId = :userId")
    Optional<UserProfile> findByIdWithAllRelations(@Param("userId") Long userId);

    /**
     * Find by Phone Number
     * 
     * <p>Find user profile by phone number.</p>
     * 
     * @param phoneNumber Phone number
     * @return Optional containing UserProfile if found
     */
    Optional<UserProfile> findByPhoneNumber(String phoneNumber);

    /**
     * Search Profiles by Name
     * 
     * <p>Search by first name or last name (case-insensitive).</p>
     * <p>Uses LIKE with wildcards.</p>
     * 
     * @param name Name to search for
     * @param pageable Pagination info
     * @return Page of matching UserProfiles
     */
    @Query("SELECT u FROM UserProfile u " +
           "WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<UserProfile> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find by Email Verified Status
     * 
     * <p>Find all profiles with given email verification status.</p>
     * 
     * @param emailVerified Email verification status
     * @param pageable Pagination info
     * @return Page of matching UserProfiles
     */
    Page<UserProfile> findByEmailVerified(Boolean emailVerified, Pageable pageable);

    /**
     * Find by Phone Verified Status
     * 
     * <p>Find all profiles with given phone verification status.</p>
     * 
     * @param phoneVerified Phone verification status
     * @param pageable Pagination info
     * @return Page of matching UserProfiles
     */
    Page<UserProfile> findByPhoneVerified(Boolean phoneVerified, Pageable pageable);

    /**
     * Find Incomplete Profiles
     * 
     * <p>Find profiles with completion percentage below threshold.</p>
     * <p>Useful for prompting users to complete their profiles.</p>
     * 
     * @param completionThreshold Completion percentage threshold
     * @param pageable Pagination info
     * @return Page of incomplete UserProfiles
     */
    @Query("SELECT u FROM UserProfile u WHERE u.profileCompletion < :threshold")
    Page<UserProfile> findIncompleteProfiles(
        @Param("threshold") Integer completionThreshold, 
        Pageable pageable
    );

    /**
     * Count by Email Verified
     * 
     * <p>Count profiles with given email verification status.</p>
     * 
     * @param emailVerified Email verification status
     * @return Count of matching profiles
     */
    long countByEmailVerified(Boolean emailVerified);

    /**
     * Check if Email Exists
     * 
     * <p>Fast check if email is already registered.</p>
     * 
     * @param email Email address
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if Phone Exists
     * 
     * <p>Fast check if phone number is already registered.</p>
     * 
     * @param phoneNumber Phone number
     * @return true if phone exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Get Profile Completion Statistics
     * 
     * <p>Aggregate query for completion statistics.</p>
     * 
     * @return List of completion statistics [completion%, count]
     */
    @Query("SELECT u.profileCompletion, COUNT(u) FROM UserProfile u " +
           "GROUP BY u.profileCompletion " +
           "ORDER BY u.profileCompletion DESC")
    List<Object[]> getCompletionStatistics();
}

