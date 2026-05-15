package com.ecommerce.report.domain.repository;

import com.ecommerce.report.domain.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {

    Optional<OrderRecord> findByOrderId(Long orderId);

    Optional<OrderRecord> findByOrderNumber(String orderNumber);

    boolean existsByOrderId(Long orderId);
}