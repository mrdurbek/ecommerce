package com.ecommerce.order.domain.entity;

import com.ecommerce.order.domain.enums.OrderStatus;
import com.ecommerce.order.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(nullable = false,unique = true, length = 30)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancelledReason;

    @OneToMany(mappedBy = "order" , cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ShippingAddress shippingAddress;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void setShipping(ShippingAddress address) {
        address.setOrder(this);
        this.shippingAddress = address;
    }

    public void transitionStatus(OrderStatus newStatus) {
        if (!this.status.canTransitTo(newStatus)) {
            throw new IllegalStateException("You cannot transit this status to " + newStatus.name());
        }

        this.status = newStatus;

        switch (newStatus) {
            case CONFIRMED -> this.confirmedAt = LocalDateTime.now();
            case PAID -> this.paidAt = LocalDateTime.now();
            case SHIPPED -> this.shippedAt = LocalDateTime.now();
            case DELIVERED -> this.deliveredAt = LocalDateTime.now();
            case CANCELLED -> this.cancelledAt = LocalDateTime.now();
            default -> {}
        }
    }
}
