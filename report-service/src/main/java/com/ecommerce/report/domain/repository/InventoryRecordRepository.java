package com.ecommerce.report.domain.repository;

import com.ecommerce.report.domain.entity.InventoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRecordRepository extends JpaRepository<InventoryRecord, Long> {

    List<InventoryRecord> findByOrderId(Long orderId);

    List<InventoryRecord> findByProductId(Long productId);

    boolean existsByOrderNumberAndStatus(String orderNumber, String status);
}