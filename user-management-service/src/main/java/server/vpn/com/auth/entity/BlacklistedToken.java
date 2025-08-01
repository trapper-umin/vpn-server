package server.vpn.com.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Сущность для хранения отозванных access токенов (blacklist)
 */
@Entity
@Table(name = "blacklisted_tokens", indexes = {
    @Index(name = "idx_token_jti", columnList = "jti"),
    @Index(name = "idx_blacklisted_tokens_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blacklisted_token_id")
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 255)
    private String jti; // JWT ID для идентификации токена

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    /**
     * Проверяет, истек ли токен
     */
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }
}