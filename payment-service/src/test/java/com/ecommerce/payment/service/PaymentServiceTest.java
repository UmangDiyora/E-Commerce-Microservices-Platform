package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentGatewayResponse;
import com.ecommerce.payment.dto.RefundGatewayResponse;
import com.ecommerce.payment.dto.RefundResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentMethod;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.exception.InvalidRequestException;
import com.ecommerce.payment.exception.ResourceNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewayService paymentGatewayService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private OrderCreatedEvent orderCreatedEvent;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .id(1L)
                .paymentId("PAY-123456")
                .orderId(1L)
                .userId(1L)
                .amount(new BigDecimal("2599.98"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .transactionId("TXN-ABC123")
                .build();

        orderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-123456")
                .userId(1L)
                .totalAmount(new BigDecimal("2599.98"))
                .shippingAddressId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void handleOrderCreated_Success() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        paymentService.handleOrderCreated(orderCreatedEvent);

        // Assert
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPaymentAsync_Success() {
        // Arrange
        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .success(true)
                .transactionId("TXN-ABC123")
                .gatewayResponseCode("00")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentGatewayService.processPayment(any(BigDecimal.class), any(PaymentMethod.class)))
                .thenReturn(gatewayResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        // Act
        paymentService.processPaymentAsync(1L);

        // Assert
        assertEquals(PaymentStatus.COMPLETED, testPayment.getStatus());
        verify(paymentRepository, times(2)).save(any(Payment.class)); // Once for PROCESSING, once for COMPLETED
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void processPaymentAsync_Failed() {
        // Arrange
        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .success(false)
                .errorMessage("Payment declined")
                .gatewayResponseCode("05")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentGatewayService.processPayment(any(BigDecimal.class), any(PaymentMethod.class)))
                .thenReturn(gatewayResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any());

        // Act
        paymentService.processPaymentAsync(1L);

        // Assert
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void getPaymentByPaymentId_Success() {
        // Arrange
        when(paymentRepository.findByPaymentId("PAY-123456")).thenReturn(Optional.of(testPayment));

        // Act
        var result = paymentService.getPaymentByPaymentId("PAY-123456");

        // Assert
        assertNotNull(result);
        assertEquals("PAY-123456", result.getPaymentId());
        verify(paymentRepository, times(1)).findByPaymentId("PAY-123456");
    }

    @Test
    void getPaymentByPaymentId_NotFound_ThrowsException() {
        // Arrange
        when(paymentRepository.findByPaymentId("PAY-123456")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.getPaymentByPaymentId("PAY-123456");
        });
    }

    @Test
    void getPaymentsByOrderId_Success() {
        // Arrange
        when(paymentRepository.findByOrderId(1L)).thenReturn(Arrays.asList(testPayment));

        // Act
        List<var> result = paymentService.getPaymentsByOrderId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentRepository, times(1)).findByOrderId(1L);
    }

    @Test
    void processRefund_Success() {
        // Arrange
        testPayment.setStatus(PaymentStatus.COMPLETED);
        RefundGatewayResponse refundResponse = RefundGatewayResponse.builder()
                .success(true)
                .refundId("REFUND-123")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentGatewayService.processRefund(anyString(), any(BigDecimal.class)))
                .thenReturn(refundResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        RefundResponse result = paymentService.processRefund(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("REFUND-123", result.getRefundId());
        assertEquals(PaymentStatus.REFUNDED, testPayment.getStatus());
        verify(paymentRepository, times(1)).save(testPayment);
    }

    @Test
    void processRefund_InvalidStatus_ThrowsException() {
        // Arrange
        testPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        assertThrows(InvalidRequestException.class, () -> {
            paymentService.processRefund(1L);
        });
        verify(paymentGatewayService, never()).processRefund(anyString(), any(BigDecimal.class));
    }

    @Test
    void getUserPayments_Success() {
        // Arrange
        when(paymentRepository.findByUserId(1L)).thenReturn(Arrays.asList(testPayment));

        // Act
        List<var> result = paymentService.getUserPayments(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentRepository, times(1)).findByUserId(1L);
    }
}
