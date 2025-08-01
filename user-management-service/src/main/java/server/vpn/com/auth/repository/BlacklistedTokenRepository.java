package server.vpn.com.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.vpn.com.auth.entity.BlacklistedToken;

import java.time.OffsetDateTime;

/**
 * Repository для работы с blacklist токенов
 */
@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    /**
     * Проверить, находится ли токен в blacklist
     */
    boolean existsByJti(String jti);

    /**
     * Найти токен по JTI
     */
    BlacklistedToken findByJti(String jti);

    /**
     * Удалить все истекшие токены из blacklist
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") OffsetDateTime now);

    /**
     * Получить количество токенов пользователя в blacklist
     */
    @Query("SELECT COUNT(bt) FROM BlacklistedToken bt WHERE bt.userEmail = :email")
    long countByUserEmail(@Param("email") String email);

    /**
     * Удалить все токены пользователя из blacklist (для очистки при logoutAll)
     */
    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.userEmail = :email")
    int deleteByUserEmail(@Param("email") String email);
}