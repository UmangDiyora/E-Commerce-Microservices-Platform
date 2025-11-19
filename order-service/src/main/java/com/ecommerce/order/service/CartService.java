package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductResponse;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.CartItem;
import com.ecommerce.order.entity.ShoppingCart;
import com.ecommerce.order.exception.InvalidRequestException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.repository.CartItemRepository;
import com.ecommerce.order.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    public CartResponse getCart(Long userId) {
        log.info("Fetching cart for user: {}", userId);
        ShoppingCart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        log.info("Adding product {} to cart for user {}", request.getProductId(), userId);

        // Validate product exists and is available
        ProductResponse product = productServiceClient.getProductById(request.getProductId());
        if (product == null || product.getId() == null) {
            throw new ResourceNotFoundException("Product not found with id: " + request.getProductId());
        }

        if (!product.getIsActive()) {
            throw new InvalidRequestException("Product is not available");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InvalidRequestException("Insufficient stock for product: " + product.getName());
        }

        ShoppingCart cart = getOrCreateCart(userId);

        // Check if product already exists in cart
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.getProductId())
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new InvalidRequestException("Insufficient stock for product: " + product.getName());
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            // Add new item
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .productId(request.getProductId())
                    .productName(product.getName())
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cart.addItem(cartItem);
            cartRepository.save(cart);
        }

        log.info("Product added to cart successfully");
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        log.info("Updating cart item {} for user {}", itemId, userId);

        ShoppingCart cart = getCartByUserId(userId);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new InvalidRequestException("Cart item does not belong to user");
        }

        // Validate stock availability
        ProductResponse product = productServiceClient.getProductById(cartItem.getProductId());
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InvalidRequestException("Insufficient stock for product: " + product.getName());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.setUnitPrice(product.getPrice());
        cartItemRepository.save(cartItem);

        log.info("Cart item updated successfully");
        return mapToResponse(cart);
    }

    @Transactional
    public void removeCartItem(Long userId, Long itemId) {
        log.info("Removing cart item {} for user {}", itemId, userId);

        ShoppingCart cart = getCartByUserId(userId);
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new InvalidRequestException("Cart item does not belong to user");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        log.info("Cart item removed successfully");
    }

    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);
        ShoppingCart cart = getCartByUserId(userId);
        cart.clearItems();
        cartItemRepository.deleteByCartId(cart.getId());
        cartRepository.save(cart);
        log.info("Cart cleared successfully");
    }

    public List<CartItem> getCartItems(Long userId) {
        ShoppingCart cart = getCartByUserId(userId);
        return cart.getItems();
    }

    private ShoppingCart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ShoppingCart newCart = ShoppingCart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private ShoppingCart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private CartResponse mapToResponse(ShoppingCart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse mapItemToResponse(CartItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(subtotal)
                .addedAt(item.getAddedAt())
                .build();
    }
}
