package com.ecommerce.user.controller;

import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Address Controller
 *
 * Provides endpoints for address management.
 */
@RestController
@RequestMapping("/api/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Address management APIs")
public class AddressController {

    private final AddressService addressService;

    /**
     * Get all addresses for current user
     *
     * @param userId User ID from request header (set by API Gateway)
     * @return List of addresses
     */
    @GetMapping
    @Operation(summary = "Get user addresses", description = "Returns all addresses for the authenticated user")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(@RequestHeader("X-User-Id") Long userId) {
        List<AddressResponse> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get a specific address
     *
     * @param userId User ID from request header
     * @param addressId Address ID
     * @return Address details
     */
    @GetMapping("/{addressId}")
    @Operation(summary = "Get address by ID", description = "Returns a specific address for the authenticated user")
    public ResponseEntity<AddressResponse> getAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long addressId) {
        AddressResponse address = addressService.getAddress(userId, addressId);
        return ResponseEntity.ok(address);
    }

    /**
     * Create a new address
     *
     * @param userId User ID from request header
     * @param request Address details
     * @return Created address
     */
    @PostMapping
    @Operation(summary = "Create address", description = "Creates a new address for the authenticated user")
    public ResponseEntity<AddressResponse> createAddress(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.createAddress(userId, request);
        return new ResponseEntity<>(address, HttpStatus.CREATED);
    }

    /**
     * Update an existing address
     *
     * @param userId User ID from request header
     * @param addressId Address ID
     * @param request Address update details
     * @return Updated address
     */
    @PutMapping("/{addressId}")
    @Operation(summary = "Update address", description = "Updates an existing address for the authenticated user")
    public ResponseEntity<AddressResponse> updateAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(address);
    }

    /**
     * Delete an address
     *
     * @param userId User ID from request header
     * @param addressId Address ID
     * @return No content
     */
    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete address", description = "Deletes an address for the authenticated user")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
