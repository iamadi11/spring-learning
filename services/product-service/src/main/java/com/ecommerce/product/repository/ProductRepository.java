package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Repository
 * 
 * <p>Repository for querying product projections (read model).
 * This is the READ side of CQRS.</p>
 * 
 * <h2>CQRS Read Model:</h2>
 * <pre>
 * Event Store (Write Side)     Product Collection (Read Side)
 * ┌────────────────────┐       ┌──────────────────────┐
 * │ ProductCreated     │───────>│ Product Document     │
 * │ PriceChanged       │ Apply  │ (Current State)      │
 * │ StockChanged       │───────>│ Optimized for Reads  │
 * └────────────────────┘       └──────────────────────┘
 * 
 * Write Operations → Event Store
 * Read Operations → Product Collection
 * 
 * Benefits:
 * - Fast reads (no event replay)
 * - Optimized indexes for queries
 * - Separate scaling
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * Find by SKU
     */
    Optional<Product> findBySku(String sku);

    /**
     * Find active products by category
     */
    Page<Product> findByCategoryIdAndActive(String categoryId, Boolean active, Pageable pageable);

    /**
     * Find featured products
     */
    Page<Product> findByFeaturedAndActive(Boolean featured, Boolean active, Pageable pageable);

    /**
     * Find products in price range
     */
    @Query("{ 'price': { $gte: ?0, $lte: ?1 }, 'active': true }")
    Page<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find out-of-stock products
     */
    @Query("{ 'stock': { $lte: 0 }, 'active': true }")
    Page<Product> findOutOfStockProducts(Pageable pageable);

    /**
     * Find low-stock products
     */
    @Query("{ 'stock': { $gt: 0, $lte: ?0 }, 'active': true }")
    Page<Product> findLowStockProducts(Integer threshold, Pageable pageable);

    /**
     * Search products by name or description
     */
    @Query("{ $text: { $search: ?0 }, 'active': true }")
    Page<Product> searchProducts(String searchText, Pageable pageable);

    /**
     * Count products by category
     */
    long countByCategoryId(String categoryId);

    /**
     * Check if SKU exists
     */
    boolean existsBySku(String sku);
}

