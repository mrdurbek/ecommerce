package com.ecommerce.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.rate-limit.login-attempts}")
    private int loginAttempts;

    @Value("${app.rate-limit.login-window-minutes}")
    private int loginWindowMinutes;

    @Value("${app.rate-limit.register-attempts}")
    private int registerAttempts;

    @Value("${app.rate-limit.register-window-minutes}")
    private int registerWindowMinutes;

    private static final String LOGIN_RATE_KEY = "rate:login:";
    private static final String REGISTER_RATE_KEY = "rate:register:";

    public boolean isLoginAllowed(String ipAddress) {
        return isAllowed(LOGIN_RATE_KEY + ipAddress, loginAttempts, Duration.ofMinutes(loginWindowMinutes));
    }

    public boolean isRegisterAllowed(String ipAddress) {
        return isAllowed(REGISTER_RATE_KEY + ipAddress, registerAttempts, Duration.ofMinutes(registerWindowMinutes));
    }

    public long getLoginRemainingAttempts(String ipAddress) {
        return getRemainingAttempts(LOGIN_RATE_KEY + ipAddress, loginAttempts);
    }

    private boolean isAllowed(String key, int maxAttempts, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, window);
            }
            return count != null && count <= maxAttempts;
        } catch (Exception e) {
            log.warn("Rate limiting check failed, allowing request: {}", e.getMessage());
            return true;
        }
    }

    private long getRemainingAttempts(String key, int maxAttempts) {
        try {
            Object count = redisTemplate.opsForValue().get(key);
            if (count == null) return maxAttempts;
            long current = Long.parseLong(count.toString());
            return Math.max(0, maxAttempts - current);
        } catch (Exception e) {
            return maxAttempts;
        }
    }

    public void resetLoginRateLimit(String ipAddress) {
        try {
            redisTemplate.delete(LOGIN_RATE_KEY + ipAddress);
        } catch (Exception e) {
            log.warn("Could not reset rate limit: {}", e.getMessage());
        }
    }
}