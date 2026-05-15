package com.ecommerce.order.domain.entity;

import com.ecommerce.order.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipping_addresses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShippingAddress extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String street;

    @Column(length = 100)
    private String apartment;

    @Column(length = 20)
    private String zipCode;

    public String getFormatted() {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName).append(", ");
        sb.append(street);
        if (apartment != null) sb.append(", ").append(apartment);
        sb.append(", ").append(city);
        if (district != null) sb.append(", ").append(district);
        sb.append(", ").append(country);
        if (zipCode != null) sb.append(" ").append(zipCode);
        return sb.toString();
    }
}
