package com.ecommerce.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for notification service status and configuration")
public class NotificationController {

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:twilio}")
    private String smsProvider;

    @GetMapping("/health")
    @Operation(summary = "Get notification service health status")
    public ResponseEntity<Map<String, Object>> getHealth() {
        log.info("Health check requested");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "notification-service");
        health.put("emailEnabled", emailEnabled);
        health.put("smsEnabled", smsEnabled);
        if (smsEnabled) {
            health.put("smsProvider", smsProvider);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/config")
    @Operation(summary = "Get notification service configuration")
    public ResponseEntity<Map<String, Object>> getConfig() {
        log.info("Configuration check requested");

        Map<String, Object> config = new HashMap<>();
        config.put("emailNotifications", emailEnabled ? "enabled" : "disabled");
        config.put("smsNotifications", smsEnabled ? "enabled" : "disabled");
        config.put("supportedChannels", emailEnabled && smsEnabled ? "email,sms" :
                emailEnabled ? "email" : smsEnabled ? "sms" : "none");

        return ResponseEntity.ok(config);
    }
}
