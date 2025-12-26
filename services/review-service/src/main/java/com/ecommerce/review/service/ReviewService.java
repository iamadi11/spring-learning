package com.ecommerce.review.service;

import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.entity.Review;
import com.ecommerce.review.entity.ReviewStatus;
import com.ecommerce.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Review Service
 * 
 * <p>Business logic for review management (used by REST API).</p>
 * 
 * @author E-commerce Platform Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    /**
     * Create review
     */
    @CacheEvict(value = "productReviews", key = "#request.productId")
    public ReviewResponse createReview(ReviewRequest request) {
        log.info("Creating review for product: {}", request.getProductId());

        Review review = Review.builder()
            .id(UUID.randomUUID().toString())
            .productId(request.getProductId())
            .userId(request.getUserId())
            .userName("User " + request.getUserId())
            .rating(request.getRating())
            .title(request.getTitle())
            .comment(request.getComment())
            .verifiedPurchase(request.getVerifiedPurchase() != null ? 
                             request.getVerifiedPurchase() : false)
            .helpfulCount(0)
            .unhelpfulCount(0)
            .status(ReviewStatus.APPROVED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        review = reviewRepository.save(review);

        return convertToDto(review);
    }

    /**
     * Get review by ID
     */
    @Cacheable(value = "reviews", key = "#reviewId")
    public ReviewResponse getReview(String reviewId) {
        log.info("Getting review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));

        return convertToDto(review);
    }

    /**
     * Get product reviews (paginated)
     */
    @Cacheable(value = "productReviews", key = "#productId + '-' + #page + '-' + #size")
    public Page<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        log.info("Getting reviews for product: {}", productId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Review> reviews = reviewRepository.findByProductIdAndStatus(
            productId, ReviewStatus.APPROVED, pageable);

        return reviews.map(this::convertToDto);
    }

    /**
     * Update review
     */
    @CacheEvict(value = {"reviews", "productReviews"}, allEntries = true)
    public ReviewResponse updateReview(String reviewId, ReviewRequest request) {
        log.info("Updating review: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        return convertToDto(review);
    }

    /**
     * Delete review
     */
    @CacheEvict(value = {"reviews", "productReviews"}, allEntries = true)
    public void deleteReview(String reviewId) {
        log.info("Deleting review: {}", reviewId);
        reviewRepository.deleteById(reviewId);
    }

    /**
     * Mark review as helpful
     */
    @CacheEvict(value = "reviews", key = "#reviewId")
    public void markHelpful(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found"));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
    }

    /**
     * Convert entity to DTO
     */
    private ReviewResponse convertToDto(Review review) {
        return ReviewResponse.builder()
            .id(review.getId())
            .productId(review.getProductId())
            .userId(review.getUserId())
            .userName(review.getUserName())
            .rating(review.getRating())
            .title(review.getTitle())
            .comment(review.getComment())
            .verifiedPurchase(review.getVerifiedPurchase())
            .helpfulCount(review.getHelpfulCount())
            .unhelpfulCount(review.getUnhelpfulCount())
            .status(review.getStatus())
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .build();
    }
}

