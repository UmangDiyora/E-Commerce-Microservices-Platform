package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.AddressResponse;
import com.ecommerce.notification.dto.UserResponse;
import com.ecommerce.notification.event.OrderCreatedEvent;
import com.ecommerce.notification.event.OrderStatusChangedEvent;
import com.ecommerce.notification.event.PaymentCompletedEvent;
import com.ecommerce.notification.exception.NotificationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Send order confirmation email
     */
    @Async
    public void sendOrderConfirmationEmail(UserResponse user, OrderCreatedEvent event, AddressResponse shippingAddress) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Skipping order confirmation email.");
            return;
        }

        try {
            String subject = "Order Confirmation - " + event.getOrderNumber();
            String content = buildOrderConfirmationHtml(user, event, shippingAddress);

            sendEmail(user.getEmail(), subject, content);
            log.info("Order confirmation email sent to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send order confirmation email", e);
            throw new NotificationException("Failed to send order confirmation email", e);
        }
    }

    /**
     * Send payment confirmation email
     */
    @Async
    public void sendPaymentConfirmationEmail(UserResponse user, PaymentCompletedEvent event) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Skipping payment confirmation email.");
            return;
        }

        try {
            String subject = "Payment Confirmation - " + event.getPaymentId();
            String content = buildPaymentConfirmationHtml(user, event);

            sendEmail(user.getEmail(), subject, content);
            log.info("Payment confirmation email sent to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send payment confirmation email", e);
            throw new NotificationException("Failed to send payment confirmation email", e);
        }
    }

    /**
     * Send order status update email
     */
    @Async
    public void sendOrderStatusUpdateEmail(UserResponse user, OrderStatusChangedEvent event) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Skipping order status update email.");
            return;
        }

        try {
            String subject = "Order Status Update - " + event.getOrderNumber();
            String content = buildOrderStatusUpdateHtml(user, event);

            sendEmail(user.getEmail(), subject, content);
            log.info("Order status update email sent to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send order status update email", e);
            throw new NotificationException("Failed to send order status update email", e);
        }
    }

    /**
     * Send email using JavaMailSender
     */
    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Build HTML content for order confirmation email
     */
    private String buildOrderConfirmationHtml(UserResponse user, OrderCreatedEvent event, AddressResponse shippingAddress) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .section { margin: 20px 0; }
                        .label { font-weight: bold; color: #555; }
                        .value { color: #333; }
                        .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Thank You for Your Order!</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Your order has been received and is being processed.</p>

                            <div class="section">
                                <h3>Order Details:</h3>
                                <p><span class="label">Order Number:</span> <span class="value">%s</span></p>
                                <p><span class="label">Order Date:</span> <span class="value">%s</span></p>
                                <p><span class="label">Total Amount:</span> <span class="value">$%.2f</span></p>
                            </div>

                            <div class="section">
                                <h3>Shipping Address:</h3>
                                <p>%s</p>
                            </div>

                            <p>You will receive another email once your payment is confirmed.</p>

                            <p>Thank you for shopping with us!</p>
                        </div>
                        <div class="footer">
                            <p>E-Commerce Platform | This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getFirstName(),
                event.getOrderNumber(),
                event.getCreatedAt().format(DATE_FORMATTER),
                event.getTotalAmount(),
                shippingAddress != null ? shippingAddress.getFormattedAddress() : "N/A"
        );
    }

    /**
     * Build HTML content for payment confirmation email
     */
    private String buildPaymentConfirmationHtml(UserResponse user, PaymentCompletedEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .section { margin: 20px 0; }
                        .label { font-weight: bold; color: #555; }
                        .value { color: #333; }
                        .success { color: #4CAF50; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Confirmed!</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p class="success">Your payment has been successfully processed!</p>

                            <div class="section">
                                <h3>Payment Details:</h3>
                                <p><span class="label">Payment ID:</span> <span class="value">%s</span></p>
                                <p><span class="label">Transaction ID:</span> <span class="value">%s</span></p>
                                <p><span class="label">Amount Paid:</span> <span class="value">$%.2f</span></p>
                                <p><span class="label">Payment Date:</span> <span class="value">%s</span></p>
                            </div>

                            <p>Your order is now being prepared for shipment. You will receive tracking information once it ships.</p>

                            <p>Thank you for your purchase!</p>
                        </div>
                        <div class="footer">
                            <p>E-Commerce Platform | This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getFirstName(),
                event.getPaymentId(),
                event.getTransactionId() != null ? event.getTransactionId() : "N/A",
                event.getAmount(),
                event.getCompletedAt().format(DATE_FORMATTER)
        );
    }

    /**
     * Build HTML content for order status update email
     */
    private String buildOrderStatusUpdateHtml(UserResponse user, OrderStatusChangedEvent event) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background-color: #f9f9f9; }
                        .section { margin: 20px 0; }
                        .label { font-weight: bold; color: #555; }
                        .value { color: #333; }
                        .status { color: #FF9800; font-weight: bold; font-size: 18px; }
                        .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Order Status Update</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Your order status has been updated.</p>

                            <div class="section">
                                <h3>Order Information:</h3>
                                <p><span class="label">Order Number:</span> <span class="value">%s</span></p>
                                <p><span class="label">New Status:</span> <span class="status">%s</span></p>
                                <p><span class="label">Updated On:</span> <span class="value">%s</span></p>
                            </div>

                            <p>You can track your order status anytime in your account.</p>

                            <p>Thank you for choosing us!</p>
                        </div>
                        <div class="footer">
                            <p>E-Commerce Platform | This is an automated message, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                user.getFirstName(),
                event.getOrderNumber(),
                event.getNewStatus(),
                event.getChangedAt().format(DATE_FORMATTER)
        );
    }
}
