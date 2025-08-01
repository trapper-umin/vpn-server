package server.vpn.com.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO для отображения информации о сессии пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private Long id;
    private String deviceInfo;
    private String ipAddress;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private boolean isActive;
    private boolean isCurrent;
}