package com.ecommerce.user.service;

import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserProfileQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User Profile Query Service
 * 
 * <p>Handles all READ operations (Queries) for user profiles.
 * All methods route to REPLICA database.</p>
 * 
 * <h2>CQRS Query Service Responsibilities:</h2>
 * <pre>
 * Queries = Read-Only Operations:
 * 1. Find by ID - Single profile lookup
 * 2. Find by Email - Lookup by email
 * 3. Search - Search profiles with criteria
 * 4. List - Paginated listing
 * 5. Statistics - Aggregate queries
 * 
 * Key Characteristics:
 * - All operations use @Transactional(readOnly = true)
 * - Routes to REPLICA database
 * - Results cached in Redis
 * - No modifications allowed
 * - Returns entity or collection
 * </pre>
 * 
 * <h2>Transaction Management:</h2>
 * <pre>
 * @Transactional(readOnly = true):
 * - Opens read-only transaction
 * - Routes to REPLICA database
 * - Can only read data
 * - No commits needed (no changes)
 * - Optimized for read performance
 * 
 * Example Flow:
 * 1. Client calls getProfile(userId)
 * 2. @Transactional(readOnly = true) starts transaction
 * 3. RoutingDataSource detects readOnly = true
 * 4. Routes to REPLICA database
 * 5. Check Redis cache first
 * 6. If cache miss, query replica
 * 7. Store result in cache
 * 8. Return profile to client
 * 
 * Why Replica?
 * - Offload read traffic from primary
 * - Primary focused on writes
 * - Replica optimized for reads
 * - Can scale reads by adding more replicas
 * - Typical ratio: 90% reads, 10% writes
 * </pre>
 * 
 * <h2>Caching Strategy:</h2>
 * <pre>
 * Multi-Level Caching:
 * 
 * Level 1 - Redis Cache:
 * - Fast in-memory lookup
 * - Distributed (shared across instances)
 * - TTL: 15 minutes for profiles
 * - Key format: userProfiles::userId
 * 
 * Level 2 - Database Replica:
 * - If cache miss, query replica
 * - SSD storage, indexed queries
 * - Result stored back in Redis
 * 
 * Cache Hit Flow:
 * Request → Redis → HIT → Return (< 1ms)
 * 
 * Cache Miss Flow:
 * Request → Redis → MISS → Replica DB → Store in Redis → Return (< 50ms)
 * 
 * Cache Eviction:
 * - Automatic on updates (CommandService @CacheEvict)
 * - TTL expiration (15 min)
 * - Manual flush (admin operation)
 * 
 * Benefits:
 * - 99%+ cache hit rate (warm cache)
 * - Sub-millisecond response times
 * - Reduced database load
 * - Scalable to millions of requests
 * </pre>
 * 
 * <h2>N+1 Query Problem Prevention:</h2>
 * <pre>
 * Problem Without Join Fetch:
 * SELECT * FROM user_profiles;              // 1 query
 * For each profile (N profiles):
 *   SELECT * FROM addresses WHERE...;       // N queries
 *   SELECT * FROM preferences WHERE...;     // N queries
 * Total: 1 + N + N queries (2N+1 problem!)
 * 
 * Solution With Join Fetch:
 * SELECT u, a, p FROM user_profiles u
 * LEFT JOIN FETCH u.addresses a
 * LEFT JOIN FETCH u.preferences p;          // 1 query
 * Total: 1 query (solved!)
 * 
 * Usage:
 * - Use findById() for profile only
 * - Use findByIdWithAddresses() when need addresses
 * - Use findByIdWithAllRelations() for complete profile
 * - Avoid over-fetching (load only what's needed)
 * </pre>
 * 
 * <h2>Pagination Best Practices:</h2>
 * <pre>
 * Pagination Components:
 * - Page number (0-indexed)
 * - Page size (items per page)
 * - Sort criteria (field + direction)
 * 
 * Example Request:
 * GET /api/users?page=0&size=20&sort=createdAt,desc
 * 
 * Spring Data Page Object:
 * {
 *   "content": [...],              // Current page items
 *   "totalElements": 1000,         // Total count
 *   "totalPages": 50,              // Total pages
 *   "size": 20,                    // Page size
 *   "number": 0,                   // Current page (0-indexed)
 *   "first": true,                 // Is first page?
 *   "last": false,                 // Is last page?
 *   "numberOfElements": 20         // Items in current page
 * }
 * 
 * Performance:
 * - Database uses LIMIT and OFFSET
 * - Indexed columns for sorting
 * - Count query cached
 * - Cursor-based for very large datasets (future)
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service  // Spring service component
public class UserProfileQueryService {

    // Logger for debugging
    private static final Logger logger = LoggerFactory.getLogger(UserProfileQueryService.class);

    // Query repository for read operations
    private final UserProfileQueryRepository queryRepository;

    /**
     * Constructor with dependency injection
     * 
     * @param queryRepository Repository for read operations
     */
    @Autowired
    public UserProfileQueryService(UserProfileQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    /**
     * Get User Profile by ID
     * 
     * <p>Retrieves user profile by user ID.</p>
     * <p>Result cached in Redis with key "userProfiles::userId".</p>
     * 
     * <p><b>Cache Flow:</b></p>
     * <pre>
     * 1. Check Redis: userProfiles::123
     * 2. If HIT: Return from cache (< 1ms)
     * 3. If MISS: Query replica database
     * 4. Store result in Redis (TTL: 15 min)
     * 5. Return profile
     * </pre>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile if found
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "userProfiles", key = "#userId")  // Cache result
    public Optional<UserProfile> getProfile(Long userId) {
        logger.debug("Fetching profile for userId: {}", userId);
        
        // Query REPLICA database
        // @Transactional(readOnly = true) ensures routing to replica
        // @Cacheable checks Redis first
        Optional<UserProfile> profile = queryRepository.findById(userId);
        
        if (profile.isPresent()) {
            logger.debug("Profile found for userId: {}", userId);
        } else {
            logger.debug("Profile not found for userId: {}", userId);
        }
        
        return profile;
    }

    /**
     * Get Profile by Email
     * 
     * <p>Retrieves user profile by email address.</p>
     * 
     * @param email Email address
     * @return Optional containing UserProfile if found
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "userProfilesByEmail", key = "#email")  // Cache by email
    public Optional<UserProfile> getProfileByEmail(String email) {
        logger.debug("Fetching profile for email: {}", email);
        return queryRepository.findByEmail(email);
    }

    /**
     * Get Profile with Addresses
     * 
     * <p>Retrieves profile with addresses loaded (avoids N+1).</p>
     * <p>Use when you need address information along with profile.</p>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile with addresses
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "userProfilesWithAddresses", key = "#userId")
    public Optional<UserProfile> getProfileWithAddresses(Long userId) {
        logger.debug("Fetching profile with addresses for userId: {}", userId);
        
        // Single query with JOIN FETCH
        // Avoids N+1 problem
        return queryRepository.findByIdWithAddresses(userId);
    }

    /**
     * Get Profile with Preferences
     * 
     * <p>Retrieves profile with preferences loaded.</p>
     * 
     * @param userId User ID
     * @return Optional containing UserProfile with preferences
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "userProfilesWithPreferences", key = "#userId")
    public Optional<UserProfile> getProfileWithPreferences(Long userId) {
        logger.debug("Fetching profile with preferences for userId: {}", userId);
        return queryRepository.findByIdWithPreferences(userId);
    }

    /**
     * Get Complete Profile
     * 
     * <p>Retrieves profile with all relations (addresses + preferences).</p>
     * <p>Use when you need complete profile data in one call.</p>
     * 
     * @param userId User ID
     * @return Optional containing complete UserProfile
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "userProfilesComplete", key = "#userId")
    public Optional<UserProfile> getCompleteProfile(Long userId) {
        logger.debug("Fetching complete profile for userId: {}", userId);
        
        // Single query with multiple JOIN FETCH
        // Loads profile, addresses, and preferences in one query
        return queryRepository.findByIdWithAllRelations(userId);
    }

    /**
     * Search Profiles by Name
     * 
     * <p>Searches profiles by name (case-insensitive).</p>
     * <p>Results paginated for performance.</p>
     * 
     * @param name Name to search for
     * @param pageable Pagination parameters
     * @return Page of matching profiles
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "userSearchResults", key = "#name + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<UserProfile> searchProfiles(String name, Pageable pageable) {
        logger.debug("Searching profiles with name: {}, page: {}, size: {}", 
                name, pageable.getPageNumber(), pageable.getPageSize());
        
        // Query replica with LIKE search and pagination
        return queryRepository.searchByName(name, pageable);
    }

    /**
     * Find Profiles by Verification Status
     * 
     * <p>Finds profiles filtered by email verification status.</p>
     * 
     * @param emailVerified Email verification status
     * @param pageable Pagination parameters
     * @return Page of profiles
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    public Page<UserProfile> getProfilesByVerificationStatus(Boolean emailVerified, Pageable pageable) {
        logger.debug("Fetching profiles with emailVerified: {}", emailVerified);
        return queryRepository.findByEmailVerified(emailVerified, pageable);
    }

    /**
     * Find Incomplete Profiles
     * 
     * <p>Finds profiles with completion below threshold.</p>
     * <p>Useful for sending "complete your profile" prompts.</p>
     * 
     * @param completionThreshold Completion percentage threshold
     * @param pageable Pagination parameters
     * @return Page of incomplete profiles
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    public Page<UserProfile> getIncompleteProfiles(Integer completionThreshold, Pageable pageable) {
        logger.debug("Fetching incomplete profiles (threshold: {})", completionThreshold);
        return queryRepository.findIncompleteProfiles(completionThreshold, pageable);
    }

    /**
     * Check if Email Exists
     * 
     * <p>Fast existence check without loading entire profile.</p>
     * <p>Used during registration validation.</p>
     * 
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    public boolean emailExists(String email) {
        logger.debug("Checking if email exists: {}", email);
        
        // Fast existence check (SELECT COUNT)
        // More efficient than loading entire entity
        return queryRepository.existsByEmail(email);
    }

    /**
     * Check if Phone Exists
     * 
     * <p>Fast existence check for phone number.</p>
     * 
     * @param phoneNumber Phone number to check
     * @return true if phone exists, false otherwise
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    public boolean phoneExists(String phoneNumber) {
        logger.debug("Checking if phone exists: {}", phoneNumber);
        return queryRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * Count Verified Profiles
     * 
     * <p>Counts profiles with email verified.</p>
     * <p>Used for analytics and reporting.</p>
     * 
     * @return Count of verified profiles
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "profileCounts", key = "'verified'")
    public long countVerifiedProfiles() {
        logger.debug("Counting verified profiles");
        return queryRepository.countByEmailVerified(true);
    }

    /**
     * Get Profile Completion Statistics
     * 
     * <p>Retrieves aggregate statistics on profile completion.</p>
     * <p>Returns completion percentage and count for each percentage.</p>
     * 
     * @return List of [completion%, count] pairs
     */
    @Transactional(readOnly = true)  // Routes to REPLICA
    @Cacheable(value = "profileStatistics", key = "'completion'")
    public List<Object[]> getCompletionStatistics() {
        logger.debug("Fetching profile completion statistics");
        
        // Aggregate query: GROUP BY completion percentage
        // Returns: [[100, 500], [80, 300], [60, 200], ...]
        return queryRepository.getCompletionStatistics();
    }
}

