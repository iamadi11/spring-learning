package com.ecommerce.product.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product Created Event
 * 
 * <p>Event emitted when a new product is created in the catalog.
 * Contains all initial product data.</p>
 * 
 * <h2>Event Sourcing Example:</h2>
 * <pre>
 * Scenario: Admin creates new laptop product
 * 
 * Command:
 * POST /api/products
 * {
 *   "name": "MacBook Pro 16",
 *   "description": "High-performance laptop",
 *   "price": 2499.99,
 *   "categoryId": "cat-electronics",
 *   "sku": "MBP16-2024",
 *   "stock": 50
 * }
 * 
 * Event Created:
 * {
 *   "eventId": "evt-abc123",
 *   "aggregateId": "prod-laptop-001",
 *   "eventType": "ProductCreatedEvent",
 *   "version": 1,
 *   "timestamp": "2024-01-01T10:00:00Z",
 *   "userId": "admin-001",
 *   "name": "MacBook Pro 16",
 *   "description": "High-performance laptop",
 *   "price": 2499.99,
 *   "categoryId": "cat-electronics",
 *   "sku": "MBP16-2024",
 *   "stock": 50,
 *   "active": true
 * }
 * 
 * Actions Triggered:
 * 1. Save event to Event Store (MongoDB)
 * 2. Create Product projection (current state)
 * 3. Publish to Kafka: product-events topic
 * 4. Consumers react:
 *    - Search Service: Index in Elasticsearch
 *    - Notification Service: Alert admins
 *    - Analytics Service: Track catalog growth
 * </pre>
 * 
 * <h2>Applying Event to Aggregate:</h2>
 * <pre>
 * Product Aggregate:
 * 
 * public void apply(ProductCreatedEvent event) {
 *     this.productId = event.getAggregateId();
 *     this.name = event.getName();
 *     this.description = event.getDescription();
 *     this.price = event.getPrice();
 *     this.categoryId = event.getCategoryId();
 *     this.sku = event.getSku();
 *     this.stock = event.getStock();
 *     this.active = event.getActive();
 *     this.images = event.getImages();
 *     this.attributes = event.getAttributes();
 *     this.version = event.getVersion();
 * }
 * 
 * Result: Product aggregate in "created" state
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data  // Lombok: getters, setters, toString, equals, hashCode
@EqualsAndHashCode(callSuper = true)  // Include superclass fields in equals/hashCode
@NoArgsConstructor  // No-arg constructor
@AllArgsConstructor  // All-args constructor
@SuperBuilder  // Builder with inheritance
public class ProductCreatedEvent extends BaseEvent {

    /**
     * Product name
     */
    private String name;

    /**
     * Product description
     */
    private String description;

    /**
     * Product price
     */
    private BigDecimal price;

    /**
     * Category ID this product belongs to
     */
    private String categoryId;

    /**
     * SKU (Stock Keeping Unit) - unique product identifier
     */
    private String sku;

    /**
     * Brand name
     */
    private String brand;

    /**
     * Initial stock quantity
     */
    private Integer stock;

    /**
     * Product weight (in kg)
     */
    private Double weight;

    /**
     * Product dimensions (length x width x height in cm)
     */
    private String dimensions;

    /**
     * Product image URLs
     */
    private List<String> images;

    /**
     * Product attributes (color, size, etc.)
     * Key-value pairs for flexible attributes
     */
    private Map<String, String> attributes;

    /**
     * Product tags for search and filtering
     */
    private List<String> tags;

    /**
     * Is product active/published
     */
    private Boolean active;

    /**
     * Is product featured
     */
    private Boolean featured;
}

