package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.entity.OutboxEvent;
import com.ecommerce.order.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusAndRetryCountLessThan(OutboxStatus status, int maxRetryCount);
}
