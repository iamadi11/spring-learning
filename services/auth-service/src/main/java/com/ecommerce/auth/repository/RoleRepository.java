package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Role Repository - Data access layer for Role entity
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    // Find role by name (e.g., "ADMIN", "USER", "PRODUCT_MANAGER")
    Optional<Role> findByName(String name);
    
    // Check if role exists by name
    boolean existsByName(String name);
    
    // Find multiple roles by names (for bulk operations)
    List<Role> findByNameIn(List<String> names);
}

