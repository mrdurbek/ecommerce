package com.ecommerce.order.domain.enums;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PAID || next == CANCELLED;
            case PAID -> next == SHIPPED || next == CANCELLED;
            case SHIPPED ->  next == DELIVERED;
            case DELIVERED , CANCELLED -> false;
        };
    }
}
