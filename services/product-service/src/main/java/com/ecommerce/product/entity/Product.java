package com.ecommerce.product.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product Entity (Aggregate Root)
 * 
 * <p>Represents the current state of a product in the catalog.
 * This is the READ MODEL (projection) in CQRS/Event Sourcing.</p>
 * 
 * <h2>Event Sourcing Architecture:</h2>
 * <pre>
 * ┌─────────────────────┐
 * │   Event Store       │  ← Immutable event log (source of truth)
 * │  product_events     │
 * └──────────┬──────────┘
 *            │ Apply events
 *            ↓
 * ┌─────────────────────┐
 * │  Product Entity     │  ← Current state (projection)
 * │   products          │  ← Optimized for queries
 * └─────────────────────┘
 * 
 * Write Flow:
 * Command → Validate → Create Event → Save to Event Store → Apply to Aggregate → Update Projection
 * 
 * Read Flow:
 * Query → Read from Projection (products collection) → Return
 * 
 * Rebuild Flow:
 * Load Events → Replay → Reconstruct Projection
 * </pre>
 * 
 * <h2>Aggregate Pattern:</h2>
 * <pre>
 * What is an Aggregate?
 * - Cluster of domain objects treated as a single unit
 * - Has a root entity (Product)
 * - Enforces business rules and invariants
 * - Receives commands and emits events
 * 
 * Product Aggregate Responsibilities:
 * 1. Validate business rules
 * 2. Apply events to update state
 * 3. Emit new events for state changes
 * 4. Ensure consistency
 * 
 * Example Business Rules:
 * - Price must be positive
 * - Stock cannot be negative
 * - SKU must be unique
 * - Product must have a category
 * - Name must not be empty
 * </pre>
 * 
 * <h2>MongoDB Collection Structure:</h2>
 * <pre>
 * Collection: products
 * 
 * Document Example:
 * {
 *   "_id": "prod-laptop-001",
 *   "name": "MacBook Pro 16",
 *   "description": "High-performance laptop",
 *   "price": NumberDecimal("2499.99"),
 *   "categoryId": "cat-electronics",
 *   "sku": "MBP16-2024",
 *   "brand": "Apple",
 *   "stock": 50,
 *   "weight": 2.0,
 *   "dimensions": "35.79 x 24.81 x 1.62",
 *   "images": [
 *     "https://cdn.example.com/products/mbp16-1.jpg",
 *     "https://cdn.example.com/products/mbp16-2.jpg"
 *   ],
 *   "attributes": {
 *     "color": "Space Gray",
 *     "memory": "32GB",
 *     "storage": "1TB SSD"
 *   },
 *   "tags": ["laptop", "apple", "macbook", "professional"],
 *   "active": true,
 *   "featured": false,
 *   "version": 5,
 *   "createdAt": ISODate("2024-01-01T10:00:00Z"),
 *   "updatedAt": ISODate("2024-01-15T14:30:00Z")
 * }
 * </pre>
 * 
 * <h2>Indexing Strategy:</h2>
 * <pre>
 * Indexes for Performance:
 * 
 * 1. Primary Key:
 *    { _id: 1 }  // Automatic
 * 
 * 2. Category and Time:
 *    { categoryId: 1, createdAt: -1 }
 *    - List products in category (newest first)
 * 
 * 3. Full-Text Search:
 *    { name: "text", description: "text" }
 *    - Search products by name/description
 * 
 * 4. Price Range:
 *    { price: 1 }
 *    - Filter by price range
 * 
 * 5. Stock Status:
 *    { stock: 1 }
 *    - Find out-of-stock products
 * 
 * 6. Active and Featured:
 *    { active: 1, featured: 1 }
 *    - Get featured active products
 * 
 * 7. SKU Unique:
 *    { sku: 1 } (unique)
 *    - Ensure SKU uniqueness
 * </pre>
 * 
 * <h2>MongoDB Sharding:</h2>
 * <pre>
 * Shard Key: { categoryId: 1, _id: 1 }
 * 
 * Why This Shard Key?
 * 1. Good Distribution:
 *    - Categories distribute across shards
 *    - Prevents hot spots
 * 
 * 2. Query Efficiency:
 *    - Category queries hit single shard
 *    - Reduces scatter-gather queries
 * 
 * 3. Logical Grouping:
 *    - Related products co-located
 *    - Better cache locality
 * 
 * Setup:
 * sh.enableSharding("ecommerce_product_db")
 * sh.shardCollection(
 *   "ecommerce_product_db.products",
 *   { categoryId: 1, _id: 1 }
 * )
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: no-arg constructor
@AllArgsConstructor  // Lombok: all-args constructor
@Builder  // Lombok: builder pattern
@Document(collection = "products")  // MongoDB collection name
@CompoundIndex(name = "category_created_idx", def = "{'categoryId': 1, 'createdAt': -1}")  // Compound index
public class Product {

