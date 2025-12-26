package com.ecommerce.product.repository;

import com.ecommerce.product.event.BaseEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Event Store Repository
 * 
 * <p>Repository for storing and retrieving domain events.
 * Core component of Event Sourcing pattern.</p>
 * 
 * <h2>Event Store Characteristics:</h2>
 * <pre>
 * Append-Only Log:
 * - Events are NEVER updated
 * - Events are NEVER deleted (immutable)
 * - Only INSERT operations allowed
 * - Ordered by timestamp and version
 * 
 * Event Store Benefits:
 * 1. Complete Audit Trail:
 *    - Who did what, when, why
 *    - Perfect for compliance
 * 
 * 2. Time Travel:
 *    - Reconstruct state at any point
 *    - "What was product price on Dec 1?"
 * 
 * 3. Event Replay:
 *    - Rebuild projections from scratch
 *    - Fix bugs by replaying with corrections
 * 
 * 4. Debugging:
 *    - See exact sequence of operations
 *    - Reproduce production issues
 * 
 * 5. Analytics:
 *    - Business intelligence
 *    - User behavior analysis
 *    - Trend detection
 * </pre>
 * 
 * <h2>Query Patterns:</h2>
 * <pre>
 * 1. Get All Events for Aggregate:
 *    findByAggregateIdOrderByVersionAsc("prod-123")
 *    - Replay to reconstruct current state
 * 
 * 2. Get Events After Version:
 *    findByAggregateIdAndVersionGreaterThan("prod-123", 5)
 *    - Used with snapshots
 *    - Only replay recent events
 * 
 * 3. Get Events in Time Range:
 *    findByAggregateIdAndTimestampBetween("prod-123", start, end)
 *    - Time-travel queries
 *    - Audit reports
 * 
 * 4. Get Latest Event:
 *    findTopByAggregateIdOrderByVersionDesc("prod-123")
 *    - Check current version
 *    - Optimistic locking
 * 
 * 5. Get Events by Type:
 *    findByEventTypeOrderByTimestampDesc("PriceChangedEvent")
 *    - Analytics on price changes
 *    - Business reports
 * </pre>
 * 
 * <h2>Event Replay Example:</h2>
 * <pre>
 * Reconstruct Product State:
 * 
 * // 1. Load all events for product
 * List<BaseEvent> events = eventStore.findByAggregateIdOrderByVersionAsc("prod-123");
 * 
 * // 2. Create empty aggregate
 * Product product = new Product();
 * 
 * // 3. Apply events in order
 * for (BaseEvent event : events) {
 *     if (event instanceof ProductCreatedEvent created) {
 *         product.apply(created);
 *     } else if (event instanceof PriceChangedEvent priceChanged) {
 *         product.apply(priceChanged);
 *     } else if (event instanceof StockChangedEvent stockChanged) {
 *         product.apply(stockChanged);
 *     }
 *     // ... handle other event types
 * }
 * 
 * // 4. Result: product now in current state
 * return product;
 * </pre>
 * 
 * <h2>Snapshot Optimization:</h2>
 * <pre>
 * Problem: Long event histories slow down replay
 * 
 * Solution: Periodic snapshots
 * 
 * Without Snapshot:
 * - Product has 1000 events
 * - Must replay all 1000 events
 * - Slow for frequently accessed products
 * 
 * With Snapshot:
 * - Create snapshot every 100 events
 * - Latest snapshot at version 900
 * - Load snapshot (v900)
 * - Replay only events 901-1000
 * - Much faster!
 * 
 * Snapshot Strategy:
 * - Store snapshot in separate collection
 * - Include version number
 * - Rebuild if corrupted (from events)
 * </pre>
 * 
 * <h2>Consistency Guarantees:</h2>
 * <pre>
 * Unique Index on (aggregateId, version):
 * - Prevents duplicate events
 * - Ensures version monotonicity
 * - Implements optimistic locking
 * 
 * Concurrency Scenario:
 * 1. User A loads product at v10
 * 2. User B loads product at v10
 * 3. User A saves event v11 → SUCCESS
 * 4. User B tries to save event v11 → FAILS (duplicate key)
 * 5. User B reloads at v11 and retries
 * 
 * Result: Consistency maintained
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository  // Spring Data repository
public interface EventStoreRepository extends MongoRepository<BaseEvent, String> {

