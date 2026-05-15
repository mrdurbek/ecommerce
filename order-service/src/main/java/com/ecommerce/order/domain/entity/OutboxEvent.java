package com.ecommerce.order.domain.entity;

import com.ecommerce.order.audit.BaseEntity;
import com.ecommerce.order.domain.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEvent extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private Long aggregateId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    private Integer retryCount = 0;

    private LocalDateTime sentAt;
}
