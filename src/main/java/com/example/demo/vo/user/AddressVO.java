package com.example.demo.vo.user;

import lombok.Builder;
import lombok.Data;

/**
 * Address Value Object
 * Represents shipping/billing address information
 */
@Data
@Builder
public class AddressVO {
    private Long addressId;
    private String username;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String addressType;
    private Boolean isDefault;
}
