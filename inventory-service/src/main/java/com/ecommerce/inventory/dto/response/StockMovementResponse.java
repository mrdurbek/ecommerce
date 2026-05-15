package com.ecommerce.inventory.dto.response;

import com.ecommerce.inventory.domain.enums.MovementType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private Long id;
    private Long productId;
    private MovementType movementType;
    private Integer quantity;
    private String referenceType;
    private String referenceId;
    private String note;
    private LocalDateTime createdAt;
}