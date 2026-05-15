package com.ecommerce.order.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderCancelledEvent {
    private String eventId;
    private String orderNumber;
    private Long orderId;
    private Long userId;
    private String reason;
    private LocalDateTime cancelledAt;
}