package com.ecommerce.user.dto;

import com.ecommerce.user.entity.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Address Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private Long id;
    private AddressType addressType;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Boolean isDefault;
}
