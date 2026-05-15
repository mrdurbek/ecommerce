package com.ecommerce.payment.controller;

import com.ecommerce.payment.domain.enums.PaymentStatus;
import com.ecommerce.payment.dto.response.ApiResponse;
import com.ecommerce.payment.dto.request.PaymentRequest;
import com.ecommerce.payment.dto.response.PaymentResponse;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment management")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Initiate a payment for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        PaymentResponse response = paymentService.pay(request, userId);
        HttpStatus status = response.getStatus() == PaymentStatus.COMPLETED
                ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(ApiResponse.success("Payment processed", response));
    }

    @GetMapping("/ref/{paymentRef}")
    @Operation(summary = "Get payment by reference")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByRef(@PathVariable String paymentRef) {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getByRef(paymentRef)));
    }

    @GetMapping("/order/{orderNumber}")
    @Operation(summary = "Get payment by order number")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getByOrderNumber(orderNumber)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getMyPayments(userId, page, size)));
    }

    @PostMapping("/ref/{paymentRef}/refund")
    @Operation(summary = "Refund a completed payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable String paymentRef,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refunded", paymentService.refund(paymentRef, request)));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ORDER_VIEW_ALL')")
    @Operation(summary = "[ADMIN] Get all payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAll(
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("OK", paymentService.getAll(status, page, size)));
    }
}