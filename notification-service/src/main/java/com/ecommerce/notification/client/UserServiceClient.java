package com.ecommerce.notification.client;

import com.ecommerce.notification.dto.AddressResponse;
import com.ecommerce.notification.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUserById(@PathVariable Long userId);

    @GetMapping("/api/addresses/{addressId}")
    AddressResponse getAddressById(@PathVariable Long addressId);
}
