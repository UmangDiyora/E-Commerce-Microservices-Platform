package com.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application
 *
 * This gateway serves as the single entry point for all client requests.
 * It provides:
 * - Request routing to appropriate microservices
 * - Load balancing across service instances
 * - Circuit breaker pattern for resilience
 * - JWT authentication and authorization
 * - Rate limiting for API protection
 *
 * @author E-Commerce Platform Team
 * @version 1.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("""

            ========================================
            🌐 API Gateway Started Successfully!
            ========================================
            📍 Gateway URL: http://localhost:8080
            🔐 JWT Authentication: Enabled
            🔄 Circuit Breaker: Enabled
            📊 Rate Limiting: Enabled
            ========================================
            """);
    }
}
