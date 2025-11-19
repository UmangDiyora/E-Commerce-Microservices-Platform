package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration
 *
 * Defines routes for all microservices with circuit breaker, retry, and rate limiting patterns.
 */
@Configuration
public class GatewayConfig {

    private final KeyResolver userKeyResolver;

    public GatewayConfig(KeyResolver userKeyResolver) {
        this.userKeyResolver = userKeyResolver;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r
                        .path("/api/users/**", "/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("userServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/user-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(null, null, null, false))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                        .uri("lb://USER-SERVICE"))

                // Product Service Routes
                .route("product-service", r -> r
                        .path("/api/products/**", "/api/categories/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("productServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/product-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(null, null, null, false))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                        .uri("lb://PRODUCT-SERVICE"))

                // Order Service Routes
                .route("order-service", r -> r
                        .path("/api/orders/**", "/api/cart/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("orderServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/order-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(null, null, null, false))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                        .uri("lb://ORDER-SERVICE"))

                // Payment Service Routes
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("paymentServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/payment-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(null, null, null, false))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                        .uri("lb://PAYMENT-SERVICE"))

                // Notification Service Routes
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c
                                        .setName("notificationServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/notification-service"))
                                .retry(config -> config
                                        .setRetries(3)
                                        .setBackoff(null, null, null, false))
                                .requestRateLimiter(c -> c
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver)))
                        .uri("lb://NOTIFICATION-SERVICE"))

                .build();
    }

    /**
     * Redis Rate Limiter Bean
     *
     * replenishRate: Number of requests per second a user is allowed
     * burstCapacity: Maximum number of requests a user is allowed in a single second
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
        // 10 requests per second, with a burst capacity of 20
    }
}
