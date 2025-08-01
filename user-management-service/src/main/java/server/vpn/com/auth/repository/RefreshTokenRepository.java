package server.vpn.com.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.auth.entity.RefreshToken;
import server.vpn.com.auth.entity.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository для работы с refresh токенами
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Найти refresh токен по строковому значению
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Найти все активные токены пользователя
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findAllActiveByUser(@Param("user") User user, @Param("now") OffsetDateTime now);

    /**
     * Найти все токены пользователя (включая истекшие и отозванные)
     */
    List<RefreshToken> findAllByUser(User user);

    /**
     * Отозвать все токены пользователя
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :now WHERE rt.user = :user AND rt.isRevoked = false")
    int revokeAllByUser(@Param("user") User user, @Param("now") OffsetDateTime now);

    /**
     * Отозвать все токены пользователя, кроме указанного
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :now WHERE rt.user = :user AND rt.token != :excludeToken AND rt.isRevoked = false")
    int revokeAllByUserExcept(@Param("user") User user, @Param("excludeToken") String excludeToken, @Param("now") OffsetDateTime now);

    /**
     * Удалить все истекшие токены
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") OffsetDateTime now);

    /**
     * Удалить все отозванные токены
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true")
    int deleteRevokedTokens();

    /**
     * Найти все истекшие токены
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") OffsetDateTime now);

    /**
     * Подсчитать активные сессии пользователя
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countActiveSessionsByUser(@Param("user") User user, @Param("now") OffsetDateTime now);

    /**
     * Проверить существование активного токена
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.token = :token AND rt.isRevoked = false AND rt.expiresAt > :now")
    boolean existsByTokenAndNotRevokedAndNotExpired(@Param("token") String token, @Param("now") OffsetDateTime now);
}