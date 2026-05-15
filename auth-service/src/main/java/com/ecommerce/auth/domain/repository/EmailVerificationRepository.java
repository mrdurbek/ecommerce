package com.ecommerce.auth.domain.repository;

import com.ecommerce.auth.domain.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByToken(String token);

    @Modifying
    @Query("UPDATE EmailVerification ev SET ev.used = true, ev.usedAt = CURRENT_TIMESTAMP WHERE ev.id = :id")
    void markAsUsed(@Param("id") Long id);
}