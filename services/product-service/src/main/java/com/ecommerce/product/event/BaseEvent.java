package com.ecommerce.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Base Event for Event Sourcing
 * 
 * <p>Abstract base class for all domain events in the Product Service.
 * Implements Event Sourcing pattern where all state changes are captured as events.</p>
 * 
 * <h2>Event Sourcing Fundamentals:</h2>
 * <pre>
 * What is an Event?
 * - Immutable fact that something happened
 * - Past tense naming (ProductCreated, not CreateProduct)
 * - Contains all data needed to describe what happened
 * - Stored permanently in Event Store
 * 
 * Event Properties:
 * - eventId: Unique identifier for this event
 * - aggregateId: ID of the entity this event affects
 * - eventType: Type of event (class name)
 * - version: Version number in event sequence
 * - timestamp: When event occurred
 * - userId: Who triggered the event
 * 
 * Event Flow:
 * 1. Command arrives (CreateProduct)
 * 2. Validate business rules
 * 3. Create event (ProductCreatedEvent)
 * 4. Save event to Event Store
 * 5. Apply event to aggregate
 * 6. Update projection/read model
 * 7. Publish event to Kafka
 * </pre>
 * 
 * <h2>Event Versioning:</h2>
 * <pre>
 * Why Version Events?
 * - Track order of events
 * - Prevent concurrent modifications
 * - Enable optimistic locking
 * 
 * Example Event Sequence:
 * Product ID: 123
 * v1: ProductCreatedEvent(name="Laptop", price=1000)
 * v2: PriceChangedEvent(oldPrice=1000, newPrice=1200)
 * v3: StockAddedEvent(quantity=50)
 * v4: ProductUpdatedEvent(description="New description")
 * 
 * Current State = Replay v1 + v2 + v3 + v4
 * 
 * Optimistic Locking:
 * - Load aggregate at version 4
 * - User A tries to update (expects v4, creates v5)
 * - User B tries to update (expects v4, creates v5)
 * - First save succeeds (v5)
 * - Second save fails (version conflict - expected v4, found v5)
 * - User B reloads and retries
 * </pre>
 * 
 * <h2>Event Immutability:</h2>
 * <pre>
 * Why Immutable?
 * - Events are historical facts
 * - Cannot change the past
 * - Audit trail integrity
 * - Concurrent access safety
 * 
 * Immutability Rules:
 * - Never update existing events
 * - Never delete events (soft delete via new event)
 * - Only append new events
 * 
 * Fixing Mistakes:
 * BAD:  Update event (violates immutability)
 * GOOD: Create compensating event
 * 
 * Example:
 * v1: ProductCreatedEvent(price=1000)  // Wrong price
 * v2: PriceChangedEvent(oldPrice=1000, newPrice=1200)  // Correction
 * 
 * Current state = 1200 (correct)
 * History shows mistake and correction (audit trail)
 * </pre>
 * 
 * <h2>MongoDB Storage:</h2>
 * <pre>
 * Collection: product_events
 * 
 * Document Structure:
 * {
 *   "_id": "evt-123-456",
 *   "aggregateId": "prod-123",
 *   "eventType": "ProductCreatedEvent",
 *   "version": 1,
 *   "timestamp": ISODate("2024-01-01T10:00:00Z"),
 *   "userId": "user-789",
 *   "data": {
 *     "name": "Laptop",
 *     "price": 1000,
 *     "categoryId": "cat-electronics"
 *   }
 * }
 * 
 * Indexes:
 * - { aggregateId: 1, version: 1 } (unique)  // Find events by product
 * - { aggregateId: 1, timestamp: -1 }        // Chronological order
 * - { eventType: 1, timestamp: -1 }          // Find by type
 * - { timestamp: -1 }                        // All events chronological
 * </pre>
 * 
 * <h2>Event Replay:</h2>
 * <pre>
 * Rebuilding Current State:
 * 
 * 1. Load all events for aggregate:
 *    events = eventStore.findByAggregateId("prod-123")
 * 
 * 2. Create empty aggregate:
 *    product = new Product()
 * 
 * 3. Apply events in order:
 *    for (event : events) {
 *        product.apply(event)
 *    }
 * 
 * 4. Result: Current state
 * 
 * Performance Optimization (Snapshots):
 * - Create snapshot every N events (e.g., 100)
 * - Load latest snapshot
 * - Replay only events after snapshot
 * - Much faster for long event histories
 * 
 * Example:
 * Events: v1...v150
 * Snapshot at v100
 * Load snapshot (v100)
 * Replay v101...v150 (only 50 events)
 * Instead of replaying all 150 events
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@SuperBuilder  // Lombok: builder pattern with inheritance support
@Document(collection = "product_events")  // MongoDB collection
public abstract class BaseEvent {

    /**
     * Event ID - unique identifier for this event
     * 
     * <p>Generated using UUID or MongoDB ObjectId.
     * Must be globally unique across all events.</p>
     */
    @Id
    private String eventId;

    /**
     * Aggregate ID - identifier of the entity this event affects
     * 
     * <p>For product events, this is the product ID.
     * All events for the same product share the same aggregateId.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * aggregateId = "prod-123"
     * All events: ProductCreated, PriceChanged, StockAdded
     * Same aggregateId links them together
     * </pre>
     */
    private String aggregateId;

    /**
     * Event Type - discriminator for event classes
     * 
     * <p>Stores the class name of the concrete event.
     * Used for polymorphic deserialization from MongoDB.</p>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>ProductCreatedEvent</li>
     *   <li>PriceChangedEvent</li>
     *   <li>StockChangedEvent</li>
     * </ul>
     */
    private String eventType;

    /**
     * Version - sequence number of this event
     * 
     * <p>Monotonically increasing version number starting from 1.
     * Used for ordering events and optimistic locking.</p>
     * 
     * <p><b>Usage:</b></p>
     * <pre>
     * Product created: version = 1
     * Price changed:   version = 2
     * Stock updated:   version = 3
     * Current version: 3
     * </pre>
     */
    private Long version;

    /**
     * Timestamp - when this event occurred
     * 
     * <p>Instant (UTC timezone) when the event was created.
     * Used for temporal queries and event ordering.</p>
     * 
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Time-travel queries: "State at 2024-01-01"</li>
     *   <li>Audit logs: "Who changed price on Jan 15?"</li>
     *   <li>Event chronology: Order events by time</li>
     * </ul>
     */
    private Instant timestamp;

    /**
     * User ID - who triggered this event
     * 
     * <p>Identifier of the user who caused this event.
     * Essential for audit trail and accountability.</p>
     * 
     * <p><b>Sources:</b></p>
     * <ul>
     *   <li>JWT token (authenticated user)</li>
     *   <li>"SYSTEM" for automated events</li>
     *   <li>"MIGRATION" for data migrations</li>
     * </ul>
     */
    private String userId;

    /**
     * Aggregate Type - type of aggregate (e.g., "Product")
     * 
     * <p>Optional field to distinguish event types when
     * multiple aggregates share the same event store.</p>
     */
    private String aggregateType;

    /**
     * Metadata - additional context
     * 
     * <p>Optional map for storing extra information:</p>
     * <ul>
     *   <li>IP address</li>
     *   <li>User agent</li>
     *   <li>Correlation ID</li>
     *   <li>Tenant ID</li>
     * </ul>
     */
    // private Map<String, Object> metadata;

    /**
     * Get Event Name
     * 
     * <p>Returns the simple class name as event name.
     * Used for event type discrimination.</p>
     * 
     * @return Event name (e.g., "ProductCreatedEvent")
     */
    public String getEventName() {
        return this.getClass().getSimpleName();
    }
}

