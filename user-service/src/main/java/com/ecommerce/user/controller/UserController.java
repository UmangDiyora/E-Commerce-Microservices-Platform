package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UpdateUserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller
 *
 * Provides endpoints for user profile management.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    /**
     * Get current user profile
     *
     * @param userId User ID from request header (set by API Gateway)
     * @return User profile
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update current user profile
     *
     * @param userId User ID from request header (set by API Gateway)
     * @param request Update request
     * @return Updated user profile
     */
    @PutMapping("/me")
    @Operation(summary = "Update user profile", description = "Updates the profile of the authenticated user")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID (Admin only - called by other services)
     *
     * @param id User ID
     * @return User profile
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns user profile by ID (for inter-service communication)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}