    /**
     * Product ID - unique identifier
     * 
     * <p>Generated as "prod-{uuid}" or MongoDB ObjectId.
     * This is also the aggregateId in Event Sourcing.</p>
     */
    @Id
    private String productId;

    /**
     * Product name
     * 
     * <p>Searchable via text index.</p>
     */
    @TextIndexed  // Enable full-text search on name
    private String name;

    /**
     * Product description
     * 
     * <p>Detailed product information.
     * Searchable via text index.</p>
     */
    @TextIndexed  // Enable full-text search on description
    private String description;

    /**
     * Product price
     * 
     * <p>Stored as BigDecimal for precision.
     * Indexed for price range queries.</p>
     */
    @Indexed  // Index for price range queries
    private BigDecimal price;

    /**
     * Category ID
     * 
     * <p>Reference to Category collection.
     * Part of shard key for distribution.</p>
     */
    @Indexed  // Index for category queries
    private String categoryId;

    /**
     * SKU (Stock Keeping Unit)
     * 
     * <p>Unique product identifier for inventory.
     * Must be unique across all products.</p>
     */
    @Indexed(unique = true)  // Unique index
    private String sku;

    /**
     * Brand name
     */
    private String brand;

    /**
     * Current stock quantity
     * 
     * <p>Updated via StockChangedEvent.
     * Indexed for inventory queries.</p>
     */
    @Indexed  // Index for stock queries
    private Integer stock;

    /**
     * Product weight in kilograms
     * 
     * <p>Used for shipping calculations.</p>
     */
    private Double weight;

    /**
     * Product dimensions (L x W x H in cm)
     * 
     * <p>Format: "35.79 x 24.81 x 1.62"
     * Used for shipping and packaging.</p>
     */
    private String dimensions;

    /**
     * Product image URLs
     * 
     * <p>List of CDN URLs for product images.
     * First image is primary.</p>
     */
    @Builder.Default
    private List<String> images = new ArrayList<>();

    /**
     * Product attributes (flexible key-value pairs)
     * 
     * <p>Examples:
     * - color: "Space Gray"
     * - memory: "32GB"
     * - storage: "1TB SSD"
     * - screen: "16 inch"
     * 
     * Allows flexible product attributes without schema changes.</p>
     */
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    /**
     * Product tags for search and filtering
     * 
     * <p>Examples: ["laptop", "apple", "professional", "high-performance"]</p>
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * Is product active/published?
     * 
     * <p>Inactive products not shown to customers.
     * Indexed for filtering active products.</p>
     */
    @Indexed  // Index for active status
    private Boolean active;

    /**
     * Is product featured?
     * 
     * <p>Featured products shown on homepage.
     * Indexed for featured queries.</p>
     */
    @Indexed  // Index for featured products
    private Boolean featured;

    /**
     * Average rating (0-5)
     * 
     * <p>Calculated from reviews.
     * Updated by Review Service.</p>
     */
    private Double averageRating;

    /**
     * Total number of reviews
     */
    private Integer reviewCount;

    /**
     * Version number (optimistic locking)
     * 
     * <p>Corresponds to event version in Event Store.
     * Prevents concurrent modification conflicts.</p>
     */
    @Version  // Spring Data optimistic locking
    private Long version;

    /**
     * Creation timestamp
     * 
     * <p>Automatically set by MongoDB auditing.</p>
     */
    @CreatedDate
    private Instant createdAt;

    /**
     * Last update timestamp
     * 
     * <p>Automatically updated by MongoDB auditing.</p>
     */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Check if product is in stock
     * 
     * @return true if stock > 0
     */
    public boolean isInStock() {
        return stock != null && stock > 0;
    }

    /**
     * Check if product is available for purchase
     * 
     * @return true if active and in stock
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(active) && isInStock();
    }

    /**
     * Add image URL
     * 
     * @param imageUrl Image URL to add
     */
    public void addImage(String imageUrl) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(imageUrl);
    }

    /**
     * Add attribute
     * 
     * @param key Attribute key
     * @param value Attribute value
     */
    public void addAttribute(String key, String value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    /**
     * Add tag
     * 
     * @param tag Tag to add
     */
    public void addTag(String tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
}

