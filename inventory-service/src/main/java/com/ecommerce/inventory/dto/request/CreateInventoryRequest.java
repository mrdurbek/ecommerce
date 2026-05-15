package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateInventoryRequest {

    @NotNull @Positive
    private Long productId;

    @NotBlank @Size(max = 255)
    private String productName;

    @NotBlank @Size(max = 100)
    private String sku;

    @NotNull @Min(0)
    private Integer initialQuantity;

    @Min(0)
    private Integer lowStockThreshold = 10;
}