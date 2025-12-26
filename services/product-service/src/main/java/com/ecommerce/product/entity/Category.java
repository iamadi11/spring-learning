package com.ecommerce.product.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Category Entity
 * 
 * <p>Represents a product category in hierarchical tree structure.</p>
 * 
 * <h2>Category Hierarchy Example:</h2>
 * <pre>
 * Electronics (parent: null)
 *   ├─ Computers (parent: Electronics)
 *   │   ├─ Laptops (parent: Computers)
 *   │   └─ Desktops (parent: Computers)
 *   └─ Phones (parent: Electronics)
 *       ├─ Smartphones (parent: Phones)
 *       └─ Feature Phones (parent: Phones)
 * 
 * Benefits:
 * - Organize products logically
 * - Browse by category
 * - Faceted navigation
 * - SEO-friendly URLs
 * </pre>
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "categories")
public class Category {

    @Id
    private String categoryId;

    /**
     * Category name
     */
    private String name;

    /**
     * Category description
     */
    private String description;

    /**
     * URL slug (e.g., "electronics", "laptops")
     */
    @Indexed(unique = true)
    private String slug;

    /**
     * Parent category ID (null for root categories)
     */
    @Indexed
    private String parentId;

    /**
     * Category level (0 = root, 1 = child, etc.)
     */
    private Integer level;

    /**
     * Category image URL
     */
    private String imageUrl;

    /**
     * Is category active?
     */
    @Indexed
    private Boolean active;

    /**
     * Display order
     */
    private Integer displayOrder;

    /**
     * Number of products in this category
     */
    private Integer productCount;

    /**
     * Child category IDs
     */
    @Builder.Default
    private List<String> childrenIds = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

