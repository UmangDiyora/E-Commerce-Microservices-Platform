package com.ecommerce.order.controller;

import com.ecommerce.order.dto.AddToCartRequest;
import com.ecommerce.order.dto.CartResponse;
import com.ecommerce.order.dto.UpdateCartItemRequest;
import com.ecommerce.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart", description = "APIs for managing shopping cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart", description = "Get the shopping cart for the authenticated user")
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add to cart", description = "Add a product to the shopping cart")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return new ResponseEntity<>(cartService.addToCart(userId, request), HttpStatus.CREATED);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update cart item", description = "Update the quantity of a cart item")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, id, request));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Remove cart item", description = "Remove an item from the cart")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        cartService.removeCartItem(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Clear all items from the cart")
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
