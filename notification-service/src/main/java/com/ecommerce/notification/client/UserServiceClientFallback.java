package com.ecommerce.notification.client;

import com.ecommerce.notification.dto.AddressResponse;
import com.ecommerce.notification.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserResponse getUserById(Long userId) {
        log.error("User Service is unavailable. Returning fallback user for userId: {}", userId);
        return UserResponse.builder()
                .id(userId)
                .email("unavailable@ecommerce.com")
                .firstName("User")
                .lastName("Unavailable")
                .build();
    }

    @Override
    public AddressResponse getAddressById(Long addressId) {
        log.error("User Service is unavailable. Returning fallback address for addressId: {}", addressId);
        return AddressResponse.builder()
                .id(addressId)
                .street("Address Unavailable")
                .city("N/A")
                .state("N/A")
                .zipCode("N/A")
                .country("N/A")
                .build();
    }
}
