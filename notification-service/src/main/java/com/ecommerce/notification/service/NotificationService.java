package com.ecommerce.notification.service;

import com.ecommerce.notification.client.UserServiceClient;
import com.ecommerce.notification.config.RabbitMQConfig;
import com.ecommerce.notification.dto.AddressResponse;
import com.ecommerce.notification.dto.UserResponse;
import com.ecommerce.notification.event.OrderCreatedEvent;
import com.ecommerce.notification.event.OrderStatusChangedEvent;
import com.ecommerce.notification.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;
    private final UserServiceClient userServiceClient;

    /**
     * Handle order created event
     * Send order confirmation email and SMS
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order created event for order: {}", event.getOrderNumber());

        try {
            // Fetch user details
            UserResponse user = userServiceClient.getUserById(event.getUserId());

            // Fetch shipping address if available
            AddressResponse shippingAddress = null;
            if (event.getShippingAddressId() != null) {
                try {
                    shippingAddress = userServiceClient.getAddressById(event.getShippingAddressId());
                } catch (Exception e) {
                    log.warn("Could not fetch shipping address: {}", event.getShippingAddressId(), e);
                }
            }

            // Send email notification
            emailService.sendOrderConfirmationEmail(user, event, shippingAddress);

            // Send SMS notification if phone number is available
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                smsService.sendOrderConfirmationSms(user.getPhone(), event.getOrderNumber());
            }

            log.info("Order confirmation notifications sent for order: {}", event.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order confirmation notification for order: {}", event.getOrderNumber(), e);
            // Don't throw exception - notification failures shouldn't break the flow
        }
    }

    /**
     * Handle payment completed event
     * Send payment confirmation email and SMS
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment completed event for payment: {}", event.getPaymentId());

        try {
            // Fetch user details
            UserResponse user = userServiceClient.getUserById(event.getUserId());

            // Send email notification
            emailService.sendPaymentConfirmationEmail(user, event);

            // Send SMS notification if phone number is available
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                smsService.sendPaymentConfirmationSms(user.getPhone(), event.getAmount());
            }

            log.info("Payment confirmation notifications sent for payment: {}", event.getPaymentId());

        } catch (Exception e) {
            log.error("Failed to send payment confirmation notification for payment: {}", event.getPaymentId(), e);
            // Don't throw exception - notification failures shouldn't break the flow
        }
    }

    /**
     * Handle order status changed event
     * Send order status update email and SMS
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_STATUS_CHANGED_QUEUE)
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received order status changed event. Order: {}, New Status: {}",
                event.getOrderNumber(), event.getNewStatus());

        try {
            // Fetch user details
            UserResponse user = userServiceClient.getUserById(event.getUserId());

            // Send email notification
            emailService.sendOrderStatusUpdateEmail(user, event);

            // Send SMS notification if phone number is available
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                smsService.sendOrderStatusUpdateSms(user.getPhone(), event.getOrderNumber(), event.getNewStatus());
            }

            log.info("Order status update notifications sent for order: {}", event.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send order status update notification for order: {}", event.getOrderNumber(), e);
            // Don't throw exception - notification failures shouldn't break the flow
        }
    }
}
