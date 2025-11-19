package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSearchRequest;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve all active products with pagination")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a product by its ID")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping("/search")
    @Operation(summary = "Search products", description = "Search products with filters and pagination")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestBody ProductSearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(request, pageable));
    }

    @PostMapping
    @Operation(summary = "Create product", description = "Create a new product (Admin only)")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product (Admin only)")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft delete a product (Admin only)")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reserve-stock")
    @Operation(summary = "Reserve stock", description = "Reserve stock for a product (Internal API)")
    public ResponseEntity<Boolean> reserveStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        boolean reserved = productService.reserveStock(id, quantity);
        return ResponseEntity.ok(reserved);
    }

    @PostMapping("/{id}/release-stock")
    @Operation(summary = "Release stock", description = "Release reserved stock (Internal API)")
    public ResponseEntity<Void> releaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        productService.releaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/batch")
    @Operation(summary = "Get products by IDs", description = "Retrieve multiple products by their IDs")
    public ResponseEntity<List<ProductResponse>> getProductsByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }
}
