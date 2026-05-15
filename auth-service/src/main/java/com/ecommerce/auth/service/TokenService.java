package com.ecommerce.auth.service;

import com.ecommerce.auth.domain.entity.RefreshToken;
import com.ecommerce.auth.domain.entity.User;
import com.ecommerce.auth.domain.repository.RefreshTokenRepository;
import com.ecommerce.auth.exception.TokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken validateAndRotateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            if (refreshToken.isRevoked()) {
                log.warn("Refresh token reuse detected for user: {}", refreshToken.getUser().getEmail());
                refreshTokenRepository.revokeAllByUserId(refreshToken.getUser().getId(), LocalDateTime.now());
                throw new TokenException("Refresh token was already used. All sessions have been invalidated.");
            }
            throw new TokenException("Refresh token has expired. Please login again.");
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return createRefreshToken(
                refreshToken.getUser(),
                refreshToken.getDeviceInfo(),
                refreshToken.getIpAddress()
        );
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }

    public void blacklistAccessToken(String token, long expirationMs) {
        try {
            redisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + token, "true", expirationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Could not blacklist token: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running cleanup for expired refresh tokens...");
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
    }
}