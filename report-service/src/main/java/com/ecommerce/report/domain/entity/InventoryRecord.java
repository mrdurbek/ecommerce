package com.ecommerce.report.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String orderNumber;

    @Column(nullable = false)
    private Long productId;

    @Column(length = 100)
    private String productSku;

    @Column(nullable = false)
    private Integer quantity;

    // RESERVED | RESERVATION_FAILED
    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}