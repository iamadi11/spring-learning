package com.ecommerce.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Tenant Entity - Represents an organization/company in a multi-tenant system
 * 
 * <p>Multi-tenancy allows multiple organizations to use the same application instance
 * while keeping their data completely isolated. Each tenant has separate users, orders,
 * products, etc.</p>
 * 
 * <h2>Multi-Tenancy Models:</h2>
 * <pre>
 * 1. Database per Tenant (Highest Isolation):
 *    - Each tenant has separate database
 *    - Complete data isolation
 *    - Easy to backup/restore per tenant
 *    - Complex: Manage many databases
 * 
 * 2. Schema per Tenant (Moderate Isolation):
 *    - Each tenant has separate schema in same database
 *    - Good isolation with simpler management
 *    - Schema = namespace for tables
 * 
 * 3. Shared Schema with Tenant ID (Low Isolation):★ WE USE THIS
 *    - All tenants share same tables
 *    - Every row has tenant_id column
 *    - Filter all queries by tenant_id
 *    - Simplest to implement and scale
 * </pre>
 * 
 * <h2>How Tenant Isolation Works:</h2>
 * <pre>
 * 1. User Authentication:
 *    - User logs in: john@companyA.com
 *    - JWT token includes: tenant_id = "tenant-123"
 * 
 * 2. Request Processing:
 *    - Gateway validates JWT
 *    - Extracts tenant_id from token
 *    - Adds tenant_id to request context (ThreadLocal)
 * 
 * 3. Database Query:
 *    // Automatic tenant filtering with Hibernate Filter
 *    SELECT * FROM orders 
 *    WHERE tenant_id = 'tenant-123'  // Auto-added by filter
 *    AND user_id = 'user-456'
 * 
 * 4. Cross-Tenant Access Prevention:
 *    - User from Tenant A cannot see Tenant B data
 *    - Enforced at database query level
 *    - Additional check in service layer
 * </pre>
 * 
 * <h2>SaaS Business Model:</h2>
 * <ul>
 *   <li><b>Tenant = Customer Company:</b> Each customer is a tenant</li>
 *   <li><b>User Accounts:</b> Users belong to a tenant</li>
 *   <li><b>Billing:</b> Bill per tenant (not per user)</li>
 *   <li><b>Subscription Plans:</b> Each tenant has a plan (Free, Pro, Enterprise)</li>
 *   <li><b>Feature Flags:</b> Enable/disable features per tenant</li>
 * </ul>
 * 
 * <h2>Example Tenant Setup:</h2>
 * <pre>
 * Tenant A: "Acme Corp"
 *   - tenant_id: "tenant-123"
 *   - Users: alice@acme.com, bob@acme.com
 *   - Orders: 1000+ orders for Acme customers
 *   - Products: Acme's product catalog
 *   - Plan: Enterprise ($1000/month)
 * 
 * Tenant B: "Widget Inc"
 *   - tenant_id: "tenant-456"
 *   - Users: charlie@widget.com, diana@widget.com
 *   - Orders: 500+ orders for Widget customers
 *   - Products: Widget's product catalog
 *   - Plan: Pro ($100/month)
 * 
 * Data Isolation:
 *   - Alice (Tenant A) CANNOT see Widget Inc's data
 *   - Charlie (Tenant B) CANNOT see Acme Corp's data
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity  // JPA annotation: marks this class as a database entity
@Table(name = "tenants", indexes = {
    // Index for fast tenant lookup by name/slug
    @Index(name = "idx_tenant_name", columnList = "name"),
    @Index(name = "idx_tenant_slug", columnList = "slug")
})
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor  // Lombok: generates no-args constructor (required by JPA)
@AllArgsConstructor  // Lombok: generates all-args constructor
public class Tenant {

    /**
     * Unique identifier for the tenant
     * This ID is included in JWT tokens and used for data filtering
     */
    @Id  // JPA: primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    /**
     * Tenant name - the organization/company name
     * 
     * <p>Examples: "Acme Corporation", "Widget Inc", "Tech Startup LLC"</p>
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * URL-friendly tenant identifier
     * 
     * <p>Used in subdomain-based routing (if implemented):
     * - acme.example.com → tenant slug = "acme"
     * - widget.example.com → tenant slug = "widget"</p>
     * 
     * <p>Also used for custom domains</p>
     */
    @Column(name = "slug", unique = true, nullable = false, length = 100)
    private String slug;

