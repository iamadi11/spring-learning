package com.ecommerce.review.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Review Entity (MongoDB)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reviews")
@CompoundIndex(name = "product_rating_idx", def = "{'productId': 1, 'rating': -1}")
@CompoundIndex(name = "user_product_idx", def = "{'userId': 1, 'productId': 1}")
public class Review {

    @Id
    private String id;

    @Indexed
    private Long productId;

    @Indexed
    private Long userId;

    private String userName;

    private Integer rating;  // 1-5

    private String title;

    private String comment;

    private Boolean verifiedPurchase;

    private Integer helpfulCount;

    private Integer unhelpfulCount;

    private ReviewStatus status;

    private List<String> images;

    private String moderationReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

