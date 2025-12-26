package com.ecommerce.user.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSource Configuration for CQRS Pattern
 * 
 * <p>Configures multiple datasources for Command (write) and Query (read) operations.
 * Implements PostgreSQL primary-replica replication strategy.</p>
 * 
 * <h2>CQRS Database Routing:</h2>
 * <pre>
 * Traditional Single Database:
 * ┌──────────────────┐
 * │    Service       │
 * └────────┬─────────┘
 *          │ All Operations
 *     ┌────┴────┐
 *     │Database │
 *     └─────────┘
 * 
 * CQRS with Primary-Replica:
 * ┌──────────────────────────────┐
 * │       User Service           │
 * ├──────────────┬───────────────┤
 * │   Commands   │    Queries    │
 * │   (Writes)   │    (Reads)    │
 * └──────┬───────┴───────┬───────┘
 *        │               │
 *   ┌────┴────┐     ┌────┴────┐
 *   │ Primary │────>│ Replica │
 *   │   DB    │Sync │   DB    │
 *   └─────────┘     └─────────┘
 * 
 * Routing Logic:
 * @Transactional(readOnly = false) → Primary DB (write)
 * @Transactional(readOnly = true)  → Replica DB (read)
 * </pre>
 * 
 * <h2>Why Primary-Replica Replication?</h2>
 * <pre>
 * Benefits:
 * 1. Scalability:
 *    - Add multiple read replicas
 *    - Scale reads independently
 *    - Typical workload: 90% reads, 10% writes
 * 
 * 2. Performance:
 *    - Write operations don't block reads
 *    - Read queries distributed across replicas
 *    - Lower latency for read operations
 * 
 * 3. High Availability:
 *    - Read operations continue if primary fails
 *    - Replica can be promoted to primary
 *    - Disaster recovery
 * 
 * Trade-offs:
 * 1. Eventual Consistency:
 *    - Small replication lag (typically <100ms)
 *    - Read-after-write may see stale data
 *    - Need to handle in application logic
 * 
 * 2. Complexity:
 *    - More infrastructure to manage
 *    - Monitoring replication lag
 *    - Failover procedures
 * 
 * 3. Cost:
 *    - Multiple database instances
 *    - Network bandwidth for replication
 * </pre>
 * 
 * <h2>PostgreSQL Streaming Replication:</h2>
 * <pre>
 * Setup:
 * 1. Primary Database:
 *    - Accepts read and write operations
 *    - Generates Write-Ahead Log (WAL)
 *    - Streams WAL to replicas
 * 
 * 2. Replica Database:
 *    - Receives WAL stream
 *    - Applies changes continuously
 *    - Read-only mode
 *    - Can lag behind primary (typically <100ms)
 * 
 * Configuration (postgresql.conf):
 * # Primary
 * wal_level = replica
 * max_wal_senders = 3
 * max_replication_slots = 3
 * 
 * # Replica
 * hot_standby = on
 * </pre>
 * 
 * <h2>Routing Implementation:</h2>
 * <pre>
 * How Routing Works:
 * 
 * 1. Request arrives:
 *    GET /api/users/me
 * 
 * 2. Controller method:
 *    @GetMapping("/me")
 *    @Transactional(readOnly = true)  // Marks as read operation
 *    public UserProfile getProfile() { ... }
 * 
 * 3. Spring detects readOnly = true
 * 
 * 4. RoutingDataSource routes to replica:
 *    if (readOnly) {
 *        return replicaDataSource;
 *    } else {
 *        return primaryDataSource;
 *    }
 * 
 * 5. Query executes on replica database
 * 
 * 6. Response returned
 * 
 * Write Operation:
 * PUT /api/users/me
 * @Transactional(readOnly = false)  // Default, marks as write
 * → Routes to primary database
 * → Primary replicates to replica
 * </pre>
 * 
 * <h2>Handling Replication Lag:</h2>
 * <pre>
 * Problem:
 * 1. User updates profile (writes to primary)
 * 2. Immediately reads profile (reads from replica)
 * 3. Replica hasn't synced yet (lag)
 * 4. User sees old data (stale read)
 * 
 * Solutions:
 * 
 * 1. Read from Primary After Write:
 *    @Transactional(readOnly = false)  // Force primary read
 *    public UserProfile getProfileAfterUpdate() { ... }
 * 
 * 2. Client-Side Handling:
 *    - Client caches updated data
 *    - Use cached data for immediate display
 *    - Refresh from server after delay
 * 
 * 3. Session Stickiness:
 *    - After write, stick to primary for N seconds
 *    - Use ThreadLocal to track recent writes
 * 
 * 4. Version Numbers:
 *    - Include version in response
 *    - Client sends version in next read
 *    - Server ensures replica has that version
 * 
 * 5. Eventual Consistency UI:
 *    - Show "Saving..." indicator
 *    - Show "Syncing..." if lag detected
 *    - Update UI when data available
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration  // Marks this as configuration class
public class DataSourceConfig {