    /**
     * Find All Events for Aggregate (Ordered by Version)
     * 
     * <p>Returns all events for a specific aggregate in version order.
     * Used to reconstruct current state by replaying events.</p>
     * 
     * <p><b>Use Case:</b> Rebuild product from event history</p>
     * 
     * @param aggregateId Aggregate ID (e.g., product ID)
     * @return List of events ordered by version (ascending)
     */
    List<BaseEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);

    /**
     * Find All Events for Aggregate (Ordered by Timestamp)
     * 
     * <p>Returns all events for a specific aggregate in chronological order.</p>
     * 
     * @param aggregateId Aggregate ID
     * @return List of events ordered by timestamp (ascending)
     */
    List<BaseEvent> findByAggregateIdOrderByTimestampAsc(String aggregateId);

    /**
     * Find Events After Specific Version
     * 
     * <p>Returns events after a specific version.
     * Used with snapshots - load snapshot, then replay events after snapshot version.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * // Load snapshot at version 100
     * Product product = snapshotRepo.findLatest("prod-123");
     * 
     * // Load events after v100
     * List<BaseEvent> recentEvents = eventStore.findByAggregateIdAndVersionGreaterThan("prod-123", 100);
     * 
     * // Replay only recent events (much faster)
     * recentEvents.forEach(product::apply);
     * </pre>
     * 
     * @param aggregateId Aggregate ID
     * @param version Version number
     * @return List of events with version > specified version
     */
    @Query("{ 'aggregateId': ?0, 'version': { $gt: ?1 } }")
    List<BaseEvent> findByAggregateIdAndVersionGreaterThan(String aggregateId, Long version);

    /**
     * Find Events in Time Range
     * 
     * <p>Returns events within a specific time range.
     * Used for temporal queries and audit reports.</p>
     * 
     * <p><b>Use Case:</b> "Show all product changes in December 2024"</p>
     * 
     * @param aggregateId Aggregate ID
     * @param start Start timestamp
     * @param end End timestamp
     * @return List of events in time range
     */
    @Query("{ 'aggregateId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<BaseEvent> findByAggregateIdAndTimestampBetween(String aggregateId, Instant start, Instant end);

    /**
     * Find Latest Event for Aggregate
     * 
     * <p>Returns the most recent event for an aggregate.
     * Used to get current version for optimistic locking.</p>
     * 
     * <p><b>Use Case:</b> Check current version before saving new event</p>
     * 
     * @param aggregateId Aggregate ID
     * @return Latest event (highest version)
     */
    Optional<BaseEvent> findTopByAggregateIdOrderByVersionDesc(String aggregateId);

    /**
     * Find Events by Event Type
     * 
     * <p>Returns all events of a specific type.
     * Used for analytics and business reports.</p>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>"How many price changes in last month?"</li>
     *   <li>"Which products had stock changes?"</li>
     *   <li>"Audit all delete operations"</li>
     * </ul>
     * 
     * @param eventType Event type (class name)
     * @return List of events of specified type
     */
    List<BaseEvent> findByEventTypeOrderByTimestampDesc(String eventType);

    /**
     * Count Events for Aggregate
     * 
     * <p>Returns total number of events for an aggregate.
     * Used to decide when to create snapshots.</p>
     * 
     * <p><b>Snapshot Strategy:</b></p>
     * <pre>
     * long eventCount = eventStore.countByAggregateId("prod-123");
     * if (eventCount % 100 == 0) {
     *     // Create snapshot every 100 events
     *     snapshotRepo.save(createSnapshot(product));
     * }
     * </pre>
     * 
     * @param aggregateId Aggregate ID
     * @return Number of events
     */
    long countByAggregateId(String aggregateId);

    /**
     * Check if Events Exist for Aggregate
     * 
     * <p>Fast existence check without loading events.</p>
     * 
     * @param aggregateId Aggregate ID
     * @return true if events exist
     */
    boolean existsByAggregateId(String aggregateId);
}

