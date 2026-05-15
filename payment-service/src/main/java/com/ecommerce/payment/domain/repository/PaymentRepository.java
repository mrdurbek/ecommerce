package com.ecommerce.payment.domain.repository;

import com.ecommerce.payment.domain.entity.Payment;
import com.ecommerce.payment.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentRef(String paymentRef);

    Optional<Payment> findByOrderNumber(String orderNumber);

    Page<Payment> findByUserId(Long userid, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    boolean existsByOrderNumberAndStatus(String orderNumber, PaymentStatus status);
}
