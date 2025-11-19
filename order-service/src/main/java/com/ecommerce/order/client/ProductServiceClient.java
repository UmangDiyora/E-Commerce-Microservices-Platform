package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "product-service",
    fallback = ProductServiceClientFallback.class
)
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);

    @PostMapping("/api/products/{id}/reserve-stock")
    Boolean reserveStock(@PathVariable Long id, @RequestParam Integer quantity);

    @PostMapping("/api/products/{id}/release-stock")
    void releaseStock(@PathVariable Long id, @RequestParam Integer quantity);

    @GetMapping("/api/products/batch")
    List<ProductResponse> getProductsByIds(@RequestParam List<Long> ids);
}
