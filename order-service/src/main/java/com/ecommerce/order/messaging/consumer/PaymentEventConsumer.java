package com.ecommerce.order.messaging.consumer;

import com.ecommerce.order.domain.entity.ProcessedEvent;
import com.ecommerce.order.domain.repository.ProcessedEventRepository;
import com.ecommerce.order.messaging.event.PaymentCompletedEvent;
import com.ecommerce.order.messaging.event.PaymentFailedEvent;
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
public class PaymentEventConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final OrderService orderService;
    private final ProcessedEventRepository processedEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange.payment}")
    private String paymentExchange;

    @Value("${messaging.routing-key.payment-completed}")
    private String paymentCompletedKey;

    @Value("${messaging.routing-key.payment-failed}")
    private String paymentFailedKey;

    @RabbitListener(queues = "${messaging.queue.payment-completed}")
    public void handlePaymentCompleted(
            PaymentCompletedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath
    ) throws IOException {
        if (isAlreadyProcessed(event.getEventId())) {
            log.info("Skipping already processed PaymentCompletedEvent: {}", event.getEventId());
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Received PaymentCompletedEvent for order {}", event.getOrderNumber());
            orderService.handlePaymentCompleted(event);
            saveProcessedEvent(event.getEventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int retryCount = extractRetryCount(xDeath);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry reached for PaymentCompletedEvent {}. Sending to DLQ.", event.getEventId(), e);
                sendToDeadLetter(paymentCompletedKey + ".dead", event);
                channel.basicAck(tag, false);
            } else {
                log.error("Failed to process PaymentCompletedEvent for order {}: {}. Retry {}.", event.getOrderNumber(), e.getMessage(), retryCount + 1, e);
                channel.basicNack(tag, false, false);
            }
        }
    }

    @RabbitListener(queues = "${messaging.queue.payment-failed}")
    public void handlePaymentFailed(
            PaymentFailedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(value = "x-death", required = false) List<Map<String, Object>> xDeath
    ) throws IOException {
        if (isAlreadyProcessed(event.getEventId())) {
            log.info("Skipping already processed PaymentFailedEvent: {}", event.getEventId());
            channel.basicAck(tag, false);
            return;
        }

        try {
            log.info("Received PaymentFailedEvent for order {}", event.getOrderNumber());
            orderService.handlePaymentFailed(event);
            saveProcessedEvent(event.getEventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int retryCount = extractRetryCount(xDeath);
            if (retryCount >= MAX_RETRY_COUNT) {
                log.error("Max retry reached for PaymentFailedEvent {}. Sending to DLQ.", event.getEventId(), e);
                sendToDeadLetter(paymentFailedKey + ".dead", event);
                channel.basicAck(tag, false);
            } else {
                log.error("Failed to process PaymentFailedEvent for order {}: {}. Retry {}.", event.getOrderNumber(), e.getMessage(), retryCount + 1, e);
                channel.basicNack(tag, false, false);
            }
        }
    }

    private boolean isAlreadyProcessed(String eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    private void saveProcessedEvent(String eventId) {
        ProcessedEvent event = new ProcessedEvent();
        event.setEventId(eventId);
        processedEventRepository.save(event);
    }

    private int extractRetryCount(List<Map<String, Object>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) {
            return 0;
        }
        Object countObject = xDeath.get(0).get("count");
        if (countObject instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private void sendToDeadLetter(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(paymentExchange + ".dlx", routingKey, event);
        } catch (Exception e) {
            log.error("Failed to send event to dead letter queue: {}", routingKey, e);
        }
    }
}
