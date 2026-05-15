package com.ecommerce.inventory.domain.entity;

import com.ecommerce.inventory.audit.BaseEntity;
import com.ecommerce.inventory.domain.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 50)
    private String referenceType;

    @Column(length = 100)
    private String referenceId;

    @Column(length = 500)
    private String note;
}