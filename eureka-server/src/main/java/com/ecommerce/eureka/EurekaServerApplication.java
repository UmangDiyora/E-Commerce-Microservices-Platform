package com.ecommerce.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Discovery Server
 *
 * This server acts as a service registry where all microservices register themselves.
 * Other services can discover and communicate with each other through this registry.
 *
 * @author E-Commerce Platform Team
 * @version 1.0
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        System.out.println("""

            ========================================
            🚀 Eureka Server Started Successfully!
            ========================================
            📍 Dashboard: http://localhost:8761
            🔍 Service Registry is ready to accept registrations
            ========================================
            """);
    }
}
