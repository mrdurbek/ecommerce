package com.ecommerce.report.messaging.consumer;

import com.ecommerce.report.domain.entity.*;
import com.ecommerce.report.domain.repository.*;
import com.ecommerce.report.messaging.event.*;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final OrderRecordRepository orderRecordRepository;
    private final OrderItemRecordRepository orderItemRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final InventoryRecordRepository inventoryRecordRepository;

    @RabbitListener(queues = "${messaging.queue.order-created}")
    @Transactional
    public void handleOrderCreated(
            OrderCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Report: OrderCreated — {}", event.getOrderNumber());
        try {
            if (orderRecordRepository.existsByOrderId(event.getOrderId())) {
                log.warn("OrderRecord already exists: {}, skipping", event.getOrderId());
                channel.basicAck(tag, false);
                return;
            }

            OrderRecord orderRecord = OrderRecord.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .userId(event.getUserId())
                    .status("PENDING")
                    .subtotal(event.getSubtotal())
                    .totalAmount(event.getTotalAmount())
                    .itemCount(event.getItems() != null ? event.getItems().size() : 0)
                    .shippingCity(event.getShippingAddress() != null
                            ? event.getShippingAddress().getCity() : null)
                    .shippingCountry(event.getShippingAddress() != null
                            ? event.getShippingAddress().getCountry() : null)
                    .orderCreatedAt(event.getCreatedAt())
                    .build();

            orderRecordRepository.save(orderRecord);

            if (event.getItems() != null) {
                List<OrderItemRecord> itemRecords = event.getItems().stream()
                        .map(item -> OrderItemRecord.builder()
                                .orderId(event.getOrderId())
                                .orderNumber(event.getOrderNumber())
                                .userId(event.getUserId())
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .productSku(item.getProductSku())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .orderCreatedAt(event.getCreatedAt())
                                .build())
                        .toList();
                orderItemRecordRepository.saveAll(itemRecords);
            }

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Report: Failed OrderCreated {}: {}", event.getOrderNumber(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queue.order-cancelled}")
    @Transactional
    public void handleOrderCancelled(
            OrderCancelledEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Report: OrderCancelled — {}", event.getOrderNumber());
        try {
            orderRecordRepository.findByOrderId(event.getOrderId()).ifPresent(record -> {
                record.setStatus("CANCELLED");
                record.setOrderCancelledAt(event.getCancelledAt());
                orderRecordRepository.save(record);
            });
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Report: Failed OrderCancelled {}: {}", event.getOrderNumber(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queue.payment-completed}")
    @Transactional
    public void handlePaymentCompleted(
            PaymentCompletedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Report: PaymentCompleted — {}", event.getPaymentRef());
        try {
            if (paymentRecordRepository.existsByPaymentRef(event.getPaymentRef())) {
                channel.basicAck(tag, false);
                return;
            }

            paymentRecordRepository.save(PaymentRecord.builder()
                    .paymentId(event.getPaymentId())
                    .paymentRef(event.getPaymentRef())
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .status("COMPLETED")
                    .method(event.getMethod())
                    .paidAt(event.getPaidAt())
                    .build());

            orderRecordRepository.findByOrderId(event.getOrderId()).ifPresent(r -> {
                r.setStatus("CONFIRMED");
                orderRecordRepository.save(r);
            });

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Report: Failed PaymentCompleted {}: {}", event.getPaymentRef(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queue.payment-failed}")
    @Transactional
    public void handlePaymentFailed(
            PaymentFailedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Report: PaymentFailed — {}", event.getPaymentRef());
        try {
            if (paymentRecordRepository.existsByPaymentRef(event.getPaymentRef())) {
                channel.basicAck(tag, false);
                return;
            }

            paymentRecordRepository.save(PaymentRecord.builder()
                    .paymentId(event.getPaymentId())
                    .paymentRef(event.getPaymentRef())
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .status("FAILED")
                    .method(event.getMethod())
                    .failureReason(event.getReason())
                    .build());

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Report: Failed PaymentFailed {}: {}", event.getPaymentRef(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queue.stock-reserved}.report")
    @Transactional
    public void handleStockReserved(
            StockReservedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Report: StockReserved — {}", event.getOrderNumber());
        try {
            if (inventoryRecordRepository.existsByOrderNumberAndStatus(
                    event.getOrderNumber(), "RESERVED")) {
                channel.basicAck(tag, false);
                return;
            }

            if (event.getItems() != null) {
                List<InventoryRecord> records = event.getItems().stream()
                        .map(item -> InventoryRecord.builder()
                                .orderId(event.getOrderId())
                                .orderNumber(event.getOrderNumber())
                                .productId(item.getProductId())
                                .productSku(item.getProductSku())
                                .quantity(item.getQuantity())
                                .status("RESERVED")
                                .eventTime(event.getReservedAt())
                                .build())
                        .toList();
                inventoryRecordRepository.saveAll(records);
            }

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Report: Failed StockReserved {}: {}", event.getOrderNumber(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = "${messaging.queue.stock-reservation-failed}.report")
    @Transactional
    public void handleStockReservationFailed(
            StockReservationFailedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Report: StockReservationFailed — {}", event.getOrderNumber());
        try {
            if (inventoryRecordRepository.existsByOrderNumberAndStatus(
                    event.getOrderNumber(), "RESERVATION_FAILED")) {
                channel.basicAck(tag, false);
                return;
            }

            inventoryRecordRepository.save(InventoryRecord.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .productId(0L)
                    .quantity(0)
                    .status("RESERVATION_FAILED")
                    .failureReason(event.getReason())
                    .eventTime(event.getFailedAt())
                    .build());

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("Report: Failed StockReservationFailed {}: {}", event.getOrderNumber(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
