package com.ecommerce.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * SMS Service - Simulated implementation
 * In production, integrate with SMS providers like Twilio, AWS SNS, etc.
 */
@Service
@Slf4j
public class SmsService {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:twilio}")
    private String smsProvider;

    /**
     * Send order confirmation SMS
     */
    @Async
    public void sendOrderConfirmationSms(String phoneNumber, String orderNumber) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled. Skipping order confirmation SMS.");
            return;
        }

        try {
            String message = String.format(
                    "Thank you for your order! Order #%s has been received and is being processed. - E-Commerce Platform",
                    orderNumber
            );

            sendSms(phoneNumber, message);
            log.info("Order confirmation SMS sent to: {}", maskPhoneNumber(phoneNumber));

        } catch (Exception e) {
            log.error("Failed to send order confirmation SMS", e);
        }
    }

    /**
     * Send payment confirmation SMS
     */
    @Async
    public void sendPaymentConfirmationSms(String phoneNumber, BigDecimal amount) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled. Skipping payment confirmation SMS.");
            return;
        }

        try {
            String message = String.format(
                    "Payment of $%.2f has been successfully processed. Your order is being prepared for shipment. - E-Commerce Platform",
                    amount
            );

            sendSms(phoneNumber, message);
            log.info("Payment confirmation SMS sent to: {}", maskPhoneNumber(phoneNumber));

        } catch (Exception e) {
            log.error("Failed to send payment confirmation SMS", e);
        }
    }

    /**
     * Send order status update SMS
     */
    @Async
    public void sendOrderStatusUpdateSms(String phoneNumber, String orderNumber, String newStatus) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled. Skipping order status update SMS.");
            return;
        }

        try {
            String message = String.format(
                    "Order #%s status updated to: %s. Track your order in your account. - E-Commerce Platform",
                    orderNumber, newStatus
            );

            sendSms(phoneNumber, message);
            log.info("Order status update SMS sent to: {}", maskPhoneNumber(phoneNumber));

        } catch (Exception e) {
            log.error("Failed to send order status update SMS", e);
        }
    }

    /**
     * Simulated SMS sending
     * In production, integrate with real SMS gateway
     */
    private void sendSms(String phoneNumber, String message) {
        // Simulate SMS sending delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[{}] SMS sent to {}: {}", smsProvider.toUpperCase(), maskPhoneNumber(phoneNumber), message);

        // In production, use actual SMS provider:
        // - Twilio: twilioClient.messages.create(...)
        // - AWS SNS: snsClient.publish(...)
        // - etc.
    }

    /**
     * Mask phone number for logging privacy
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
