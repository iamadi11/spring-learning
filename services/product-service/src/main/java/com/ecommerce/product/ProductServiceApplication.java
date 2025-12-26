package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Product Service Application
 * 
 * <p>Product Catalog Management Service implementing Event Sourcing pattern
 * with MongoDB for scalable product data management.</p>
 * 
 * <h2>Service Responsibilities:</h2>
 * <ul>
 *   <li>Product Catalog Management (create, update, delete products)</li>
 *   <li>Category Hierarchy Management</li>
 *   <li>Product Search and Filtering</li>
 *   <li>Inventory Tracking</li>
 *   <li>Product Reviews Integration</li>
 *   <li>Event Sourcing for Complete Audit Trail</li>
 * </ul>
 * 
 * <h2>Event Sourcing Pattern:</h2>
 * <pre>
 * What is Event Sourcing?
 * - Store all changes to application state as a sequence of events
 * - Instead of storing current state, store all state transitions
 * - Can reconstruct current state by replaying events
 * - Provides complete audit trail
 * 
 * Traditional CRUD:
 * ┌─────────────────────┐
 * │   Product Table     │
 * ├─────────────────────┤
 * │ id: 1               │
 * │ name: "Laptop"      │
 * │ price: 1200.00      │  ← Only current state
 * │ stock: 50           │
 * └─────────────────────┘
 * 
 * Event Sourcing:
 * ┌─────────────────────────────────────────┐
 * │          Event Store                    │
 * ├─────────────────────────────────────────┤
 * │ ProductCreatedEvent(id:1, name:"Laptop", price:1000) │
 * │ PriceChangedEvent(id:1, oldPrice:1000, newPrice:1200) │
 * │ StockAddedEvent(id:1, quantity:50)      │
 * │ ProductUpdatedEvent(id:1, ...)          │
 * └─────────────────────────────────────────┘
 *          ↓ Replay Events
 * ┌─────────────────────┐
 * │   Current State     │
 * ├─────────────────────┤
 * │ id: 1               │
 * │ name: "Laptop"      │
 * │ price: 1200.00      │
 * │ stock: 50           │
 * └─────────────────────┘
 * 
 * Benefits:
 * 1. Complete Audit Trail:
 *    - Know exactly what happened and when
 *    - Who made what changes
 *    - Why changes were made
 * 
 * 2. Time Travel:
 *    - Reconstruct state at any point in time
 *    - "What was the price on Dec 1st?"
 *    - Replay events to specific timestamp
 * 
 * 3. Event Replay:
 *    - Rebuild entire database from events
 *    - Fix bugs by replaying with corrected logic
 *    - Create new projections (read models)
 * 
 * 4. Debugging:
 *    - See exact sequence of operations
 *    - Reproduce bugs by replaying events
 *    - Understand how system reached current state
 * 
 * 5. Analytics:
 *    - Analyze user behavior
 *    - Business intelligence
 *    - Trend analysis
 * </pre>
 * 
 * <h2>Event Sourcing Implementation:</h2>
 * <pre>
 * Components:
 * 
 * 1. Events:
 *    - ProductCreatedEvent
 *    - ProductUpdatedEvent
 *    - PriceChangedEvent
 *    - StockChangedEvent
 *    - ProductDeletedEvent
 * 
 * 2. Event Store:
 *    - MongoDB collection: product_events
 *    - Stores all events chronologically
 *    - Immutable (never update, only append)
 * 
 * 3. Aggregate:
 *    - Product aggregate
 *    - Applies events to build current state
 *    - Validates business rules
 * 
 * 4. Projections (Read Models):
 *    - ProductView (current state for queries)
 *    - Rebuilt from events
 *    - Optimized for reads
 * 
 * Flow:
 * 
 * Command (Write):
 * 1. User: "Update product price to $1200"
 * 2. Command Handler validates
 * 3. Create PriceChangedEvent
 * 4. Save event to Event Store
 * 5. Apply event to aggregate
 * 6. Update projection (ProductView)
 * 7. Publish event to Kafka
 * 
 * Query (Read):
 * 1. User: "Get product details"
 * 2. Query ProductView collection
 * 3. Return current state
 * 4. (No event replay needed for reads)
 * </pre>
 * 
 * <h2>MongoDB Sharding Strategy:</h2>
 * <pre>
 * Why Shard MongoDB?
 * - Horizontal scaling for massive datasets
 * - Distribute data across multiple servers
 * - Handle millions of products
 * 
 * Sharding Setup:
 * ┌──────────────────────────────────────┐
 * │         MongoDB Router (mongos)       │
 * └──────────────┬───────────────────────┘
 *                │
 *       ┌────────┴────────┐
 *       ↓                 ↓
 * ┌──────────┐      ┌──────────┐
 * │ Shard 1  │      │ Shard 2  │
 * │ Products │      │ Products │
 * │ A-M      │      │ N-Z      │
 * └──────────┘      └──────────┘
 * 
 * Shard Key Selection:
 * - Option 1: category_id (distribute by category)
 * - Option 2: product_id (hash-based distribution)
 * - Option 3: created_date (range-based)
 * 
 * We'll use: category_id + product_id (compound key)
 * - Good distribution
 * - Supports category-based queries
 * - Prevents hot spots
 * 
 * Configuration:
 * sh.enableSharding("ecommerce_product_db")
 * sh.shardCollection(
 *   "ecommerce_product_db.products",
 *   { category_id: 1, _id: 1 }
 * )
 * </pre>
 * 
 * <h2>MongoDB Collections:</h2>
 * <pre>
 * 1. product_events (Event Store):
 *    - Immutable event log
 *    - Chronological order
 *    - Never delete
 *    - Indexed by product_id and timestamp
 * 
 * 2. products (Current State/Projection):
 *    - Current product data
 *    - Optimized for queries
 *    - Can be rebuilt from events
 *    - Sharded by category_id + _id
 * 
 * 3. categories:
 *    - Category hierarchy
 *    - Parent-child relationships
 *    - Tree structure
 * 
 * 4. product_snapshots (Optional):
 *    - Periodic snapshots of product state
 *    - Avoid replaying all events
 *    - Optimization for long event histories
 * </pre>
 * 
 * <h2>Service Interactions:</h2>
 * <pre>
 * Product Service interactions:
 * 
 * 1. Order Service:
 *    Product Service → Kafka → Order Service
 *    - ProductCreatedEvent
 *    - PriceChangedEvent (update orders)
 *    - StockChangedEvent (availability)
 * 
 * 2. Review Service:
 *    Review Service → Product Service (gRPC)
 *    - Get product details for reviews
 *    - Update product ratings
 * 
 * 3. Search Service (Future):
 *    Product Service → Kafka → Search Service
 *    - Index products in Elasticsearch
 *    - Full-text search
 *    - Faceted search
 * 
 * 4. Inventory Service (Future):
 *    Product Service ↔ Inventory Service
 *    - Stock synchronization
 *    - Availability checks
 * </pre>
 * 
 * <h2>Caching Strategy:</h2>
 * <pre>
 * Redis Cache:
 * 
 * 1. Product Details:
 *    - Key: product:{productId}
 *    - TTL: 1 hour
 *    - Invalidate on product update
 * 
 * 2. Category Products:
 *    - Key: category:{categoryId}:products:{page}
 *    - TTL: 30 minutes
 *    - Invalidate on category changes
 * 
 * 3. Search Results:
 *    - Key: search:{query}:{filters}:{page}
 *    - TTL: 15 minutes
 *    - Invalidate on product updates
 * </pre>
 * 
 * <h2>API Design:</h2>
 * <pre>
 * Product Management:
 * POST   /api/products              - Create product
 * GET    /api/products/{id}         - Get product details
 * PUT    /api/products/{id}         - Update product
 * DELETE /api/products/{id}         - Delete product
 * GET    /api/products               - List products (paginated)
 * GET    /api/products/search       - Search products
 * 
 * Category Management:
 * POST   /api/categories            - Create category
 * GET    /api/categories            - List categories
 * GET    /api/categories/{id}       - Get category details
 * PUT    /api/categories/{id}       - Update category
 * 
 * Inventory:
 * PATCH  /api/products/{id}/stock   - Update stock
 * GET    /api/products/{id}/availability - Check availability
 * 
 * Events:
 * GET    /api/products/{id}/history - Get event history
 * POST   /api/products/{id}/replay  - Replay events (admin)
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication  // Main Spring Boot application
@EnableDiscoveryClient  // Register with Eureka
@EnableCaching  // Enable Redis caching
@EnableKafka  // Enable Kafka for events
@EnableMongoAuditing  // Enable MongoDB auditing (createdAt, updatedAt)
public class ProductServiceApplication {

    /**
     * Main method - application entry point
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}

