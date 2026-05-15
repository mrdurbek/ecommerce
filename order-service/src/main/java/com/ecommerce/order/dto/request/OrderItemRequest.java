package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String productName;

    @Size(max = 100)
    private String productSku;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "1", message = "Quantity must be at least 1")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;
}