package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Permission Entity - Represents a fine-grained permission in the system
 * 
 * <p>Permissions represent specific actions that can be performed in the system.
 * They are the most granular level of access control in RBAC.</p>
 * 
 * <h2>Permission Naming Convention:</h2>
 * <pre>
 * Format: {RESOURCE}_{ACTION}
 * 
 * Examples:
 * - PRODUCT_CREATE: Can create products
 * - PRODUCT_UPDATE: Can update products
 * - PRODUCT_DELETE: Can delete products
 * - PRODUCT_VIEW: Can view products
 * - ORDER_CREATE: Can create orders
 * - ORDER_VIEW: Can view orders
 * - ORDER_UPDATE: Can update order status
 * - ORDER_CANCEL: Can cancel orders
 * - USER_CREATE: Can create users
 * - USER_UPDATE: Can update users
 * - USER_DELETE: Can delete users
 * - INVENTORY_MANAGE: Can manage inventory
 * - PAYMENT_PROCESS: Can process payments
 * - REPORT_VIEW: Can view reports
 * </pre>
 * 
 * <h2>How Permissions Work:</h2>
 * <pre>
 * 1. Define Permissions:
 *    PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE
 * 
 * 2. Assign to Roles:
 *    PRODUCT_MANAGER role gets:
 *      - PRODUCT_CREATE
 *      - PRODUCT_UPDATE
 *      - PRODUCT_DELETE
 * 
 * 3. Assign Roles to Users:
 *    User "alice@example.com" has PRODUCT_MANAGER role
 * 
 * 4. Authorization Check:
 *    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
 *    public Product createProduct(...) {
 *        // Alice can execute this because she has PRODUCT_MANAGER role
 *        // which includes PRODUCT_CREATE permission
 *    }
 * </pre>
 * 
 * <h2>Benefits of Fine-Grained Permissions:</h2>
 * <ul>
 *   <li><b>Flexibility:</b> Can create custom roles by combining permissions</li>
 *   <li><b>Security:</b> Principle of least privilege - give only needed access</li>
 *   <li><b>Audit:</b> Track exactly what actions a user can perform</li>
 *   <li><b>Compliance:</b> Meets regulatory requirements for access control</li>
 * </ul>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA annotation: marks this class as a database entity
@Table(name = "permissions", indexes = {
    // Index for fast permission lookup by name
    @Index(name = "idx_permission_name", columnList = "name")
})
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
public class Permission {

    /**
     * Unique identifier for the permission
     * Using UUID for distributed system compatibility
     */
    @Id  // JPA: primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Permission name - must be unique
     * 
     * <p>Convention: {RESOURCE}_{ACTION} in UPPER_CASE</p>
     * <p>Examples: PRODUCT_CREATE, ORDER_VIEW, USER_DELETE</p>
     * 
     * <p>In Spring Security authorization:</p>
     * <pre>
     * // Check single permission
     * @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
     * 
     * // Check multiple permissions (any of)
     * @PreAuthorize("hasAnyAuthority('PRODUCT_CREATE', 'PRODUCT_UPDATE')")
     * 
     * // Check multiple permissions (all of)
     * @PreAuthorize("hasAuthority('PRODUCT_CREATE') and hasAuthority('INVENTORY_MANAGE')")
     * </pre>
     */
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    /**
     * Human-readable description of the permission
     * 
     * <p>Explains what this permission allows the user to do.
     * Displayed in admin UI when managing roles.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>PRODUCT_CREATE: "Allows creating new products in the catalog"</li>
     *   <li>ORDER_CANCEL: "Allows canceling customer orders"</li>
     *   <li>REPORT_VIEW: "Allows viewing sales and analytics reports"</li>
     * </ul>
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Category/module this permission belongs to
     * 
     * <p>Groups related permissions together for easier management.
     * Used for UI organization in admin panel.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>PRODUCT: All product-related permissions</li>
     *   <li>ORDER: All order-related permissions</li>
     *   <li>USER: All user-management permissions</li>
     *   <li>INVENTORY: All inventory-related permissions</li>
     *   <li>PAYMENT: All payment-related permissions</li>
     *   <li>REPORT: All reporting permissions</li>
     * </ul>
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * Timestamp when the permission was created
     * 
     * <p>Automatically set by Hibernate when entity is first persisted.
     * Useful for tracking when new permissions are added to the system.</p>
     */
    @CreationTimestamp  // Hibernate: auto-set on insert
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