    /**
     * Tenant domain/website
     * 
     * <p>Example: "acme.com", "widget.io"</p>
     * <p>Can be used for email domain verification</p>
     */
    @Column(name = "domain", length = 255)
    private String domain;

    /**
     * Primary contact email for the tenant
     * 
     * <p>Used for:</p>
     * <ul>
     *   <li>Billing notifications</li>
     *   <li>System announcements</li>
     *   <li>Support communications</li>
     * </ul>
     */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /**
     * Subscription plan level
     * 
     * <p>Determines features and limits available to tenant</p>
     * 
     * <p>Example plans:</p>
     * <ul>
     *   <li><b>FREE:</b> 10 products, 50 orders/month, basic features</li>
     *   <li><b>PRO:</b> 1000 products, 1000 orders/month, advanced features</li>
     *   <li><b>ENTERPRISE:</b> Unlimited, all features, priority support</li>
     * </ul>
     */
    @Column(name = "plan", length = 50)
    private String plan = "FREE";

    /**
     * Whether the tenant account is active
     * 
     * <p>false: Tenant suspended (users cannot login, data inaccessible)
     * true: Tenant active and operational</p>
     * 
     * <p>Suspension reasons:</p>
     * <ul>
     *   <li>Payment failure</li>
     *   <li>Terms of service violation</li>
     *   <li>Voluntary account pause</li>
     * </ul>
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Maximum number of users allowed for this tenant
     * 
     * <p>Enforced by subscription plan:</p>
     * <ul>
     *   <li>FREE: 5 users</li>
     *   <li>PRO: 50 users</li>
     *   <li>ENTERPRISE: Unlimited (null or very large number)</li>
     * </ul>
     */
    @Column(name = "max_users")
    private Integer maxUsers;

    /**
     * Current number of users in this tenant
     * 
     * <p>Updated when users are added/removed</p>
     * <p>Used to enforce max_users limit</p>
     */
    @Column(name = "current_users")
    private Integer currentUsers = 0;

    /**
     * Tenant-specific settings (JSON format)
     * 
     * <p>Flexible field for tenant-specific configurations:</p>
     * <pre>
     * {
     *   "features": ["advanced_analytics", "api_access"],
     *   "branding": {
     *     "logo_url": "https://cdn.example.com/acme-logo.png",
     *     "primary_color": "#007bff"
     *   },
     *   "integrations": {
     *     "stripe_account": "acct_xxx",
     *     "sendgrid_api_key": "SG.xxx"
     *   }
     * }
     * </pre>
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /**
     * Subscription start date
     * 
     * <p>When tenant subscribed to paid plan</p>
     */
    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    /**
     * Subscription end date
     * 
     * <p>When subscription expires (for fixed-term subscriptions)</p>
     * <p>null = ongoing subscription</p>
     */
    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    /**
     * Timestamp when the tenant was created
     */
    @CreationTimestamp  // Hibernate: auto-set on insert
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the tenant was last updated
     */
    @UpdateTimestamp  // Hibernate: auto-update on modify
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== Helper Methods ====================

    /**
     * Check if tenant can add more users
     * 
     * @return true if under user limit
     */
    public boolean canAddUsers() {
        if (maxUsers == null) {
            return true;  // Unlimited
        }
        return currentUsers < maxUsers;
    }

    /**
     * Increment user count
     */
    public void incrementUserCount() {
        this.currentUsers++;
    }

    /**
     * Decrement user count
     */
    public void decrementUserCount() {
        if (this.currentUsers > 0) {
            this.currentUsers--;
        }
    }

    /**
     * Check if subscription is active
     * 
     * @return true if within subscription period
     */
    public boolean isSubscriptionActive() {
        if (subscriptionEndDate == null) {
            return true;  // Ongoing subscription
        }
        return LocalDateTime.now().isBefore(subscriptionEndDate);
    }

    /**
     * Check if tenant can access the system
     * 
     * @return true if active and subscription valid
     */
    public boolean canAccess() {
        return active && isSubscriptionActive();
    }
}

