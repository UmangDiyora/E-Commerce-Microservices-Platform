package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentGatewayResponse;
import com.ecommerce.payment.dto.RefundGatewayResponse;
import com.ecommerce.payment.entity.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Simulated Payment Gateway Service
 * In production, this would integrate with real payment gateways like Stripe, PayPal, etc.
 */
@Service
@Slf4j
public class PaymentGatewayService {

    @Value("${payment.gateway.simulation-delay-ms:2000}")
    private long simulationDelayMs;

    @Value("${payment.gateway.success-rate:0.95}")
    private double successRate;

    private final Random random = new Random();

    /**
     * Simulate payment processing with external gateway
     */
    public PaymentGatewayResponse processPayment(BigDecimal amount, PaymentMethod paymentMethod) {
        log.info("Processing payment of {} via {}", amount, paymentMethod);

        try {
            // Simulate network delay
            Thread.sleep(simulationDelayMs);

            // Simulate success/failure based on success rate
            boolean success = random.nextDouble() < successRate;

            if (success) {
                String transactionId = "TXN-" + UUID.randomUUID().toString();
                log.info("Payment processed successfully. Transaction ID: {}", transactionId);

                return PaymentGatewayResponse.builder()
                        .success(true)
                        .transactionId(transactionId)
                        .gatewayResponseCode("00")
                        .build();
            } else {
                String errorMessage = "Payment declined by gateway";
                log.warn("Payment processing failed: {}", errorMessage);

                return PaymentGatewayResponse.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .gatewayResponseCode("05")
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted", e);

            return PaymentGatewayResponse.builder()
                    .success(false)
                    .errorMessage("Payment processing interrupted")
                    .gatewayResponseCode("99")
                    .build();
        }
    }

    /**
     * Simulate refund processing with external gateway
     */
    public RefundGatewayResponse processRefund(String transactionId, BigDecimal amount) {
        log.info("Processing refund for transaction {} of amount {}", transactionId, amount);

        try {
            // Simulate network delay
            Thread.sleep(1000);

            // Simulate success (higher success rate for refunds)
            boolean success = random.nextDouble() < 0.98;

            if (success) {
                String refundId = "REFUND-" + UUID.randomUUID().toString();
                log.info("Refund processed successfully. Refund ID: {}", refundId);

                return RefundGatewayResponse.builder()
                        .success(true)
                        .refundId(refundId)
                        .build();
            } else {
                String errorMessage = "Refund declined by gateway";
                log.warn("Refund processing failed: {}", errorMessage);

                return RefundGatewayResponse.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Refund processing interrupted", e);

            return RefundGatewayResponse.builder()
                    .success(false)
                    .errorMessage("Refund processing interrupted")
                    .build();
        }
    }
}
