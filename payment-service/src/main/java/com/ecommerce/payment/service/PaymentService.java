package com.ecommerce.payment.service;

import com.ecommerce.payment.config.RabbitMQConfig;
import com.ecommerce.payment.dto.*;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.event.OrderCreatedEvent;
import com.ecommerce.payment.event.PaymentCompletedEvent;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.exception.InvalidRequestException;
import com.ecommerce.payment.exception.PaymentException;
import com.ecommerce.payment.exception.ResourceNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Listen to order.created events and initiate payment processing
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for order: {}", event.getOrderNumber());

        try {
            // Create payment record
            Payment payment = Payment.builder()
                    .paymentId(generatePaymentId())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getTotalAmount())
                    .status(PaymentStatus.PENDING)
                    .build();

            payment = paymentRepository.save(payment);
            log.info("Created payment record with ID: {}", payment.getPaymentId());

            // Process payment asynchronously
            processPaymentAsync(payment.getId());

        } catch (Exception e) {
            log.error("Failed to handle order created event for order: {}", event.getOrderNumber(), e);
            // Publish payment failed event
            publishPaymentFailedEvent(event.getOrderId(), null, e.getMessage());
        }
    }

    /**
     * Process payment asynchronously
     */
    @Async
    public void processPaymentAsync(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        try {
            log.info("Starting async payment processing for payment: {}", payment.getPaymentId());

            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepository.save(payment);

            // Call payment gateway (Stripe, PayPal, etc.)
            PaymentGatewayResponse response = paymentGatewayService.processPayment(
                    payment.getAmount(),
                    payment.getPaymentMethod()
            );

            if (response.isSuccess()) {
                handlePaymentSuccess(payment, response);
            } else {
                handlePaymentFailure(payment, response);
            }

        } catch (Exception e) {
            log.error("Payment processing failed for payment: {}", payment.getPaymentId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            publishPaymentFailedEvent(payment.getOrderId(), payment.getPaymentId(), e.getMessage());
        }
    }

    /**
     * Handle successful payment
     */
    private void handlePaymentSuccess(Payment payment, PaymentGatewayResponse response) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(response.getTransactionId());
        payment.setPaymentGatewayResponse(response.toString());
        paymentRepository.save(payment);

        log.info("Payment completed successfully: {}", payment.getPaymentId());

        // Publish payment completed event
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .completedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMPLETED_ROUTING_KEY,
                event
        );

        log.info("Published payment completed event for payment: {}", payment.getPaymentId());
    }

    /**
     * Handle failed payment
     */
    private void handlePaymentFailure(Payment payment, PaymentGatewayResponse response) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setPaymentGatewayResponse(response.toString());
        paymentRepository.save(payment);

        log.warn("Payment failed: {}", payment.getPaymentId());

        publishPaymentFailedEvent(payment.getOrderId(), payment.getPaymentId(), response.getErrorMessage());
    }

    /**
     * Publish payment failed event
     */
    private void publishPaymentFailedEvent(Long orderId, String paymentId, String errorMessage) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .errorMessage(errorMessage)
                .failedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                event
        );

        log.info("Published payment failed event for order: {}", orderId);
    }

    /**
     * Process refund for a completed payment
     */
    @Transactional
    public RefundResponse processRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidRequestException("Cannot refund payment with status: " + payment.getStatus());
        }

        log.info("Processing refund for payment: {}", payment.getPaymentId());

        // Process refund with gateway
        RefundGatewayResponse response = paymentGatewayService.processRefund(
                payment.getTransactionId(),
                payment.getAmount()
        );

        if (response.isSuccess()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            log.info("Refund processed successfully for payment: {}", payment.getPaymentId());

            return RefundResponse.builder()
                    .success(true)
                    .refundId(response.getRefundId())
                    .amount(payment.getAmount())
                    .message("Refund processed successfully")
                    .build();
        } else {
            log.error("Refund failed for payment: {}", payment.getPaymentId());
            throw new PaymentException("Refund failed: " + response.getErrorMessage());
        }
    }

    /**
     * Get payment by payment ID
     */
    public PaymentResponse getPaymentByPaymentId(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with paymentId: " + paymentId));

        return mapToResponse(payment);
    }

    /**
     * Get payments by order ID
     */
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        return payments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user's payment history
     */
    public List<PaymentResponse> getUserPayments(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Generate unique payment ID
     */
    private String generatePaymentId() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Map Payment entity to PaymentResponse DTO
     */
    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
