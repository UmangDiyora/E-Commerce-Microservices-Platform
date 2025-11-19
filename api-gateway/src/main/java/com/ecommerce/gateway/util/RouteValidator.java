package com.ecommerce.gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Route Validator
 *
 * Determines which endpoints are public and which require authentication.
 */
@Component
public class RouteValidator {

    /**
     * List of public endpoints that don't require authentication
     */
    public static final List<String> publicEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/products",
            "/api/products/",
            "/api/categories",
            "/api/categories/",
            "/fallback",
            "/actuator",
            "/eureka"
    );

    /**
     * Predicate to check if the request path is secured
     */
    public Predicate<ServerHttpRequest> isSecured =
            request -> publicEndpoints.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));

    /**
     * Check if a path requires authentication
     */
    public boolean isPublicEndpoint(String path) {
        return publicEndpoints.stream()
                .anyMatch(path::contains);
    }
}
