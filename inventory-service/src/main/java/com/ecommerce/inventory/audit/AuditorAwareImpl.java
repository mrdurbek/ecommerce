package com.ecommerce.inventory.audit;

import com.ecommerce.inventory.util.SecurityUtils;
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
        Long userId = securityUtils.getCurrentUserId();
        if(userId  == null) {
            return Optional.of(-1l);
        }
        return Optional.of(userId);
    }
}