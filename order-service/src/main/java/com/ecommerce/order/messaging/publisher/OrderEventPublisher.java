package com.ecommerce.order.messaging.publisher;

import com.ecommerce.order.domain.entity.OutboxEvent;
import com.ecommerce.order.domain.enums.OutboxStatus;
import com.ecommerce.order.domain.repository.OutboxEventRepository;
import com.ecommerce.order.messaging.event.OrderCancelledEvent;
import com.ecommerce.order.messaging.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange.order}")
    private String orderExchange;

    @Value("${messaging.routing-key.order-created}")
    private String orderCreatedKey;

    @Value("${messaging.routing-key.order-cancelled}")
    private String orderCancelledKey;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveOrderCreatedEvent(OrderCreatedEvent event) {
        saveToOutbox(event.getOrderId(), "Order", "ORDER_CREATED", event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveOrderCancelledEvent(OrderCancelledEvent event) {
        saveToOutbox(event.getOrderId(), "Order", "ORDER_CANCELLED", event);
    }

    private void saveToOutbox(Long aggregateId, String aggregateType, String eventType, Object event) {

        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }

    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusAndRetryCountLessThan(OutboxStatus.PENDING, 3);
        for (OutboxEvent event : pending) {
            try {
                String routingKey = resolveRoutingKey(event.getEventType());
                Object eventObject = deserializeEventPayload(event.getEventType(), event.getPayload());
                rabbitTemplate.convertAndSend(orderExchange, routingKey, eventObject);

                event.setStatus(OutboxStatus.SENT);
                event.setSentAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.debug("Published outbox event: {} for aggregate: {}",
                        event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event {} permanently failed after 3 retries", event.getId());
                }
                outboxEventRepository.save(event);
            }
        }
    }

    private Object deserializeEventPayload(String eventType, String payload) throws JsonProcessingException {
        return switch (eventType) {
            case "ORDER_CREATED" -> objectMapper.readValue(payload, OrderCreatedEvent.class);
            case "ORDER_CANCELLED" -> objectMapper.readValue(payload, OrderCancelledEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }

    private String resolveRoutingKey(String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED" -> orderCreatedKey;
            case "ORDER_CANCELLED" -> orderCancelledKey;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
