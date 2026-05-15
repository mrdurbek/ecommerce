package com.ecommerce.report.domain.repository;

import com.ecommerce.report.domain.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByPaymentRef(String paymentRef);

    boolean existsByPaymentRef(String paymentRef);
}