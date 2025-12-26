package com.ecommerce.product.service;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.event.BaseEvent;
import com.ecommerce.product.repository.EventStoreRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Query Service
 * 
 * <p>Handles all read operations (queries) for products.
 * This is the READ side of CQRS.</p>
 * 
 * <h2>CQRS Query Side:</h2>
 * <pre>
 * Responsibilities:
 * - Read from projection (products collection)
 * - Cache results in Redis
 * - Optimize for fast reads
 * - No state modifications
 * 
 * Benefits:
 * - Fast queries (no event replay)
 * - Separate scaling from writes
 * - Multiple read models possible
 * - Cache-friendly
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Service
public class ProductQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ProductQueryService.class);

    private final ProductRepository productRepository;
    private final EventStoreRepository eventStoreRepository;

    @Autowired
    public ProductQueryService(
            ProductRepository productRepository,
            EventStoreRepository eventStoreRepository) {
        this.productRepository = productRepository;
        this.eventStoreRepository = eventStoreRepository;
    }

    /**
     * Get Product by ID
     * 
     * <p>Retrieves product from projection with Redis caching.</p>
     * 
     * @param productId Product ID
     * @return Optional containing product if found
     */
    @Cacheable(value = "products", key = "#productId")
    public Optional<Product> getProduct(String productId) {
        logger.debug("Fetching product: {}", productId);
        return productRepository.findById(productId);
    }

    /**
     * Get Product by SKU
     */
    @Cacheable(value = "productsBySku", key = "#sku")
    public Optional<Product> getProductBySku(String sku) {
        logger.debug("Fetching product by SKU: {}", sku);
        return productRepository.findBySku(sku);
    }

    /**
     * Get Products by Category
     */
    @Cacheable(value = "categoryProducts", key = "#categoryId + '_' + #pageable.pageNumber")
    public Page<Product> getProductsByCategory(String categoryId, Pageable pageable) {
        logger.debug("Fetching products for category: {}", categoryId);
        return productRepository.findByCategoryIdAndActive(categoryId, true, pageable);
    }

    /**
     * Get Featured Products
     */
    @Cacheable(value = "featuredProducts", key = "#pageable.pageNumber")
    public Page<Product> getFeaturedProducts(Pageable pageable) {
        logger.debug("Fetching featured products");
        return productRepository.findByFeaturedAndActive(true, true, pageable);
    }

    /**
     * Search Products
     */
    @Cacheable(value = "productSearch", key = "#searchText + '_' + #pageable.pageNumber")
    public Page<Product> searchProducts(String searchText, Pageable pageable) {
        logger.debug("Searching products: {}", searchText);
        return productRepository.searchProducts(searchText, pageable);
    }

    /**
     * Get Products by Price Range
     */
    public Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        logger.debug("Fetching products in price range: {} - {}", minPrice, maxPrice);
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }

    /**
     * Get Out-of-Stock Products
     */
    public Page<Product> getOutOfStockProducts(Pageable pageable) {
        logger.debug("Fetching out-of-stock products");
        return productRepository.findOutOfStockProducts(pageable);
    }

    /**
     * Get Low-Stock Products
     */
    public Page<Product> getLowStockProducts(Integer threshold, Pageable pageable) {
        logger.debug("Fetching low-stock products (threshold: {})", threshold);
        return productRepository.findLowStockProducts(threshold, pageable);
    }

    /**
     * Get Product Event History
     * 
     * <p>Returns complete event history for a product.
     * Used for audit trail and debugging.</p>
     * 
     * @param productId Product ID
     * @return List of all events for product
     */
    public List<BaseEvent> getProductHistory(String productId) {
        logger.debug("Fetching event history for product: {}", productId);
        return eventStoreRepository.findByAggregateIdOrderByTimestampAsc(productId);
    }

    /**
     * Count Products by Category
     */
    public long countProductsByCategory(String categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }

    /**
     * Check if SKU Exists
     */
    public boolean skuExists(String sku) {
        return productRepository.existsBySku(sku);
    }
}

