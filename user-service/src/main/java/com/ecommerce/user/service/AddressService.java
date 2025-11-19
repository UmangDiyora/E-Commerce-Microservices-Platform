package com.ecommerce.user.service;

import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.entity.Address;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.ResourceNotFoundException;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Address Service
 *
 * Handles address management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Get all addresses for a user
     *
     * @param userId User's ID
     * @return List of address responses
     */
    public List<AddressResponse> getUserAddresses(Long userId) {
        log.info("Fetching addresses for user: {}", userId);
        List<Address> addresses = addressRepository.findByUserId(userId);
        return addresses.stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific address
     *
     * @param userId User's ID
     * @param addressId Address ID
     * @return Address response
     */
    public AddressResponse getAddress(Long userId, Long addressId) {
        log.info("Fetching address {} for user {}", addressId, userId);
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return mapToAddressResponse(address);
    }

    /**
     * Create a new address for a user
     *
     * @param userId User's ID
     * @param request Address request
     * @return Created address response
     */
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        log.info("Creating new address for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // If this is the default address, unset other default addresses
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            List<Address> userAddresses = addressRepository.findByUserId(userId);
            userAddresses.forEach(addr -> addr.setIsDefault(false));
            addressRepository.saveAll(userAddresses);
        }

        Address address = Address.builder()
                .user(user)
                .addressType(request.getAddressType())
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        address = addressRepository.save(address);
        log.info("Address created successfully: {}", address.getId());

        return mapToAddressResponse(address);
    }

    /**
     * Update an existing address
     *
     * @param userId User's ID
     * @param addressId Address ID
     * @param request Address update request
     * @return Updated address response
     */
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        log.info("Updating address {} for user {}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // If setting as default, unset other default addresses
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            List<Address> userAddresses = addressRepository.findByUserId(userId);
            userAddresses.forEach(addr -> addr.setIsDefault(false));
            addressRepository.saveAll(userAddresses);
        }

        // Update fields
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }
        if (request.getStreetAddress() != null) {
            address.setStreetAddress(request.getStreetAddress());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            address.setZipCode(request.getZipCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        address = addressRepository.save(address);
        log.info("Address updated successfully: {}", addressId);

        return mapToAddressResponse(address);
    }

    /**
     * Delete an address
     *
     * @param userId User's ID
     * @param addressId Address ID
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address {} for user {}", addressId, userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        addressRepository.delete(address);
        log.info("Address deleted successfully: {}", addressId);
    }

    /**
     * Map Address entity to AddressResponse DTO
     */
    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .addressType(address.getAddressType())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}
