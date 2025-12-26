package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.event.BaseEvent;
import com.ecommerce.product.service.ProductCommandService;
import com.ecommerce.product.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Product Controller
 * 
 * <p>REST API for product catalog management.</p>
 * <p>Implements CQRS pattern with separate command and query operations.</p>
 * 
 * @author E-commerce Platform Team
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Product catalog operations")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductCommandService commandService;
    private final ProductQueryService queryService;

    @Autowired
    public ProductController(
            ProductCommandService commandService,
            ProductQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /**
     * Create Product
     */
    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product in the catalog")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userId = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Creating product: {} by user: {}", request.getName(), userId);
        
        Product product = toEntity(request);
        Product created = commandService.createProduct(product, userId);
        ProductResponse response = toResponse(created);
        
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(
            "Product created successfully",
            response
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Get Product by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product", description = "Retrieves product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        logger.debug("Fetching product: {}", id);
        
        Product product = queryService.getProduct(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        
        ProductResponse response = toResponse(product);
        
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(
            "Product retrieved successfully",
            response
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Update Product
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates product details")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userId = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Updating product: {} by user: {}", id, userId);
        
        Product updates = toEntity(request);
        Product updated = commandService.updateProduct(id, updates, userId);
        ProductResponse response = toResponse(updated);
        
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(
            "Product updated successfully",
            response
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Change Product Price
     */
    @PatchMapping("/{id}/price")
    @Operation(summary = "Change price", description = "Updates product price")
    public ResponseEntity<ApiResponse<ProductResponse>> changePrice(
            @PathVariable String id,
            @RequestParam BigDecimal price,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userId = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Changing price for product: {} to {} by user: {}", id, price, userId);
        
        Product updated = commandService.changePrice(id, price, reason, userId);
        ProductResponse response = toResponse(updated);
        
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(
            "Price updated successfully",
            response
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Change Product Stock
     */
    @PatchMapping("/{id}/stock")
    @Operation(summary = "Change stock", description = "Updates product stock quantity")
    public ResponseEntity<ApiResponse<ProductResponse>> changeStock(
            @PathVariable String id,
            @RequestParam Integer stock,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userId = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Changing stock for product: {} to {} by user: {}", id, stock, userId);
        
        Product updated = commandService.changeStock(id, stock, reason, userId);
        ProductResponse response = toResponse(updated);
        
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(
            "Stock updated successfully",
            response
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Delete Product
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft deletes a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        String userId = authentication != null ? authentication.getName() : "SYSTEM";
        logger.info("Deleting product: {} by user: {}", id, userId);
        
        commandService.deleteProduct(id, reason, userId);
        
        ApiResponse<Void> apiResponse = ApiResponse.success(
            "Product deleted successfully"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get Products by Category
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Lists products in a category")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Fetching products for category: {}", categoryId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = queryService.getProductsByCategory(categoryId, pageable);
        Page<ProductResponse> response = products.map(this::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get Featured Products
     */
    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Lists featured products")
    public ResponseEntity<Page<ProductResponse>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Fetching featured products");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = queryService.getFeaturedProducts(pageable);
        Page<ProductResponse> response = products.map(this::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Search Products
     */
    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Full-text search on products")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("Searching products: {}", q);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = queryService.searchProducts(q, pageable);
        Page<ProductResponse> response = products.map(this::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get Product Event History
     */
    @GetMapping("/{id}/history")
    @Operation(summary = "Get product history", description = "Retrieves event history for a product")
    public ResponseEntity<ApiResponse<List<BaseEvent>>> getProductHistory(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        logger.debug("Fetching event history for product: {}", id);
        
        List<BaseEvent> events = queryService.getProductHistory(id);
        
        ApiResponse<List<BaseEvent>> apiResponse = ApiResponse.success(
            "Event history retrieved successfully",
            events
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Rebuild Product from Events
     */
    @PostMapping("/{id}/rebuild")
    @Operation(summary = "Rebuild product", description = "Rebuilds product from event store")
    public ResponseEntity<ApiResponse<ProductResponse>> rebuildProduct(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        
        logger.info("Rebuilding product from events: {}", id);
        
        Product rebuilt = commandService.rebuildFromEvents(id);
        ProductResponse response = toResponse(rebuilt);
        
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success(
            "Product rebuilt from events successfully",
            response
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    // ============= Helper Methods =============

    private Product toEntity(ProductRequest request) {
        return Product.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .categoryId(request.getCategoryId())
            .sku(request.getSku())
            .brand(request.getBrand())
            .stock(request.getStock() != null ? request.getStock() : 0)
            .weight(request.getWeight())
            .dimensions(request.getDimensions())
            .images(request.getImages())
            .attributes(request.getAttributes())
            .tags(request.getTags())
            .active(request.getActive() != null ? request.getActive() : true)
            .featured(request.getFeatured() != null ? request.getFeatured() : false)
            .build();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
            .productId(product.getProductId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .categoryId(product.getCategoryId())
            .sku(product.getSku())
            .brand(product.getBrand())
            .stock(product.getStock())
            .weight(product.getWeight())
            .dimensions(product.getDimensions())
            .images(product.getImages())
            .attributes(product.getAttributes())
            .tags(product.getTags())
            .active(product.getActive())
            .featured(product.getFeatured())
            .averageRating(product.getAverageRating())
            .reviewCount(product.getReviewCount())
            .version(product.getVersion())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}

