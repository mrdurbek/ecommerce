package com.ecommerce.order.service;

import com.ecommerce.order.domain.entity.*;
import com.ecommerce.order.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .cancelledReason(order.getCancelledReason())
                .items(toItemResponses(order.getItems()))
                .shippingAddress(toShippingResponse(order.getShippingAddress()))
                .confirmedAt(order.getConfirmedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private List<OrderItemResponse> toItemResponses(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream().map(item -> OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .quantity(item.getQty())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build()).toList();
    }

    private ShippingAddressResponse toShippingResponse(ShippingAddress addr) {
        if (addr == null) return null;
        return ShippingAddressResponse.builder()
                .id(addr.getId())
                .fullName(addr.getFullName())
                .phoneNumber(addr.getPhoneNumber())
                .country(addr.getCountry())
                .city(addr.getCity())
                .district(addr.getDistrict())
                .street(addr.getStreet())
                .apartment(addr.getApartment())
                .zipCode(addr.getZipCode())
                .formatted(addr.getFormatted())
                .build();
    }
}