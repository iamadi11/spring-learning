package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token Repository - Data access layer for RefreshToken entity
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    
    // Find refresh token by token value
    Optional<RefreshToken> findByToken(String token);
    
    // Find all tokens for a user
    List<RefreshToken> findByUser(User user);
    
    // Find valid (not revoked, not expired) tokens for a user
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    // Revoke all tokens for a user (logout from all devices)
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllUserTokens(@Param("user") User user);
    
    // Delete expired tokens (cleanup job)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    // Delete revoked tokens older than certain date (cleanup)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt < :date")
    void deleteOldRevokedTokens(@Param("date") LocalDateTime date);
}

