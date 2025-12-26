package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.event.*;
import com.ecommerce.product.repository.EventStoreRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Product Command Service
 * 
 * <p>Handles all write operations using Event Sourcing pattern.
 * Commands create events, events update state.</p>
 * 
 * <h2>Command Pattern in Event Sourcing:</h2>
 * <pre>
 * Command Flow:
 * 
 * 1. Command arrives (CreateProduct)
 *    ↓
 * 2. Validate business rules
 *    ↓
 * 3. Create event (ProductCreatedEvent)
 *    ↓
 * 4. Save event to Event Store
 *    ↓
 * 5. Apply event to aggregate
 *    ↓
 * 6. Save aggregate to projection
 *    ↓
 * 7. Publish event to Kafka
 *    ↓
 * 8. Return result
 * 
 * Example: Create Product
 * 
 * Command: CreateProductCommand {
 *   name: "Laptop",
 *   price: 1000,
 *   ...
 * }
 * 
 * Event: ProductCreatedEvent {
 *   aggregateId: "prod-123",
 *   version: 1,
 *   timestamp: now,
 *   name: "Laptop",
 *   price: 1000,
 *   ...
 * }
 * 
 * Projection: Product {
 *   productId: "prod-123",
 *   name: "Laptop",
 *   price: 1000,
 *   version: 1
 * }
 * </pre>
 * 
 * <h2>Why Event Sourcing for Products?</h2>
 * <pre>
 * Benefits:
 * 
 * 1. Price History:
 *    - Track all price changes
 *    - Know exact price at any date
 *    - Analytics on pricing strategies
 * 
 * 2. Audit Trail:
 *    - Who changed what and when
 *    - Compliance requirements
 *    - Dispute resolution
 * 
 * 3. Inventory Tracking:
 *    - Complete stock movement history
 *    - Reconciliation
 *    - Loss prevention
 * 
 * 4. Debugging:
 *    - Reproduce exact state
 *    - Understand how bug occurred
 *    - Fix and replay events
 * 
 * 5. Business Intelligence:
 *    - Product lifecycle analysis
 *    - Demand forecasting
 *    - Supplier performance
 * </pre>
 * 
 * <h2>Transaction Boundaries:</h2>
 * <pre>
 * Single Transaction:
 * - Save event to Event Store
 * - Update projection (Product)
 * - Both succeed or both fail (ACID)
 * 
 * Eventual Consistency:
 * - Kafka publish happens after commit
 * - Other services eventually consistent
 * - Use Outbox Pattern for guaranteed delivery
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service  // Spring service component
public class ProductCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ProductCommandService.class);

    // Event Store for saving events
    private final EventStoreRepository eventStoreRepository;
    
    // Product projection for current state
    private final ProductRepository productRepository;

    @Autowired
    public ProductCommandService(
            EventStoreRepository eventStoreRepository,
            ProductRepository productRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.productRepository = productRepository;
    }

    /**
     * Create Product
     * 
     * <p>Creates a new product using Event Sourcing.</p>
     * 
     * <p><b>Flow:</b></p>
     * <pre>
     * 1. Validate: SKU unique, price positive, etc.
     * 2. Create ProductCreatedEvent
     * 3. Save event to Event Store
     * 4. Create Product projection from event
     * 5. Save projection
     * 6. Publish event to Kafka (TODO)
     * 7. Return product
     * </pre>
     * 
     * @param product Product to create
     * @param userId User creating the product
     * @return Created product
     * @throws RuntimeException if validation fails
     */
    @Transactional  // ACID transaction
    public Product createProduct(Product product, String userId) {
        logger.info("Creating product: {}", product.getName());
        
        // Step 1: Validate business rules
        validateProductCreation(product);
        
        // Step 2: Generate product ID
        String productId = "prod-" + UUID.randomUUID();
        
        // Step 3: Create ProductCreatedEvent
        ProductCreatedEvent event = ProductCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(productId)
            .aggregateType("Product")
            .eventType("ProductCreatedEvent")
            .version(1L)
            .timestamp(Instant.now())
            .userId(userId)
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .categoryId(product.getCategoryId())
            .sku(product.getSku())
            .brand(product.getBrand())
            .stock(product.getStock())
            .weight(product.getWeight())
            .dimensions(product.getDimensions())
            .images(product.getImages())
            .attributes(product.getAttributes())
            .tags(product.getTags())
            .active(product.getActive() != null ? product.getActive() : true)
            .featured(product.getFeatured() != null ? product.getFeatured() : false)
            .build();
        
        // Step 4: Save event to Event Store (immutable log)
        eventStoreRepository.save(event);
        logger.debug("Saved ProductCreatedEvent: {}", event.getEventId());
        
        // Step 5: Apply event to create projection
        Product newProduct = applyProductCreatedEvent(event);
        
        // Step 6: Save projection (current state)
        Product savedProduct = productRepository.save(newProduct);
        logger.info("Product created successfully: {}", savedProduct.getProductId());
        
        // Step 7: Publish event to Kafka (TODO)
        // kafkaTemplate.send("product-events", event);
        
        return savedProduct;
    }

    /**
     * Update Product
     * 
     * <p>Updates product details using Event Sourcing.</p>
     * 
     * @param productId Product ID
     * @param updates Product updates
     * @param userId User performing update
     * @return Updated product
     * @throws RuntimeException if product not found
     */
    @Transactional
    public Product updateProduct(String productId, Product updates, String userId) {
        logger.info("Updating product: {}", productId);
        
        // Load current product
        Product currentProduct = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        // Get next version
        Long nextVersion = currentProduct.getVersion() + 1;
        
        // Create ProductUpdatedEvent
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(productId)
            .aggregateType("Product")
            .eventType("ProductUpdatedEvent")
            .version(nextVersion)
            .timestamp(Instant.now())
            .userId(userId)
            .name(updates.getName())
            .description(updates.getDescription())
            .price(updates.getPrice())
            .categoryId(updates.getCategoryId())
            .brand(updates.getBrand())
            .weight(updates.getWeight())
            .dimensions(updates.getDimensions())
            .images(updates.getImages())
            .attributes(updates.getAttributes())
            .tags(updates.getTags())
            .active(updates.getActive())
            .featured(updates.getFeatured())
            .build();
        
        // Save event
        eventStoreRepository.save(event);
        logger.debug("Saved ProductUpdatedEvent: {}", event.getEventId());
        
        // Apply event to projection
        applyProductUpdatedEvent(currentProduct, event);
        
        // Save updated projection
        Product savedProduct = productRepository.save(currentProduct);
        logger.info("Product updated successfully: {}", productId);
        
        // Publish event (TODO)
        // kafkaTemplate.send("product-events", event);
        
        return savedProduct;
    }

    /**
     * Change Product Price
     * 
     * <p>Changes product price with reason tracking.</p>
     * 
     * @param productId Product ID
     * @param newPrice New price
     * @param reason Reason for price change
     * @param userId User changing price
     * @return Updated product
     */
    @Transactional
    public Product changePrice(String productId, BigDecimal newPrice, String reason, String userId) {
        logger.info("Changing price for product: {} to {}", productId, newPrice);
        
        // Validate price
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Price must be positive");
        }
        
        // Load current product
        Product currentProduct = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        BigDecimal oldPrice = currentProduct.getPrice();
        
        // No change - skip
        if (oldPrice.compareTo(newPrice) == 0) {
            logger.debug("Price unchanged, skipping event");
            return currentProduct;
        }
        
        // Get next version
        Long nextVersion = currentProduct.getVersion() + 1;
        
        // Create PriceChangedEvent
        PriceChangedEvent event = PriceChangedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(productId)
            .aggregateType("Product")
            .eventType("PriceChangedEvent")
            .version(nextVersion)
            .timestamp(Instant.now())
            .userId(userId)
            .oldPrice(oldPrice)
            .newPrice(newPrice)
            .reason(reason)
            .build();
        
        // Save event
        eventStoreRepository.save(event);
        logger.debug("Saved PriceChangedEvent: {}", event.getEventId());
        
        // Apply event
        applyPriceChangedEvent(currentProduct, event);
        
        // Save projection
        Product savedProduct = productRepository.save(currentProduct);
        logger.info("Price changed successfully: {} -> {}", oldPrice, newPrice);
        
        // Publish event (TODO)
        // kafkaTemplate.send("product-events", event);
        
        return savedProduct;
    }

    /**
     * Change Product Stock
     * 
     * <p>Updates product stock quantity.</p>
     * 
     * @param productId Product ID
     * @param newStock New stock quantity
     * @param reason Reason for stock change
     * @param userId User changing stock
     * @return Updated product
     */
    @Transactional
    public Product changeStock(String productId, Integer newStock, String reason, String userId) {
        logger.info("Changing stock for product: {} to {}", productId, newStock);
        
        // Validate stock
        if (newStock == null || newStock < 0) {
            throw new RuntimeException("Stock cannot be negative");
        }
        
        // Load current product
        Product currentProduct = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        Integer oldStock = currentProduct.getStock();
        Integer changeAmount = newStock - oldStock;
        
        // No change - skip
        if (changeAmount == 0) {
            logger.debug("Stock unchanged, skipping event");
            return currentProduct;
        }
        
        // Get next version
        Long nextVersion = currentProduct.getVersion() + 1;
        
        // Create StockChangedEvent
        StockChangedEvent event = StockChangedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(productId)
            .aggregateType("Product")
            .eventType("StockChangedEvent")
            .version(nextVersion)
            .timestamp(Instant.now())
            .userId(userId)
            .oldStock(oldStock)
            .newStock(newStock)
            .changeAmount(changeAmount)
            .reason(reason)
            .build();
        
        // Save event
        eventStoreRepository.save(event);
        logger.debug("Saved StockChangedEvent: {}", event.getEventId());
        
        // Apply event
        applyStockChangedEvent(currentProduct, event);
        
        // Save projection
        Product savedProduct = productRepository.save(currentProduct);
        logger.info("Stock changed successfully: {} -> {}", oldStock, newStock);
        
        // Publish event (TODO)
        // kafkaTemplate.send("product-events", event);
        
        return savedProduct;
    }

    /**
     * Delete Product
     * 
     * <p>Soft deletes product (marks as inactive).</p>
     * 
     * @param productId Product ID
     * @param reason Reason for deletion
     * @param userId User deleting product
     */
    @Transactional
    public void deleteProduct(String productId, String reason, String userId) {
        logger.info("Deleting product: {}", productId);
        
        // Load current product
        Product currentProduct = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        
        // Get next version
        Long nextVersion = currentProduct.getVersion() + 1;
        
        // Create ProductDeletedEvent
        ProductDeletedEvent event = ProductDeletedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .aggregateId(productId)
            .aggregateType("Product")
            .eventType("ProductDeletedEvent")
            .version(nextVersion)
            .timestamp(Instant.now())
            .userId(userId)
            .reason(reason)
            .softDelete(true)  // Soft delete
            .build();
        
        // Save event
        eventStoreRepository.save(event);
        logger.debug("Saved ProductDeletedEvent: {}", event.getEventId());
        
        // Apply event (mark inactive)
        applyProductDeletedEvent(currentProduct, event);
        
        // Save projection
        productRepository.save(currentProduct);
        logger.info("Product deleted successfully: {}", productId);
        
        // Publish event (TODO)
        // kafkaTemplate.send("product-events", event);
    }

    /**
     * Rebuild Product from Events
     * 
     * <p>Reconstructs product state by replaying all events.
     * Used for testing, debugging, or rebuilding corrupted projections.</p>
     * 
     * @param productId Product ID
     * @return Reconstructed product
     */
    public Product rebuildFromEvents(String productId) {
        logger.info("Rebuilding product from events: {}", productId);
        
        // Load all events
        List<BaseEvent> events = eventStoreRepository.findByAggregateIdOrderByVersionAsc(productId);
        
        if (events.isEmpty()) {
            throw new RuntimeException("No events found for product: " + productId);
        }
        
        // Create empty product
        Product product = new Product();
        
        // Apply events in order
        for (BaseEvent event : events) {
            if (event instanceof ProductCreatedEvent created) {
                product = applyProductCreatedEvent(created);
            } else if (event instanceof ProductUpdatedEvent updated) {
                applyProductUpdatedEvent(product, updated);
            } else if (event instanceof PriceChangedEvent priceChanged) {
                applyPriceChangedEvent(product, priceChanged);
            } else if (event instanceof StockChangedEvent stockChanged) {
                applyStockChangedEvent(product, stockChanged);
            } else if (event instanceof ProductDeletedEvent deleted) {
                applyProductDeletedEvent(product, deleted);
            }
        }
        
        logger.info("Product rebuilt from {} events", events.size());
        return product;
    }

    // ============= Event Application Methods =============

    private Product applyProductCreatedEvent(ProductCreatedEvent event) {
        return Product.builder()
            .productId(event.getAggregateId())
            .name(event.getName())
            .description(event.getDescription())
            .price(event.getPrice())
            .categoryId(event.getCategoryId())
            .sku(event.getSku())
            .brand(event.getBrand())
            .stock(event.getStock())
            .weight(event.getWeight())
            .dimensions(event.getDimensions())
            .images(event.getImages())
            .attributes(event.getAttributes())
            .tags(event.getTags())
            .active(event.getActive())
            .featured(event.getFeatured())
            .version(event.getVersion())
            .createdAt(event.getTimestamp())
            .updatedAt(event.getTimestamp())
            .build();
    }

    private void applyProductUpdatedEvent(Product product, ProductUpdatedEvent event) {
        if (event.getName() != null) product.setName(event.getName());
        if (event.getDescription() != null) product.setDescription(event.getDescription());
        if (event.getPrice() != null) product.setPrice(event.getPrice());
        if (event.getCategoryId() != null) product.setCategoryId(event.getCategoryId());
        if (event.getBrand() != null) product.setBrand(event.getBrand());
        if (event.getWeight() != null) product.setWeight(event.getWeight());
        if (event.getDimensions() != null) product.setDimensions(event.getDimensions());
        if (event.getImages() != null) product.setImages(event.getImages());
        if (event.getAttributes() != null) product.setAttributes(event.getAttributes());
        if (event.getTags() != null) product.setTags(event.getTags());
        if (event.getActive() != null) product.setActive(event.getActive());
        if (event.getFeatured() != null) product.setFeatured(event.getFeatured());
        product.setVersion(event.getVersion());
        product.setUpdatedAt(event.getTimestamp());
    }

    private void applyPriceChangedEvent(Product product, PriceChangedEvent event) {
        product.setPrice(event.getNewPrice());
        product.setVersion(event.getVersion());
        product.setUpdatedAt(event.getTimestamp());
    }

    private void applyStockChangedEvent(Product product, StockChangedEvent event) {
        product.setStock(event.getNewStock());
        product.setVersion(event.getVersion());
        product.setUpdatedAt(event.getTimestamp());
    }

    private void applyProductDeletedEvent(Product product, ProductDeletedEvent event) {
        if (event.getSoftDelete()) {
            product.setActive(false);
        }
        product.setVersion(event.getVersion());
        product.setUpdatedAt(event.getTimestamp());
    }

    // ============= Validation Methods =============

    private void validateProductCreation(Product product) {
        if (product.getName() == null || product.getName().isEmpty()) {
            throw new RuntimeException("Product name is required");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Product price must be positive");
        }
        if (product.getCategoryId() == null || product.getCategoryId().isEmpty()) {
            throw new RuntimeException("Category ID is required");
        }
        if (product.getSku() == null || product.getSku().isEmpty()) {
            throw new RuntimeException("SKU is required");
        }
        if (productRepository.existsBySku(product.getSku())) {
            throw new RuntimeException("SKU already exists: " + product.getSku());
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new RuntimeException("Stock cannot be negative");
        }
    }
}