    /**
     * Primary DataSource (Write Operations)
     * 
     * <p>Configured for write operations. All create, update, delete
     * operations are routed to this datasource.</p>
     * 
     * <p><b>Properties from application.yml:</b></p>
     * <pre>
     * spring:
     *   datasource:
     *     primary:
     *       jdbc-url: jdbc:postgresql://primary-host:5432/user_db
     *       username: postgres
     *       password: password
     *       driver-class-name: org.postgresql.Driver
     * </pre>
     * 
     * @return Primary datasource for writes
     */
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        // Build datasource from properties
        // spring.datasource.primary.* properties automatically mapped
        return DataSourceBuilder.create().build();
    }

    /**
     * Replica DataSource (Read Operations)
     * 
     * <p>Configured for read operations. All select queries with
     * @Transactional(readOnly = true) are routed to this datasource.</p>
     * 
     * <p><b>Properties from application.yml:</b></p>
     * <pre>
     * spring:
     *   datasource:
     *     replica:
     *       jdbc-url: jdbc:postgresql://replica-host:5432/user_db
     *       username: postgres
     *       password: password
     *       driver-class-name: org.postgresql.Driver
     * </pre>
     * 
     * <p><b>Note:</b> In development, this can point to the same database.
     * In production, this should point to a read replica.</p>
     * 
     * @return Replica datasource for reads
     */
    @Bean(name = "replicaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.replica")
    public DataSource replicaDataSource() {
        // Build datasource from properties
        // spring.datasource.replica.* properties automatically mapped
        return DataSourceBuilder.create().build();
    }

    /**
     * Routing DataSource
     * 
     * <p>Custom datasource that routes queries to primary or replica
     * based on transaction read-only flag.</p>
     * 
     * <p><b>Routing Logic:</b></p>
     * <ul>
     *   <li>@Transactional(readOnly = false) → primaryDataSource</li>
     *   <li>@Transactional(readOnly = true) → replicaDataSource</li>
     *   <li>No @Transactional → primaryDataSource (default)</li>
     * </ul>
     * 
     * @param primaryDataSource Primary datasource for writes
     * @param replicaDataSource Replica datasource for reads
     * @return Routing datasource
     */
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource) {
        
        // Create custom routing datasource
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        // Create map of datasources
        // Key: datasource identifier (used in routing logic)
        // Value: actual datasource
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("primary", primaryDataSource);  // Write operations
        dataSourceMap.put("replica", replicaDataSource);  // Read operations
        
        // Set target datasources
        routingDataSource.setTargetDataSources(dataSourceMap);
        
        // Set default datasource (used when no routing key)
        // Default to primary for safety (writes always go to primary)
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        
        return routingDataSource;
    }

    /**
     * Actual DataSource Bean (Primary & Main)
     * 
     * <p>This is the datasource used by Spring Data JPA.
     * It's wrapped in LazyConnectionDataSourceProxy to ensure
     * routing logic is applied when connection is actually requested,
     * not when transaction starts.</p>
     * 
     * <p><b>Why LazyConnectionDataSourceProxy?</b></p>
     * <pre>
     * Without Lazy Proxy:
     * 1. Transaction starts
     * 2. Connection acquired from datasource (routing happens here)
     * 3. @Transactional annotation processed
     * 4. Read-only flag set
     * 
     * Problem: Connection already acquired before read-only flag set!
     * Result: Wrong datasource used
     * 
     * With Lazy Proxy:
     * 1. Transaction starts
     * 2. @Transactional annotation processed
     * 3. Read-only flag set
     * 4. First query executes
     * 5. Connection acquired from datasource (routing happens here)
     * 
     * Result: Correct datasource based on read-only flag!
     * </pre>
     * 
     * @param routingDataSource Routing datasource
     * @return Lazy proxy wrapping routing datasource
     */
    @Bean
    @Primary  // This is the default datasource for Spring Data JPA
    public DataSource dataSource(
            @Qualifier("routingDataSource") DataSource routingDataSource) {
        
        // Wrap routing datasource in lazy proxy
        // This ensures connection is acquired AFTER transaction is configured
        // So read-only flag is set before routing happens
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}

