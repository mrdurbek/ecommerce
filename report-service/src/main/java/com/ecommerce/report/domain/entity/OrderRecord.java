package com.ecommerce.report.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    @Builder.Default
    private Integer itemCount = 0;

    @Column(length = 100)
    private String shippingCity;

    @Column(length = 100)
    private String shippingCountry;

    @Column(nullable = false)
    private LocalDateTime orderCreatedAt;

    private LocalDateTime orderConfirmedAt;
    private LocalDateTime orderShippedAt;
    private LocalDateTime orderDeliveredAt;
    private LocalDateTime orderCancelledAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}