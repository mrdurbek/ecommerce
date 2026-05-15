package com.ecommerce.inventory.domain.entity;

import com.ecommerce.inventory.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public boolean hasEnoughStock(int required) {
        return quantityAvailable >= required;
    }

    public boolean isLowStock() {
        return quantityAvailable <= lowStockThreshold;
    }

    public void reserve(int qty) {
        if (!hasEnoughStock(qty)) {
            throw new IllegalStateException(
                    String.format("Insufficient stock: sku=%s, available=%d, required=%d",
                            sku, quantityAvailable, qty));
        }
        quantityAvailable -= qty;
        quantityReserved  += qty;
    }

    public void release(int qty) {
        int actual = Math.min(qty, quantityReserved);
        quantityReserved  -= actual;
        quantityAvailable += actual;
    }

    public void addStock(int qty) {
        quantityAvailable += qty;
    }

    public void adjust(int delta) {
        if (quantityAvailable + delta < 0) {
            throw new IllegalStateException("Adjustment would result in negative stock");
        }
        quantityAvailable += delta;
    }
}