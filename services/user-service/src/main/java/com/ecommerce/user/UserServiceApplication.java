package com.ecommerce.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * User Service Application
 * 
 * <p>User Profile Management Service implementing CQRS pattern with
 * PostgreSQL primary-replica replication for scalability.</p>
 * 
 * <h2>Service Responsibilities:</h2>
 * <ul>
 *   <li>User Profile Management (view, update, delete)</li>
 *   <li>User Address Management (shipping, billing addresses)</li>
 *   <li>User Preferences (notifications, language, currency)</li>
 *   <li>User Search and Filtering</li>
 *   <li>Avatar Upload and Management</li>
 * </ul>
 * 
 * <h2>CQRS Pattern Implementation:</h2>
 * <pre>
 * CQRS = Command Query Responsibility Segregation
 * 
 * Traditional Architecture:
 * ┌─────────────────┐
 * │    Service      │
 * ├─────────────────┤
 * │  Read & Write   │
 * └────────┬────────┘
 *          │
 *    ┌─────┴─────┐
 *    │ Database  │
 *    └───────────┘
 * 
 * CQRS Architecture:
 * ┌──────────────────────────────────┐
 * │         User Service             │
 * ├─────────────────┬────────────────┤
 * │  Command Side   │   Query Side   │
 * │  (Write Model)  │  (Read Model)  │
 * └────────┬────────┴────────┬───────┘
 *          │                  │
 *    ┌─────┴─────┐      ┌────┴────┐
 *    │  Primary  │──────>│ Replica │
 *    │ Database  │ Sync  │Database │
 *    └───────────┘       └─────────┘
 * 
 * Benefits:
 * 1. Scalability:
 *    - Write operations → Primary database
 *    - Read operations → Replica database(s)
 *    - Scale reads independently (add more replicas)
 *    - Typical ratio: 90% reads, 10% writes
 * 
 * 2. Performance:
 *    - Read models optimized for queries
 *    - Write models optimized for updates
 *    - No read/write contention
 *    - Can use different caching strategies
 * 
 * 3. Flexibility:
 *    - Different models for different use cases
 *    - Can denormalize read model
 *    - Easy to add projections
 *    - Independent evolution
 * </pre>
 * 
 * <h2>PostgreSQL Primary-Replica Replication:</h2>
 * <pre>
 * Replication Flow:
 * 
 * 1. Write Operation (Command):
 *    Client → Update Profile → Command Handler
 *    ↓
 *    Primary Database (Write)
 *    ↓
 *    Write-Ahead Log (WAL)
 *    ↓
 *    Streaming Replication
 *    ↓
 *    Replica Database (Sync)
 * 
 * 2. Read Operation (Query):
 *    Client → Get Profile → Query Handler
 *    ↓
 *    Replica Database (Read)
 *    ↓
 *    Response
 * 
 * Configuration:
 * - Primary: Read-Write
 * - Replica: Read-Only
 * - Replication: Streaming (real-time)
 * - Lag: <100ms (typically)
 * </pre>
 * 
 * <h2>Service Interactions:</h2>
 * <pre>
 * User Service interactions:
 * 
 * 1. Authentication:
 *    User Service → Auth Service (JWT validation)
 *    - Validate access token
 *    - Extract user ID
 *    - Check permissions
 * 
 * 2. Event Publishing:
 *    User Service → Kafka → [Other Services]
 *    Events:
 *    - UserCreatedEvent (when profile completed)
 *    - UserUpdatedEvent (profile changes)
 *    - UserDeletedEvent (account deletion)
 * 
 * 3. Service Discovery:
 *    User Service → Eureka Server
 *    - Register service instance
 *    - Health check updates
 *    - Dynamic discovery by other services
 * 
 * 4. Configuration:
 *    User Service → Config Server
 *    - Fetch database URLs
 *    - Get caching config
 *    - Load feature flags
 * </pre>
 * 
 * <h2>Caching Strategy:</h2>
 * <pre>
 * Cache Implementation:
 * 
 * 1. User Profile Cache:
 *    - TTL: 15 minutes
 *    - Invalidate on update
 *    - Key: user:{userId}
 *    - Store in Redis
 * 
 * 2. Address Cache:
 *    - TTL: 30 minutes
 *    - Invalidate on update
 *    - Key: user:{userId}:addresses
 * 
 * 3. Search Results Cache:
 *    - TTL: 5 minutes
 *    - Key: users:search:{query}
 *    - Paginated results
 * 
 * Cache Flow:
 * 1. Read request comes in
 * 2. Check Redis cache
 * 3. If hit: return cached data
 * 4. If miss: query database (replica)
 * 5. Store in cache
 * 6. Return data
 * 
 * Cache Invalidation:
 * - Write operation → Invalidate cache
 * - Update triggers cache delete
 * - Ensures consistency
 * </pre>
 * 
 * <h2>API Design:</h2>
 * <pre>
 * User Profile Endpoints:
 * GET    /api/users/me              - Get current user profile
 * PUT    /api/users/me              - Update current user profile
 * DELETE /api/users/me              - Delete current user account
 * POST   /api/users/me/avatar       - Upload avatar image
 * 
 * Address Management:
 * GET    /api/users/me/addresses              - Get all addresses
 * POST   /api/users/me/addresses              - Add new address
 * PUT    /api/users/me/addresses/{id}         - Update address
 * DELETE /api/users/me/addresses/{id}         - Delete address
 * PUT    /api/users/me/addresses/{id}/default - Set default address
 * 
 * Preferences:
 * GET    /api/users/me/preferences   - Get user preferences
 * PUT    /api/users/me/preferences   - Update preferences
 * 
 * Admin Endpoints:
 * GET    /api/admin/users            - Search users (paginated)
 * GET    /api/admin/users/{id}       - Get user by ID
 * PUT    /api/admin/users/{id}       - Update user
 * DELETE /api/admin/users/{id}       - Delete user
 * </pre>
 * 
 * <h2>Data Model:</h2>
 * <pre>
 * UserProfile:
 * - id (from Auth Service)
 * - bio
 * - avatarUrl
 * - phoneNumber
 * - dateOfBirth
 * - gender
 * - created/updated timestamps
 * 
 * Address:
 * - id
 * - userId
 * - type (SHIPPING, BILLING)
 * - fullName
 * - addressLine1, addressLine2
 * - city, state, postalCode, country
 * - phone
 * - isDefault
 * 
 * Preferences:
 * - userId
 * - language
 * - currency
 * - timezone
 * - notificationSettings (email, sms, push)
 * - theme (light, dark, auto)
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication  // Marks this as Spring Boot application
@EnableDiscoveryClient  // Register with Eureka for service discovery
@EnableCaching  // Enable Spring's caching abstraction (with Redis)
@EnableKafka  // Enable Kafka for event publishing
public class UserServiceApplication {

    /**
     * Main method - application entry point
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Start Spring Boot application
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

