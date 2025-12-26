package com.ecommerce.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routing DataSource for CQRS
 * 
 * <p>Custom datasource that routes database connections to either primary
 * or replica based on the transaction's read-only flag.</p>
 * 
 * <h2>How AbstractRoutingDataSource Works:</h2>
 * <pre>
 * Spring's AbstractRoutingDataSource:
 * - Abstract class for datasource routing
 * - Maintains map of datasources (primary, replica, etc.)
 * - Calls determineCurrentLookupKey() to get routing key
 * - Returns corresponding datasource from map
 * 
 * Our Implementation:
 * 1. Override determineCurrentLookupKey()
 * 2. Check TransactionSynchronizationManager.isCurrentTransactionReadOnly()
 * 3. Return "primary" for writes, "replica" for reads
 * 4. Spring uses returned key to lookup datasource
 * </pre>
 * 
 * <h2>Routing Flow:</h2>
 * <pre>
 * Example - Read Operation:
 * 
 * 1. Controller method:
 *    @GetMapping("/me")
 *    @Transactional(readOnly = true)
 *    public UserProfile getProfile() {
 *        return userService.findById(userId);
 *    }
 * 
 * 2. Spring starts transaction:
 *    - Creates transaction
 *    - Sets read-only flag to true
 *    - Stores in TransactionSynchronizationManager
 * 
 * 3. Service executes query:
 *    userRepository.findById(userId);
 * 
 * 4. JPA needs database connection:
 *    dataSource.getConnection();
 * 
 * 5. LazyConnectionDataSourceProxy defers to routing datasource:
 *    routingDataSource.getConnection();
 * 
 * 6. AbstractRoutingDataSource calls our method:
 *    String key = determineCurrentLookupKey();
 * 
 * 7. Our implementation checks read-only:
 *    boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
 *    if (isReadOnly) {
 *        return "replica";  // Return replica key
 *    } else {
 *        return "primary";  // Return primary key
 *    }
 * 
 * 8. AbstractRoutingDataSource looks up datasource:
 *    DataSource ds = targetDataSources.get("replica");
 * 
 * 9. Connection acquired from replica database
 * 
 * 10. Query executes on replica
 * 
 * 11. Results returned
 * 
 * Example - Write Operation:
 * 
 * 1. Controller method:
 *    @PutMapping("/me")
 *    @Transactional  // readOnly = false (default)
 *    public UserProfile updateProfile(...) {
 *        return userService.update(userId, updates);
 *    }
 * 
 * 2. Spring starts transaction with readOnly = false
 * 
 * 3. determineCurrentLookupKey() returns "primary"
 * 
 * 4. Query executes on primary database
 * 
 * 5. Changes replicated to replica
 * </pre>
 * 
 * <h2>TransactionSynchronizationManager:</h2>
 * <pre>
 * What is it?
 * - Spring Framework class for transaction metadata
 * - Uses ThreadLocal storage (thread-safe)
 * - Stores transaction state per thread
 * 
 * Key Methods:
 * - isActualTransactionActive() → Is transaction active?
 * - isCurrentTransactionReadOnly() → Is transaction read-only?
 * - getCurrentTransactionName() → Transaction name
 * - getCurrentTransactionIsolationLevel() → Isolation level
 * 
 * ThreadLocal Storage:
 * - Each thread has its own transaction state
 * - No interference between concurrent requests
 * - Automatically cleaned up after request
 * 
 * Example ThreadLocal Values:
 * Thread-1: transactionActive=true, readOnly=true  (read operation)
 * Thread-2: transactionActive=true, readOnly=false (write operation)
 * Thread-3: transactionActive=false               (no transaction)
 * </pre>
 * 
 * <h2>Edge Cases Handled:</h2>
 * <pre>
 * 1. No Transaction:
 *    - isActualTransactionActive() returns false
 *    - Route to primary (safe default)
 *    - Prevents accidental replica writes
 * 
 * 2. Nested Transactions:
 *    - Uses outermost transaction's read-only flag
 *    - Inner transaction inherits routing
 *    - Cannot mix read and write in same transaction
 * 
 * 3. Transaction Propagation:
 *    @Transactional(propagation = REQUIRES_NEW)
 *    - Creates new transaction
 *    - New routing decision made
 *    - Independent of parent transaction
 * 
 * 4. Mixed Operations:
 *    - One transaction = one datasource
 *    - Cannot read from replica and write to primary in same transaction
 *    - Must use separate transactions
 * </pre>
 * 
 * <h2>Monitoring and Logging:</h2>
 * <pre>
 * Logging Strategy:
 * - DEBUG level: Log every routing decision
 * - INFO level: Log datasource selection summary
 * 
 * Metrics to Track:
 * - Primary connection pool utilization
 * - Replica connection pool utilization
 * - Routing errors (should be rare)
 * - Read/write ratio
 * 
 * Common Issues:
 * 1. High primary load:
 *    - Check if reads going to primary
 *    - Verify @Transactional(readOnly = true)
 * 
 * 2. Stale reads:
 *    - Check replication lag
 *    - Consider reading from primary after writes
 * 
 * 3. Connection pool exhaustion:
 *    - Monitor pool sizes
 *    - Adjust max connections
 *    - Check for connection leaks
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    // Logger for debugging datasource routing
    private static final Logger logger = LoggerFactory.getLogger(RoutingDataSource.class);

    /**
     * Determine Current Lookup Key
     * 
     * <p>This method is called by Spring whenever a database connection is needed.
     * It returns a key that identifies which datasource to use.</p>
     * 
     * <p><b>Routing Logic:</b></p>
     * <ul>
     *   <li>If transaction is read-only → return "replica"</li>
     *   <li>If transaction is not read-only → return "primary"</li>
     *   <li>If no transaction → return "primary" (safe default)</li>
     * </ul>
     * 
     * <p><b>Called By:</b></p>
     * <ul>
     *   <li>AbstractRoutingDataSource.getConnection()</li>
     *   <li>Every time JPA needs a database connection</li>
     *   <li>Once per transaction (connection is reused)</li>
     * </ul>
     * 
     * @return Datasource key: "primary" or "replica"
     */
    @Override
    protected Object determineCurrentLookupKey() {
        // Check if there's an active transaction
        // TransactionSynchronizationManager stores transaction metadata in ThreadLocal
        // This is thread-safe - each request thread has its own transaction state
        boolean isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        
        // If no transaction, default to primary for safety
        // This prevents accidental writes to replica
        if (!isTransactionActive) {
            logger.debug("No active transaction, routing to PRIMARY datasource");
            return "primary";
        }
        
        // Check if current transaction is read-only
        // This flag is set by @Transactional(readOnly = true/false)
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        // Route based on read-only flag
        if (isReadOnly) {
            // Read-only transaction → Route to replica
            // Replica is optimized for reads and can handle more load
            logger.debug("Read-only transaction detected, routing to REPLICA datasource");
            return "replica";
        } else {
            // Write transaction → Route to primary
            // Primary is the source of truth for all writes
            logger.debug("Write transaction detected, routing to PRIMARY datasource");
            return "primary";
        }
    }
}

