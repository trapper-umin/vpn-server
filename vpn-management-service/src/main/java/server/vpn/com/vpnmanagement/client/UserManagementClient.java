package server.vpn.com.vpnmanagement.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import server.vpn.com.vpnmanagement.dto.UserProfileDto;

import java.util.UUID;

/**
 * Клиент для взаимодействия с user-management-service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagementClient {

    private final RestTemplate restTemplate;

    @Value("${app.user-management-service.url:http://localhost:8081}")
    private String userManagementServiceUrl;

    /**
     * Получение профиля пользователя по email из JWT токена
     * @param authHeader Authorization header с JWT токеном
     * @return профиль пользователя
     */
    public UserProfileDto getUserProfile(String authHeader) {
        try {
            String url = userManagementServiceUrl + "/api/auth/profile";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<UserProfileDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserProfileDto.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Ошибка при получении профиля пользователя: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить данные пользователя", e);
        }
    }
}