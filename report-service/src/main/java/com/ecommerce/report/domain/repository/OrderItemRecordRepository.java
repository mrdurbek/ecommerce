package com.ecommerce.report.domain.repository;

import com.ecommerce.report.domain.entity.OrderItemRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRecordRepository extends JpaRepository<OrderItemRecord, Long> {
}
