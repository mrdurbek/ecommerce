package com.ecommerce.report.messaging.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderCancelledEvent {
    private String eventId;
    private String orderNumber;
    private Long orderId;
    private Long userId;
    private String reason;
    private LocalDateTime cancelledAt;
}