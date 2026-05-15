package com.ecommerce.inventory.messaging.consumer;

import com.ecommerce.inventory.domain.repository.ProcessedEventRepository;
import com.ecommerce.inventory.messaging.event.OrderCancelledEvent;
import com.ecommerce.inventory.messaging.event.OrderCreatedEvent;
import com.ecommerce.inventory.service.InventoryService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final InventoryService inventoryService;
    private final ProcessedEventRepository processedEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange.order}")
    private String orderExchange;

    @Value("${messaging.routing-key.order-created}")
    private String orderCreatedKey;

    @Value("${messaging.routing-key.order-cancelled}")
    private String orderCancelledKey;

    @RabbitListener(queues = "${messaging.queue.order-created}")
    public void handleOrderCreated(
            OrderCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath
    ) throws IOException {
        if (isAlreadyProcessed(event.getEventId())) {
            log.info("Skipping already processed OrderCreatedEvent: {}", event.getEventId());
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Received OrderCreatedEvent: {}", event.getOrderNumber());
            inventoryService.handleOrderCreated(event);
            saveProcessedEvent(event.getEventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int retryCount = extractRetryCount(xDeath);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry reached for OrderCreatedEvent {}. Sending to DLQ.", event.getEventId(), e);
                sendToDeadLetter(orderCreatedKey + ".dead", event);
                channel.basicAck(tag, false);
            } else {
                log.error("Failed to handle OrderCreatedEvent for {}: {}. Retry {}.", event.getOrderNumber(), e.getMessage(), retryCount + 1, e);
                channel.basicNack(tag, false, false);
            }
        }
    }

    @RabbitListener(queues = "${messaging.queue.order-cancelled}")
    public void handleOrderCancelled(
            OrderCancelledEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath) throws IOException {
        if (isAlreadyProcessed(event.getEventId())) {
            log.info("Skipping already processed OrderCancelledEvent: {}", event.getEventId());
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Received OrderCancelledEvent: {}", event.getOrderNumber());
            inventoryService.handleOrderCancelled(event);
            saveProcessedEvent(event.getEventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int retryCount = extractRetryCount(xDeath);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry reached for OrderCancelledEvent {}. Sending to DLQ.", event.getEventId(), e);
                sendToDeadLetter(orderCancelledKey + ".dead", event);
                channel.basicAck(tag, false);
            } else {
                log.error("Failed to handle OrderCancelledEvent for {}: {}. Retry {}.", event.getOrderNumber(), e.getMessage(), retryCount + 1, e);
                channel.basicNack(tag, false, false);
            }
        }
    }

    private boolean isAlreadyProcessed(String eventId) {
        return eventId != null && !eventId.isBlank() && processedEventRepository.existsByEventId(eventId);
    }

    private void saveProcessedEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        var processedEvent = com.ecommerce.inventory.domain.entity.ProcessedEvent.builder()
                .eventId(eventId)
                .build();
        processedEventRepository.save(processedEvent);
    }

    private int extractRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) {
            return 0;
        }
        return xDeath.stream()
                .map(entry -> entry.get("count"))
                .filter(count -> count instanceof Number)
                .mapToInt(count -> ((Number) count).intValue())
                .sum();
    }

    private void sendToDeadLetter(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(orderExchange + ".dlx", routingKey, event);
    }
}
