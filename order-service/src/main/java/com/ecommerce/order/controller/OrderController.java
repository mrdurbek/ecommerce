package com.ecommerce.order.controller;

import com.ecommerce.order.domain.enums.OrderStatus;
import com.ecommerce.order.dto.request.*;
import com.ecommerce.order.dto.response.*;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        OrderResponse order = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Get current user's orders with pagination")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PageResponse<OrderResponse> orders = orderService.getMyOrders(userId, status, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orders));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        OrderResponse order = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", order));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(
            @PathVariable String orderNumber,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        OrderResponse order = orderService.getOrderByNumber(orderNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", order));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        OrderResponse order = orderService.cancelOrder(orderId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ORDER_VIEW_ALL')")
    @Operation(summary = "[ADMIN] Get all orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction
    ) {
        PageResponse<OrderResponse> orders = orderService.getAllOrders(status, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success("All orders retrieved", orders));
    }

    @PatchMapping("/admin/{orderId}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    @Operation(summary = "[ADMIN] Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        Long adminId = (Long) httpRequest.getAttribute("userId");
        OrderResponse order = orderService.updateOrderStatus(orderId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }
}