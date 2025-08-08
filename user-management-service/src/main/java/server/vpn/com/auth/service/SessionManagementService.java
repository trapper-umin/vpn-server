package server.vpn.com.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.auth.entity.BlacklistedToken;
import server.vpn.com.auth.entity.RefreshToken;
import server.vpn.com.auth.entity.User;
import server.vpn.com.auth.exception.InvalidCredentialsException;
import server.vpn.com.auth.repository.BlacklistedTokenRepository;
import server.vpn.com.auth.repository.RefreshTokenRepository;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static server.vpn.com.auth.util.enums.Constant.*;

/**
 * Сервис для управления refresh токенами
 * Обеспечивает безопасное создание, валидацию и отзыв токенов
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration:2592000000}")
    private long refreshTokenExpirationMs;

    @Value("${app.security.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    @Transactional
    public RefreshToken createRefreshToken(User user, String deviceInfo, String ipAddress) {

        OffsetDateTime now = OffsetDateTime.now();
        long activeSessions = refreshTokenRepository.countActiveSessionsByUser(user, now);
        
        if (activeSessions >= maxSessionsPerUser) {
            cleanupOldestTokensForUser(user, maxSessionsPerUser - 1);
        }

        String token = generateSecureToken();
        OffsetDateTime expiresAt = now.plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
            .token(token)
            .user(user)
            .expiresAt(expiresAt)
            .deviceInfo(sanitizeDeviceInfo(deviceInfo))
            .ipAddress(ipAddress)
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE));

        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllRefreshTokens(User user) {
        int revokedCount = refreshTokenRepository.revokeAllByUser(user, OffsetDateTime.now());
    }

    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE));

        if (!refreshToken.isValid()) {
            throw new InvalidCredentialsException(REFRESH_TOKEN_EXPIRED_OR_REVOKE_MESSAGE);
        }

        return refreshToken;
    }

    /**
     * Отозвать все токены пользователя, кроме текущего
     */
    @Transactional
    public void revokeOtherRefreshTokens(User user, String currentToken) {
        int revokedCount = refreshTokenRepository.revokeAllByUserExcept(user, currentToken, OffsetDateTime.now());
    }

    public List<RefreshToken> getActiveSessions(User user) {
        return refreshTokenRepository.findAllActiveByUser(user, OffsetDateTime.now());
    }

    /**
     * Проверить существование активного токена
     */
    public boolean isTokenActive(String token) {
        return refreshTokenRepository.existsByTokenAndNotRevokedAndNotExpired(token, OffsetDateTime.now());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info(SCHEDULER_WORK_START_MESSAGE);
        
        OffsetDateTime now = OffsetDateTime.now();

        int deletedExpiredRefresh = refreshTokenRepository.deleteExpiredTokens(now);
        int deletedRevokedRefresh = refreshTokenRepository.deleteRevokedTokens();
        
        log.info(SCHEDULER_WORK_END_MESSAGE, deletedExpiredRefresh, deletedRevokedRefresh);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String sanitizeDeviceInfo(String deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        // Ограничиваем длину и удаляем потенциально опасные символы
        String sanitized = deviceInfo.replaceAll("[<>\"'&]", "").trim();
        return sanitized.length() > 500 ? sanitized.substring(0, 500) : sanitized;
    }

    @Transactional
    public void cleanupOldestTokensForUser(User user, int keepCount) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUser(user, OffsetDateTime.now());
        
        if (activeTokens.size() > keepCount) {
            activeTokens.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .limit(activeTokens.size() - keepCount)
                .forEach(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}