package com.ecommerce.order.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCreatedEvent {
    private String eventId;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal subtotal;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemEvent> items;
    private ShippingAddressEvent shippingAddress;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
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
