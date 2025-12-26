package com.ecommerce.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Notification Entity
 * 
 * <p>Stores notification history in MongoDB.</p>
 * 
 * @author E-commerce Platform Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private Long userId;
    
    private String recipient;  // Email address, phone number, or device token
    
    private NotificationType type;
    
    private NotificationPriority priority;
    
    private NotificationStatus status;
    
    private String subject;
    
    private String message;
    
    private Map<String, Object> data;  // Additional data
    
    private String templateId;  // Email template ID
    
    private Integer retryCount;
    
    private String errorMessage;
    
    private String threadName;  // Which thread processed this
    
    private Long processingTimeMs;  // How long it took
    
    private LocalDateTime sentAt;
    
    private LocalDateTime deliveredAt;
    
    private LocalDateTime readAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

