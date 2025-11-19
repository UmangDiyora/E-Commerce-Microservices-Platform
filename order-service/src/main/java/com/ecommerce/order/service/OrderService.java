package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.config.RabbitMQConfig;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderItemEvent;
import com.ecommerce.order.exception.InvalidRequestException;
import com.ecommerce.order.exception.OutOfStockException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductServiceClient productServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        log.info("Creating order for user: {}", userId);

        // 1. Get cart items
        List<CartItem> cartItems = cartService.getCartItems(userId);

        if (cartItems.isEmpty()) {
            throw new InvalidRequestException("Cart is empty");
        }

        // 2. Reserve stock for all products
        List<StockReservation> reservations = new ArrayList<>();
        try {
            for (CartItem item : cartItems) {
                boolean reserved = productServiceClient.reserveStock(
                        item.getProductId(),
                        item.getQuantity()
                );

                if (!reserved) {
                    // Rollback previous reservations
                    rollbackReservations(reservations);
                    throw new OutOfStockException("Product out of stock: " + item.getProductName());
                }

                reservations.add(new StockReservation(item.getProductId(), item.getQuantity()));
            }

            // 3. Calculate total amount
            BigDecimal totalAmount = cartItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. Create order
            Order order = Order.builder()
                    .orderNumber(generateOrderNumber())
                    .userId(userId)
                    .status(OrderStatus.PENDING)
                    .totalAmount(totalAmount)
                    .shippingAddressId(request.getShippingAddressId())
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            // 5. Create order items
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .productId(cartItem.getProductId())
                        .productName(cartItem.getProductName())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getUnitPrice())
                        .subtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                        .build();
                order.addOrderItem(orderItem);
            }

            order = orderRepository.save(order);
            log.info("Order created with number: {}", order.getOrderNumber());

            // 6. Clear cart
            cartService.clearCart(userId);

            // 7. Publish order created event
            publishOrderCreatedEvent(order);

            return mapToResponse(order);

        } catch (Exception e) {
            // Rollback stock reservations on any error
            rollbackReservations(reservations);
            throw e;
        }
    }

    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        log.info("Fetching orders for user: {}", userId);
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    public OrderResponse getOrderById(Long orderId, Long userId) {
        log.info("Fetching order {} for user {}", orderId, userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new InvalidRequestException("Order does not belong to user");
        }

        return mapToResponse(order);
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        log.info("Fetching order by number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new InvalidRequestException("Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidRequestException("Cannot cancel order in status: " + order.getStatus());
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidRequestException("Order is already cancelled");
        }

        // Release stock for all items
        for (OrderItem item : order.getOrderItems()) {
            productServiceClient.releaseStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        log.info("Order cancelled successfully");
        return mapToResponse(order);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Order status updated successfully");
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, PaymentStatus paymentStatus, String paymentId) {
        log.info("Updating payment status for order {} to {}", orderId, paymentStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setPaymentStatus(paymentStatus);
        order.setPaymentId(paymentId);

        if (paymentStatus == PaymentStatus.COMPLETED) {
            order.setStatus(OrderStatus.CONFIRMED);
        } else if (paymentStatus == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.CANCELLED);
            // Release stock
            for (OrderItem item : order.getOrderItems()) {
                productServiceClient.releaseStock(item.getProductId(), item.getQuantity());
            }
        }

        orderRepository.save(order);
        log.info("Payment status updated successfully");
    }

    private void publishOrderCreatedEvent(Order order) {
        List<OrderItemEvent> itemEvents = order.getOrderItems().stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .shippingAddressId(order.getShippingAddressId())
                .items(itemEvents)
                .createdAt(order.getCreatedAt())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );

        log.info("Published order created event for order: {}", order.getOrderNumber());
    }

    private void rollbackReservations(List<StockReservation> reservations) {
        log.warn("Rolling back {} stock reservations", reservations.size());
        for (StockReservation reservation : reservations) {
            try {
                productServiceClient.releaseStock(
                        reservation.getProductId(),
                        reservation.getQuantity()
                );
            } catch (Exception e) {
                log.error("Failed to release stock for product: {}", reservation.getProductId(), e);
            }
        }
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD-" + timestamp + "-" + randomSuffix;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddressId(order.getShippingAddressId())
                .paymentStatus(order.getPaymentStatus())
                .paymentId(order.getPaymentId())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class StockReservation {
        private Long productId;
        private Integer quantity;
    }
}
