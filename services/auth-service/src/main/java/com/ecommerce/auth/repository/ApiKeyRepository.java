package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.ApiKey;
import com.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * API Key Repository - Data access layer for ApiKey entity
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    
    // Find API key by hash (for authentication)
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    // Find API key by prefix (for faster lookup)
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);
    
    // Find all API keys for a user
    List<ApiKey> findByUser(User user);
    
    // Find active API keys for a user
    List<ApiKey> findByUserAndActiveTrue(User user);
    
    // Count active API keys for a user
    long countByUserAndActiveTrue(User user);
}

