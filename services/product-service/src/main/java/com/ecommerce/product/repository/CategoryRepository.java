package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Category Repository
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    /**
     * Find by slug
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Find root categories (no parent)
     */
    List<Category> findByParentIdIsNullAndActive(Boolean active);

    /**
     * Find child categories
     */
    List<Category> findByParentIdAndActive(String parentId, Boolean active);

    /**
     * Find categories by level
     */
    List<Category> findByLevelAndActive(Integer level, Boolean active);

    /**
     * Check if slug exists
     */
    boolean existsBySlug(String slug);
}

