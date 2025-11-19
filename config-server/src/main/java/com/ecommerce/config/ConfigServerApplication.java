package com.ecommerce.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Configuration Server for Centralized Configuration Management
 *
 * This server provides externalized configuration for all microservices.
 * All services can retrieve their configuration from this central location.
 *
 * @author E-Commerce Platform Team
 * @version 1.0
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
        System.out.println("""

            ========================================
            ⚙️  Config Server Started Successfully!
            ========================================
            📍 Configuration URL: http://localhost:8888
            📁 Serving configurations from native file system
            ========================================
            """);
    }
}
