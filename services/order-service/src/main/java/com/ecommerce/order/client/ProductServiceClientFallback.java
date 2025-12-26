package com.ecommerce.order.client;

import com.ecommerce.order.dto.ReserveInventoryRequest;
import com.ecommerce.order.dto.ReleaseInventoryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Product Service Client Fallback
 * 
 * <p>Provides fallback behavior when Product Service is unavailable.</p>
 * 
 * @author E-commerce Platform Team
 */
@Component
public class ProductServiceClientFallback implements ProductServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceClientFallback.class);

    @Override
    public void reserveInventory(ReserveInventoryRequest request) {
        logger.error("Product Service unavailable - cannot reserve inventory for order: {}", 
            request.getOrderId());
        throw new RuntimeException("Product Service unavailable");
    }

    @Override
    public void releaseInventory(ReleaseInventoryRequest request) {
        logger.error("Product Service unavailable - cannot release inventory for order: {}", 
            request.getOrderId());
        throw new RuntimeException("Product Service unavailable");
    }
}

