package com.ecommerce.report.messaging.event;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservedEvent {
    private String eventId;
    private String orderNumber;
    private Long orderId;
    private boolean success;
    private List<StockItemEvent> items;
    private LocalDateTime reservedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItemEvent {
        private Long productId;
        private String productSku;
        private Integer quantity;
    }
}