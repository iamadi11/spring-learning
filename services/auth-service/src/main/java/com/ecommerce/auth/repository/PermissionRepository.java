package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Permission Repository - Data access layer for Permission entity
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    
    // Find permission by name (e.g., "PRODUCT_CREATE", "ORDER_VIEW")
    Optional<Permission> findByName(String name);
    
    // Find all permissions by category (e.g., "PRODUCT", "ORDER")
    List<Permission> findByCategory(String category);
    
    // Find multiple permissions by names
    List<Permission> findByNameIn(List<String> names);
}

