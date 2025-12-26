package com.ecommerce.review.repository;

import com.ecommerce.review.entity.Review;
import com.ecommerce.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

/**
 * Review Repository
 */
@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    /**
     * Find reviews by product (paginated)
     */
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    /**
     * Find reviews by product (stream for gRPC server streaming)
     */
    Stream<Review> findByProductIdAndStatus(Long productId, ReviewStatus status);

    /**
     * Find reviews by user (stream for gRPC)
     */
    Stream<Review> findByUserIdAndStatus(Long userId, ReviewStatus status);

    /**
     * Count reviews by product
     */
    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    /**
     * Calculate average rating
     */
    @Query(value = "{ 'productId': ?0, 'status': 'APPROVED' }", 
           fields = "{ 'rating': 1 }")
    List<Review> findRatingsByProductId(Long productId);

    /**
     * Count by rating
     */
    long countByProductIdAndStatusAndRating(Long productId, ReviewStatus status, Integer rating);

    /**
     * Count verified purchases
     */
    long countByProductIdAndStatusAndVerifiedPurchase(
        Long productId, ReviewStatus status, Boolean verifiedPurchase);
}

