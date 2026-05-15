package com.ecommerce.report.messaging.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private String eventId;
    private String paymentRef;
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String reason;
    private LocalDateTime failedAt;
}