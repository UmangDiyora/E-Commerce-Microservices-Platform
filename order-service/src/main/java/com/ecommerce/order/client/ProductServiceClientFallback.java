package com.ecommerce.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ProductResponse getProductById(Long id) {
        log.warn("Product service is unavailable, returning fallback for product id: {}", id);
        return ProductResponse.builder()
                .id(id)
                .name("Product Unavailable")
                .build();
    }

    @Override
    public Boolean reserveStock(Long id, Integer quantity) {
        log.error("Failed to reserve stock, product service unavailable");
        return false;
    }

    @Override
    public void releaseStock(Long id, Integer quantity) {
        log.error("Failed to release stock, product service unavailable");
    }

    @Override
    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        log.warn("Product service is unavailable, returning empty list");
        return Collections.emptyList();
    }
}
