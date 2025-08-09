package server.vpn.com.servermanagement.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HeadersUtil {

    /**
     * Извлечение информации об устройстве из запроса
     */
    public static String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
    }

    /**
     * Извлечение IP-адреса из запроса
     */
    public static String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddr();

        // Обработка IPv6 локальных адресов для читаемости
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1 (localhost)";
        }

        // Если это IPv4 localhost
        if ("127.0.0.1".equals(remoteAddr)) {
            return "127.0.0.1 (localhost)";
        }

        return remoteAddr;
    }

    /**
     * Извлечение access токена из заголовка Authorization
     */
    public static String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}