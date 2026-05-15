package com.ecommerce.inventory.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockReservationFailedEvent {
    private String eventId;
    private Long orderId;
    private String orderNumber;
    private String reason;
    private LocalDateTime failedAt;
}
