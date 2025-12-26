package com.ecommerce.notification.repository;

import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.entity.NotificationStatus;
import com.ecommerce.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification Repository
 * 
 * @author E-commerce Platform Team
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Find user notifications
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find unread notifications
     */
    List<Notification> findByUserIdAndStatusNot(Long userId, NotificationStatus status);

    /**
     * Find pending notifications for retry
     */
    List<Notification> findByStatusAndCreatedAtBefore(
        NotificationStatus status, LocalDateTime before);

    /**
     * Count unread by user
     */
    long countByUserIdAndStatusNot(Long userId, NotificationStatus status);

    /**
     * Find by type and status
     */
    List<Notification> findByTypeAndStatus(NotificationType type, NotificationStatus status);
}

