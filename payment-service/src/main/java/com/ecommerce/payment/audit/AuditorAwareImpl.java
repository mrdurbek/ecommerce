package com.ecommerce.payment.audit;

import com.ecommerce.payment.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<Long> {

    private final SecurityUtils securityUtils;

    @Override
    public Optional<Long> getCurrentAuditor() {
        if (securityUtils.getCurrentUserId() != null) {
            return Optional.of(securityUtils.getCurrentUserId());
        }
        return Optional.of(-1L);
    }
}