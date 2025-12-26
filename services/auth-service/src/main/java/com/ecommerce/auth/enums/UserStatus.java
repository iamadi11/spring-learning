package com.ecommerce.auth.enums;

/**
 * User Status Enum
 * 
 * <p>Represents the current status of a user account in the system.
 * User status determines whether the user can access the platform and
 * what actions they can perform.</p>
 * 
 * <h2>Status Lifecycle:</h2>
 * <pre>
 * PENDING → ACTIVE → INACTIVE
 *            ↓          ↑
 *        SUSPENDED ←────┘
 *            ↓
 *         BANNED
 * 
 * Registration:
 * 1. User registers → Status: PENDING
 * 2. Email verification sent
 * 3. User clicks verification link → Status: ACTIVE
 * 
 * Normal Usage:
 * - User logs in, uses platform → Status: ACTIVE
 * 
 * Inactivity:
 * - User doesn't login for 180 days → Status: INACTIVE
 * - Can be reactivated on next login
 * 
 * Policy Violation:
 * - Spam, abuse, etc. → Status: SUSPENDED (temporary)
 * - Review after period → ACTIVE or BANNED
 * 
 * Severe Violation:
 * - Fraud, illegal activity → Status: BANNED (permanent)
 * - Cannot reactivate, must create new account
 * </pre>
 * 
 * <h2>Status Checks in Code:</h2>
 * <pre>
 * // In UserDetails implementation (User entity):
 * @Override
 * public boolean isAccountNonLocked() {
 *     return status == UserStatus.ACTIVE;  // Only ACTIVE users can login
 * }
 * 
 * @Override
 * public boolean isEnabled() {
 *     return status == UserStatus.ACTIVE && emailVerified;
 * }
 * 
 * // In service methods:
 * public void performSensitiveAction(User user) {
 *     if (user.getStatus() != UserStatus.ACTIVE) {
 *         throw new AccountSuspendedException("Account not active");
 *     }
 *     // Proceed with action
 * }
 * 
 * // Scheduled task to mark inactive users:
 * @Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
 * public void markInactiveUsers() {
 *     LocalDateTime threshold = LocalDateTime.now().minusDays(180);
 *     userRepository.findByLastLoginBeforeAndStatus(threshold, UserStatus.ACTIVE)
 *         .forEach(user -> {
 *             user.setStatus(UserStatus.INACTIVE);
 *             userRepository.save(user);
 *             emailService.sendInactivityNotice(user);
 *         });
 * }
 * </pre>
 * 
 * <h2>Database Queries:</h2>
 * <pre>
 * // Find all active users
 * List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
 * 
 * // Count users by status
 * Map<UserStatus, Long> statusCounts = userRepository.findAll().stream()
 *     .collect(Collectors.groupingBy(User::getStatus, Collectors.counting()));
 * 
 * // Find users needing review
 * List<User> suspended = userRepository.findBySuspendedEndDateBefore(LocalDateTime.now());
 * </pre>
 * 
 * <h2>API Response:</h2>
 * <pre>
 * GET /api/users/me
 * Response: {
 *   "id": 123,
 *   "email": "user@example.com",
 *   "status": "ACTIVE",
 *   "statusDescription": "Account is active and in good standing"
 * }
 * 
 * POST /api/auth/login
 * Error Response (if suspended): {
 *   "status": 403,
 *   "message": "Account suspended until 2024-02-01",
 *   "userStatus": "SUSPENDED"
 * }
 * </pre>
 * 
 * <h2>Admin Actions:</h2>
 * <pre>
 * // Suspend user (requires ADMIN role)
 * PUT /api/admin/users/{id}/suspend
 * Request: {
 *   "reason": "Spam behavior detected",
 *   "duration": 7,  // days
 *   "notifyUser": true
 * }
 * 
 * // Ban user (requires ADMIN role)
 * PUT /api/admin/users/{id}/ban
 * Request: {
 *   "reason": "Fraudulent transactions",
 *   "permanent": true
 * }
 * 
 * // Reactivate user (requires ADMIN role)
 * PUT /api/admin/users/{id}/activate
 * </pre>
 * 
 * @author E-commerce Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum UserStatus {
    
    /**
     * Pending status - newly registered, awaiting email verification
     * 
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Account created but not yet verified</li>
     *   <li>Cannot login until email verified</li>
     *   <li>Email verification link sent</li>
     *   <li>Link expires after 24 hours</li>
     * </ul>
     * 
     * <p><b>Transitions to ACTIVE:</b></p>
     * <ul>
     *   <li>User clicks email verification link</li>
     *   <li>Admin manually activates account</li>
     *   <li>OAuth registration (email pre-verified)</li>
     * </ul>
     * 
     * <p><b>User Experience:</b></p>
     * <pre>
     * Login attempt:
     * - Error: "Please verify your email address to activate your account"
     * - Button: "Resend verification email"
     * </pre>
     */
    PENDING,
    
    /**
     * Active status - account verified and in good standing
     * 
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Email verified</li>
     *   <li>Can login and use all features</li>
     *   <li>No restrictions on account</li>
     *   <li>Regular user status</li>
     * </ul>
     * 
     * <p><b>Required for:</b></p>
     * <ul>
     *   <li>Placing orders</li>
     *   <li>Writing reviews</li>
     *   <li>Accessing account settings</li>
     *   <li>Making payments</li>
     * </ul>
     */
    ACTIVE,
    
    /**
     * Inactive status - account not used for extended period
     * 
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>No login activity for 180+ days</li>
     *   <li>Account preserved but dormant</li>
     *   <li>Can be reactivated on next login</li>
     *   <li>Periodic cleanup after 2+ years</li>
     * </ul>
     * 
     * <p><b>Reactivation:</b></p>
     * <pre>
     * User attempts login:
     * 1. Check if INACTIVE
     * 2. If yes, verify identity (password/2FA)
     * 3. Change status to ACTIVE
     * 4. Log reactivation event
     * 5. Send welcome back email
     * </pre>
     * 
     * <p><b>Benefits:</b></p>
     * <ul>
     *   <li>Identify dormant accounts</li>
     *   <li>Free up resources (cache, sessions)</li>
     *   <li>GDPR compliance (data retention)</li>
     *   <li>Security (prevent stale account breaches)</li>
     * </ul>
     */
    INACTIVE,
    
    /**
     * Suspended status - temporary account restriction
     * 
     * <p><b>Reasons for Suspension:</b></p>
     * <ul>
     *   <li>Policy violation (spam, abuse)</li>
     *   <li>Suspicious activity (potential hack)</li>
     *   <li>Multiple failed payment attempts</li>
     *   <li>Too many login failures</li>
     *   <li>Pending investigation</li>
     * </ul>
     * 
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Temporary restriction (7-30 days)</li>
     *   <li>Cannot login during suspension</li>
     *   <li>Can appeal the suspension</li>
     *   <li>Data preserved</li>
     * </ul>
     * 
     * <p><b>Suspension Flow:</b></p>
     * <pre>
     * 1. Admin/System suspends account:
     *    user.setStatus(UserStatus.SUSPENDED);
     *    user.setSuspensionReason("Spam behavior");
     *    user.setSuspensionEndDate(LocalDateTime.now().plusDays(7));
     * 
     * 2. User tries to login:
     *    throw new AccountSuspendedException(
     *        "Account suspended until " + user.getSuspensionEndDate()
     *    );
     * 
     * 3. After suspension period:
     *    Scheduled task automatically changes status to ACTIVE
     *    Send reactivation email to user
     * 
     * 4. User appeals:
     *    Admin reviews case
     *    Either extend suspension or reactivate early
     * </pre>
     */
    SUSPENDED,
    
    /**
     * Banned status - permanent account termination
     * 
     * <p><b>Reasons for Ban:</b></p>
     * <ul>
     *   <li>Severe policy violation</li>
     *   <li>Fraudulent activity</li>
     *   <li>Illegal transactions</li>
     *   <li>Repeated suspensions</li>
     *   <li>Chargebacks/payment fraud</li>
     * </ul>
     * 
     * <p><b>Characteristics:</b></p>
     * <ul>
     *   <li>Permanent restriction</li>
     *   <li>Cannot login or create new account with same email</li>
     *   <li>Can appeal but rarely reversed</li>
     *   <li>Order history preserved for records</li>
     * </ul>
     * 
     * <p><b>Ban Enforcement:</b></p>
     * <pre>
     * // Check for ban during registration
     * if (bannedEmailRepository.existsByEmail(newUser.getEmail())) {
     *     throw new BannedEmailException("This email is banned from registration");
     * }
     * 
     * // Check for ban during login
     * if (user.getStatus() == UserStatus.BANNED) {
     *     log.warn("Banned user attempted login: {}", user.getEmail());
     *     throw new AccountBannedException(
     *         "Account permanently banned. Contact support for more information."
     *     );
     * }
     * 
     * // Prevent ban circumvention
     * - Block IP address (temporarily)
     * - Flag device fingerprint
     * - Monitor for similar accounts (name, address, payment method)
     * </pre>
     * 
     * <p><b>Legal Considerations:</b></p>
     * <ul>
     *   <li>Provide clear reason for ban</li>
     *   <li>Allow data export (GDPR)</li>
     *   <li>Preserve evidence of violation</li>
     *   <li>Follow terms of service</li>
     * </ul>
     */
    BANNED
}

