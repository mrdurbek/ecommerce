package com.ecommerce.inventory.domain.repository;

import com.ecommerce.inventory.domain.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductId(Long productId);

    Optional<InventoryItem> findBySku(String sku);

    boolean existsByProductId(Long productId);

    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId")
    Optional<InventoryItem> findByProductIdWithLock(@Param("productId") Long productId);

    @Query("SELECT i FROM InventoryItem i WHERE i.isActive = true AND i.quantityAvailable <= i.lowStockThreshold")
    List<InventoryItem> findLowStockItems();

    Page<InventoryItem> findByIsActive(Boolean isActive, Pageable pageable);
}