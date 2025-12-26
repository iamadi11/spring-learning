package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role Entity - Represents a user role in the RBAC (Role-Based Access Control) system
 * 
 * <p>Roles are collections of permissions. Users are assigned roles, and through these
 * roles, they inherit the associated permissions. This implements the RBAC pattern
 * which is standard in enterprise applications.</p>
 * 
 * <h2>RBAC Hierarchy:</h2>
 * <pre>
 * User → Role → Permission
 * 
 * Example:
 * User: john@example.com
 *   ↓ has role
 * Role: PRODUCT_MANAGER
 *   ↓ has permissions
 * Permissions:
 *   - PRODUCT_CREATE
 *   - PRODUCT_UPDATE
 *   - PRODUCT_DELETE
 *   - INVENTORY_MANAGE
 * </pre>
 * 
 * <h2>Standard Roles in E-commerce:</h2>
 * <ul>
 *   <li><b>USER:</b> Basic customer role - can browse, order, review</li>
 *   <li><b>ADMIN:</b> Full system administrator - complete access</li>
 *   <li><b>PRODUCT_MANAGER:</b> Manages product catalog</li>
 *   <li><b>ORDER_MANAGER:</b> Manages orders and fulfillment</li>
 *   <li><b>CUSTOMER_SUPPORT:</b> Assists customers, views orders</li>
 *   <li><b>MARKETING:</b> Manages promotions, campaigns</li>
 *   <li><b>FINANCE:</b> Views financial reports, manages payments</li>
 * </ul>
 * 
 * <h2>Authorization Example:</h2>
 * <pre>
 * // Method-level security
 * @PreAuthorize("hasRole('PRODUCT_MANAGER')")
 * public Product createProduct(ProductRequest request) {
 *     // Only users with PRODUCT_MANAGER role can execute this
 *     return productService.create(request);
 * }
 * 
 * // Or check specific permission
 * @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
 * public Product createProduct(ProductRequest request) {
 *     // Only users with PRODUCT_CREATE permission can execute
 *     return productService.create(request);
 * }
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA annotation: marks this class as a database entity
@Table(name = "roles", indexes = {
    // Index for fast role lookup by name
    @Index(name = "idx_role_name", columnList = "name")
})
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
public class Role {

    /**
     * Unique identifier for the role
     * Using UUID for distributed system compatibility
     */
    @Id  // JPA: primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Role name - must be unique
     * 
     * <p>Convention: Use UPPER_CASE_WITH_UNDERSCORES</p>
     * <p>Examples: USER, ADMIN, PRODUCT_MANAGER, CUSTOMER_SUPPORT</p>
     * 
     * <p>In Spring Security, roles are prefixed with "ROLE_" when checking:
     * hasRole('ADMIN') checks for "ROLE_ADMIN" authority</p>
     */
    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    /**
     * Human-readable description of the role
     * 
     * <p>Displayed in admin UI when assigning roles to users</p>
     * <p>Example: "Product Manager - Can manage products and inventory"</p>
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Permissions associated with this role
     * 
     * <p>Many-to-Many relationship: A role can have multiple permissions,
     * and a permission can belong to multiple roles.</p>
     * 
     * <p>Join table: role_permissions</p>
     * <pre>
     * Columns:
     * - role_id: Foreign key to roles.id
     * - permission_id: Foreign key to permissions.id
     * </pre>
     * 
     * <p>Example role-permission mapping:</p>
     * <pre>
     * PRODUCT_MANAGER role has permissions:
     *   - PRODUCT_CREATE
     *   - PRODUCT_UPDATE
     *   - PRODUCT_DELETE
     *   - PRODUCT_VIEW
     *   - INVENTORY_MANAGE
     * 
     * USER role has permissions:
     *   - PRODUCT_VIEW
     *   - ORDER_CREATE
     *   - ORDER_VIEW (own orders only)
     *   - REVIEW_CREATE
     * </pre>
     * 
     * <p>Loading strategy: EAGER (permissions loaded with role)
     * Justification: Roles typically have few permissions, and we need them
     * for authorization checks, so eager loading is efficient</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",  // Join table name
        joinColumns = @JoinColumn(name = "role_id"),  // This entity's foreign key
        inverseJoinColumns = @JoinColumn(name = "permission_id")  // Other entity's foreign key
    )
    private Set<Permission> permissions = new HashSet<>();

    /**
     * Timestamp when the role was created
     * 
     * <p>Automatically set by Hibernate when entity is first persisted.
     * Useful for audit trails.</p>
     */
    @CreationTimestamp  // Hibernate: auto-set on insert
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the role was last updated
     * 
     * <p>Automatically updated by Hibernate whenever entity is modified.
     * Tracks when permissions were added/removed.</p>
     */
    @UpdateTimestamp  // Hibernate: auto-update on modify
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Helper Methods ====================

    /**
     * Add a permission to this role
     * 
     * @param permission Permission to add
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Remove a permission from this role
     * 
     * @param permission Permission to remove
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /**
     * Check if role has a specific permission
     * 
     * @param permissionName Permission name to check
     * @return true if role has the permission
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
            .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    /**
     * Get all permission names for this role
     * 
     * @return Set of permission names
     */
    public Set<String> getPermissionNames() {
        Set<String> names = new HashSet<>();
        for (Permission permission : permissions) {
            names.add(permission.getName());
        }
        return names;
    }
}

