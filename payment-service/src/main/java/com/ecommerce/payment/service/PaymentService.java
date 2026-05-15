package com.ecommerce.payment.service;
import com.ecommerce.payment.domain.entity.Payment;
import com.ecommerce.payment.domain.enums.PaymentStatus;
import com.ecommerce.payment.domain.repository.PaymentRepository;
import com.ecommerce.payment.dto.request.PaymentRequest;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentResponse;
import com.ecommerce.payment.gateway.MockPaymentGateway;
import com.ecommerce.payment.messaging.event.PaymentCompletedEvent;
import com.ecommerce.payment.messaging.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway gateway;
    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange.payment}")
    private String paymentExchange;

    @Value("${messaging.routing-key.payment-completed}")
    private String completedKey;

    @Value("${messaging.routing-key.payment-failed}")
    private String failedKey;

    private final AtomicInteger sequence = new AtomicInteger(0);

    @Transactional
    public PaymentResponse pay(PaymentRequest request, Long userId) {
        if (paymentRepository.existsByOrderNumberAndStatus(request.getOrderNumber(), PaymentStatus.COMPLETED)) {
            throw new IllegalStateException("Order already paid: " + request.getOrderNumber());
        }

        String ref = generateRef();

        Payment payment = Payment.builder()
                .paymentRef(ref)
                .orderId(request.getOrderId())
                .orderNumber(request.getOrderNumber())
                .userId(userId)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .method(request.getMethod())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        MockPaymentGateway.GatewayResult result = gateway.process(ref, request.getAmount(), request.getMethod());

        if (result.success()) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            rabbitTemplate.convertAndSend(paymentExchange, completedKey,
                    PaymentCompletedEvent.builder()
                            .paymentRef(payment.getPaymentRef())
                            .paymentId(payment.getId())
                            .orderId(payment.getOrderId())
                            .orderNumber(payment.getOrderNumber())
                            .userId(payment.getUserId())
                            .amount(payment.getAmount())
                            .currency(payment.getCurrency())
                            .method(payment.getMethod())
                            .paidAt(payment.getPaidAt())
                            .build());

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.errorMessage());
            paymentRepository.save(payment);

            rabbitTemplate.convertAndSend(paymentExchange, failedKey,
                    PaymentFailedEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .paymentRef(ref)
                            .orderId(request.getOrderId())
                            .orderNumber(request.getOrderNumber())
                            .reason(result.errorMessage())
                            .failedAt(LocalDateTime.now())
                            .build());
        }

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refund(String paymentRef, RefundRequest request) {
        Payment payment = findByRef(paymentRef);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED payments can be refunded");
        }

        MockPaymentGateway.GatewayResult result = gateway.refund(paymentRef, payment.getAmount());

        if (result.success()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundReason(request.getReason());
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Payment {} refunded", paymentRef);
        } else {
            throw new RuntimeException("Refund failed: " + result.errorMessage());
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByRef(String paymentRef) {
        return toResponse(findByRef(paymentRef));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderNumber(String orderNumber) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderNumber));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return paymentRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAll(PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return status != null
                ? paymentRepository.findByStatus(status, pageable).map(this::toResponse)
                : paymentRepository.findAll(pageable).map(this::toResponse);
    }

    private Payment findByRef(String ref) {
        return paymentRepository.findByPaymentRef(ref)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + ref));
    }

    private String generateRef() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("PAY-%s-%06d", date, sequence.incrementAndGet() % 1_000_000);
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentRef(p.getPaymentRef())
                .orderId(p.getOrderId())
                .orderNumber(p.getOrderNumber())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .method(p.getMethod())
                .failureReason(p.getFailureReason())
                .refundReason(p.getRefundReason())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}