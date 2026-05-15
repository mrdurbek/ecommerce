package com.ecommerce.report.messaging.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private String eventId;
    private String orderNumber;
    private Long orderId;
    private Long userId;
    private BigDecimal subtotal;
    private BigDecimal totalAmount;
    private List<OrderItemEvent> items;
    private ShippingAddressEvent shippingAddress;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressEvent {
        private String city;
        private String country;
    }
}