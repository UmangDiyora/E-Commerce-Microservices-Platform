package com.ecommerce.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User Service Application
 *
 * This service handles:
 * - User registration and authentication
 * - JWT token generation and validation
 * - User profile management
 * - Address management
 *
 * @author E-Commerce Platform Team
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("""

            ========================================
            👤 User Service Started Successfully!
            ========================================
            📍 Service URL: http://localhost:8081
            🔐 Authentication: Enabled
            📊 API Docs: http://localhost:8081/swagger-ui.html
            ========================================
            """);
    }
}
