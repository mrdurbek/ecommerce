package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddStockRequest {

    @NotNull @Min(1)
    private Integer quantity;

    @Size(max = 500)
    private String note;
}