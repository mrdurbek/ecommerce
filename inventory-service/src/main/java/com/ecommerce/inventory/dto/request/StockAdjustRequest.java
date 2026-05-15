package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StockAdjustRequest {

    @NotNull
    private Integer delta;

    @NotBlank @Size(max = 500)
    private String reason;
}