package com.ecommerce.payment.domain.entity;

import com.ecommerce.payment.audit.BaseEntity;
import com.ecommerce.payment.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String paymentRef;

    @Column(nullable = false, length = 30)
    private Long orderId;

    @Column(nullable = false, length = 100)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "UZS";

    @Column(length = 500)
    private String failureReason;

    @Column(length = 500)
    private String refundReason;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;
}
