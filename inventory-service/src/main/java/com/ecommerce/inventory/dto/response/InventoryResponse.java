package com.ecommerce.inventory.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Integer quantityAvailable;
    private Integer quantityReserved;
    private Integer lowStockThreshold;
    private Boolean isActive;
    private Boolean isLowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}