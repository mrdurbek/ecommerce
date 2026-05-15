package com.ecommerce.order.service;

import com.ecommerce.order.domain.entity.*;
import com.ecommerce.order.domain.enums.OrderStatus;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.dto.request.*;
import com.ecommerce.order.dto.response.*;
import com.ecommerce.order.exception.*;
import com.ecommerce.order.messaging.event.OrderCancelledEvent;
import com.ecommerce.order.messaging.event.OrderCreatedEvent;
import com.ecommerce.order.messaging.event.PaymentCompletedEvent;
import com.ecommerce.order.messaging.event.PaymentFailedEvent;
import com.ecommerce.order.messaging.event.StockReservedEvent;
import com.ecommerce.order.messaging.event.StockReservationFailedEvent;
import com.ecommerce.order.messaging.publisher.OrderEventPublisher;
import com.ecommerce.order.util.OrderNumberGenerator;
import com.ecommerce.order.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderMapper orderMapper;
    private final SecurityUtils securityUtils;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        BigDecimal subtotal = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderNumber(generateUniqueOrderNumber())
                .userId(userId)
                .subtotal(subtotal)
                .totalAmount(subtotal)
                .notes(request.getNotes())
                .build();

        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .productSku(itemReq.getProductSku())
                    .qty(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(itemReq.getUnitPrice().multiply(itemReq.getQuantity()))
                    .build();
            order.addItem(item);
        }

        ShippingAddressRequest addrReq = request.getShippingAddress();
        ShippingAddress address = ShippingAddress.builder()
                .fullName(addrReq.getFullName())
                .phoneNumber(addrReq.getPhoneNumber())
                .country(addrReq.getCountry())
                .city(addrReq.getCity())
                .district(addrReq.getDistrict())
                .street(addrReq.getStreet())
                .apartment(addrReq.getApartment())
                .zipCode(addrReq.getZipCode())
                .build();
        order.setShipping(address);

        order = orderRepository.save(order);

        OrderCreatedEvent event = buildOrderCreatedEvent(order);
        eventPublisher.saveOrderCreatedEvent(event);

        log.info("Order created: {} for user: {}", order.getOrderNumber(), userId);
        return orderMapper.toResponse(order);
    }


    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = securityUtils.isAdmin()
                ? orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId))
                : orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (!securityUtils.isAdmin() && !order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + orderNumber);
        }

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Long userId, OrderStatus status,
                                                   int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> orders = status != null
                ? orderRepository.findByUserIdAndStatus(pageable, userId, status)
                : orderRepository.findByUserId(pageable, userId);

        return PageResponse.of(orders.map(orderMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size,
                                                    String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> orders = status != null
                ? orderRepository.findByStatus(pageable, status)
                : orderRepository.findAll(pageable);

        return PageResponse.of(orders.map(orderMapper::toResponse));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        try {
            order.transitionStatus(request.getNewStatus());
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }

        order = orderRepository.save(order);
        log.info("Order {} status updated to {} by admin {}", order.getOrderNumber(), request.getNewStatus(), adminId);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request, Long userId) {
        Order order = securityUtils.isAdmin()
                ? orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId))
                : orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        try {
            order.setCancelledReason(request.getReason());
            order.transitionStatus(OrderStatus.CANCELLED);
        } catch (IllegalStateException e) {
            throw new BadRequestException(e.getMessage());
        }

        order = orderRepository.save(order);

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderNumber(order.getOrderNumber())
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(request.getReason())
                .cancelledAt(order.getCancelledAt())
                .build();
        eventPublisher.saveOrderCancelledEvent(event);

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
        return orderMapper.toResponse(order);
    }

    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Skipping stock reserved event for order {} because status is {}",
                    order.getOrderNumber(), order.getStatus());
            return;
        }

        order.transitionStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} confirmed after stock reservation", order.getOrderNumber());
    }

    @Transactional
    public void handleStockReservationFailed(StockReservationFailedEvent event) {
        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Skipping stock reservation failed event for order {} because status is {}",
                    order.getOrderNumber(), order.getStatus());
            return;
        }

        order.setCancelledReason("Stock reservation failed: " + event.getReason());
        order.transitionStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderCancelledEvent cancelledEvent = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .reason(event.getReason())
                .cancelledAt(order.getCancelledAt())
                .build();
        eventPublisher.saveOrderCancelledEvent(cancelledEvent);

        log.info("Order {} cancelled because stock reservation failed: {}", order.getOrderNumber(), event.getReason());
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.info("Skipping payment completed event for order {} because status is {}",
                    order.getOrderNumber(), order.getStatus());
            return;
        }

        order.transitionStatus(OrderStatus.PAID);
        order.setPaidAt(event.getPaidAt());
        orderRepository.save(order);
        log.info("Order {} status updated to PAID. Payment ref: {}", order.getOrderNumber(), event.getPaymentRef());
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        var order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.info("Skipping payment failed event for order {} because status is {}",
                    order.getOrderNumber(), order.getStatus());
            return;
        }

        order.setCancelledReason("Payment failed: " + event.getReason());
        order.transitionStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderCancelledEvent cancelledEvent = OrderCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .reason(event.getReason())
                .cancelledAt(order.getCancelledAt())
                .build();
        eventPublisher.saveOrderCancelledEvent(cancelledEvent);

        log.info("Order {} cancelled because payment failed: {}", order.getOrderNumber(), event.getReason());
    }

    private String generateUniqueOrderNumber() {
        String number;
        do {
            number = orderNumberGenerator.generate();
        } while (orderRepository.existsByOrderNumber(number));
        return number;
    }

    private OrderCreatedEvent buildOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .quantity(item.getQty().intValueExact())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();

        OrderCreatedEvent.ShippingAddressEvent shippingEvent = null;
        if (order.getShippingAddress() != null) {
            shippingEvent = OrderCreatedEvent.ShippingAddressEvent.builder()
                    .city(order.getShippingAddress().getCity())
                    .country(order.getShippingAddress().getCountry())
                    .build();
        }

        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderNumber(order.getOrderNumber())
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(itemEvents)
                .shippingAddress(shippingEvent)
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .build();
    }
}