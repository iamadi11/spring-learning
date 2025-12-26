package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant Repository - Data access layer for Tenant entity
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
    
    // Find tenant by name
    Optional<Tenant> findByName(String name);
    
    // Find tenant by slug (URL-friendly identifier)
    Optional<Tenant> findBySlug(String slug);
    
    // Find tenant by domain
    Optional<Tenant> findByDomain(String domain);
    
    // Check if tenant exists by slug
    boolean existsBySlug(String slug);
    
    // Find all active tenants
    List<Tenant> findByActiveTrue();
    
    // Find tenants by subscription plan
    List<Tenant> findByPlan(String plan);
}

