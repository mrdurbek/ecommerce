package com.ecommerce.order.messaging.consumer;

import com.ecommerce.order.domain.entity.ProcessedEvent;
import com.ecommerce.order.domain.repository.ProcessedEventRepository;
import com.ecommerce.order.messaging.event.StockReservedEvent;
import com.ecommerce.order.messaging.event.StockReservationFailedEvent;
import com.ecommerce.order.service.OrderService;
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
public class InventoryEventConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange.inventory}")
    private String inventoryExchange;

    @Value("${messaging.routing-key.stock-reserved}")
    private String stockReservedKey;

    @Value("${messaging.routing-key.stock-reservation-failed}")
    private String stockReservationFailedKey;

    @RabbitListener(queues = "${messaging.queue.stock-reserved}")
    public void handleStockReserved(
            StockReservedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath
    ) throws IOException {
        if (isAlreadyProcessed(event.getEventId())) {
            log.info("Skipping already processed StockReservedEvent: {}", event.getEventId());
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Received StockReservedEvent for order {}", event.getOrderNumber());
            orderService.handleStockReserved(event);
            saveProcessedEvent(event.getEventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int retryCount = extractRetryCount(xDeath);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry reached for StockReservedEvent {}. Sending to DLQ.", event.getEventId(), e);
                sendToDeadLetter(stockReservedKey + ".dead", event);
                channel.basicAck(tag, false);
            } else {
                log.error("Failed to process StockReservedEvent for order {}: {}. Retry {}.", event.getOrderNumber(), e.getMessage(), retryCount + 1, e);
                channel.basicNack(tag, false, false);
            }
        }
    }

    @RabbitListener(queues = "${messaging.queue.stock-reservation-failed}")
    public void handleStockReservationFailed(
            StockReservationFailedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath
    ) throws IOException {
        if (isAlreadyProcessed(event.getEventId())) {
            log.info("Skipping already processed StockReservationFailedEvent: {}", event.getEventId());
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Received StockReservationFailedEvent for order {}", event.getOrderNumber());
            orderService.handleStockReservationFailed(event);
            saveProcessedEvent(event.getEventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int retryCount = extractRetryCount(xDeath);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry reached for StockReservationFailedEvent {}. Sending to DLQ.", event.getEventId(), e);
                sendToDeadLetter(stockReservationFailedKey + ".dead", event);
                channel.basicAck(tag, false);
            } else {
                log.error("Failed to process StockReservationFailedEvent for order {}: {}. Retry {}.", event.getOrderNumber(), e.getMessage(), retryCount + 1, e);
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
        var processedEvent = ProcessedEvent.builder()
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
        rabbitTemplate.convertAndSend(inventoryExchange + ".dlx", routingKey, event);
    }
}
