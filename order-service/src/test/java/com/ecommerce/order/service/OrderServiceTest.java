package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderRequest orderRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productResponse = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(50)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .productName("Laptop")
                .quantity(2)
                .price(new BigDecimal("1299.99"))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-123456")
                .userId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("2599.98"))
                .shippingAddressId(1L)
                .items(Arrays.asList(orderItem))
                .build();

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        orderRequest = OrderRequest.builder()
                .userId(1L)
                .shippingAddressId(1L)
                .items(Arrays.asList(itemRequest))
                .build();
    }

    @Test
    void createOrder_Success() {
        // Arrange
        when(productServiceClient.getProductById(1L)).thenReturn(productResponse);
        when(productServiceClient.reserveStock(eq(1L), eq(2))).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        // Act
        OrderResponse result = orderService.createOrder(orderRequest);

        // Assert
        assertNotNull(result);
        assertEquals("ORD-123456", result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("2599.98"), result.getTotalAmount());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void createOrder_StockReservationFailed_ThrowsException() {
        // Arrange
        when(productServiceClient.getProductById(1L)).thenReturn(productResponse);
        when(productServiceClient.reserveStock(eq(1L), eq(2))).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderRequest);
        });
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        OrderResponse result = orderService.getOrderById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD-123456", result.getOrderNumber());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void getOrderById_NotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderById(1L);
        });
    }

    @Test
    void getOrderByOrderNumber_Success() {
        // Arrange
        when(orderRepository.findByOrderNumber("ORD-123456")).thenReturn(Optional.of(testOrder));

        // Act
        OrderResponse result = orderService.getOrderByOrderNumber("ORD-123456");

        // Assert
        assertNotNull(result);
        assertEquals("ORD-123456", result.getOrderNumber());
        verify(orderRepository, times(1)).findByOrderNumber("ORD-123456");
    }

    @Test
    void getUserOrders_Success() {
        // Arrange
        when(orderRepository.findByUserId(1L)).thenReturn(Arrays.asList(testOrder));

        // Act
        List<OrderResponse> result = orderService.getUserOrders(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORD-123456", result.get(0).getOrderNumber());
        verify(orderRepository, times(1)).findByUserId(1L);
    }

    @Test
    void updateOrderStatus_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    void cancelOrder_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(productServiceClient).releaseStock(anyLong(), anyInt());

        // Act
        orderService.cancelOrder(1L);

        // Assert
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
        verify(productServiceClient, times(1)).releaseStock(1L, 2);
    }
}
