package com.ecommerce.report.messaging.event;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationFailedEvent {
    private String eventId;
    private String orderNumber;
    private Long orderId;
    private String reason;
    private LocalDateTime failedAt;
}