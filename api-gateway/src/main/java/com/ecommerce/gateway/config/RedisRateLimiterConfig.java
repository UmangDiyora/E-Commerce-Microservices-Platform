package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Redis Rate Limiter Configuration
 *
 * Configures rate limiting based on user ID or IP address.
 */
@Configuration
public class RedisRateLimiterConfig {

    /**
     * Key resolver for rate limiting based on user ID
     * Falls back to IP address if user is not authenticated
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from header (set by JWT filter)
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }

            // Fall back to IP address for unauthenticated requests
            String ipAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";

            return Mono.just(ipAddress);
        };
    }

    /**
     * Alternative: Rate limiting based on IP address only
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ipAddress = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ipAddress);
        };
    }

    /**
     * Alternative: Rate limiting based on API key
     */
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");
            return Mono.just(apiKey != null ? apiKey : "anonymous");
        };
    }
}
