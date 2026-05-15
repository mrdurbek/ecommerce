package com.ecommerce.order.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String country;
    private String city;
    private String district;
    private String street;
    private String apartment;
    private String zipCode;
    private String formatted;
}