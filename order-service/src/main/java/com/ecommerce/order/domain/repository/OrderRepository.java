package com.ecommerce.order.domain.repository;

import com.ecommerce.order.domain.entity.Order;
import com.ecommerce.order.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserIdAndStatus(Pageable pageable, Long userId, OrderStatus status);

    Page<Order> findByUserId(Pageable pageable, Long userId);

    Page<Order> findByStatus(Pageable pageable, OrderStatus status);

    boolean existsByOrderNumber(String orderNumber);
}
