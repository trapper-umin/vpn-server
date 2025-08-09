package server.vpn.com.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import server.vpn.com.gateway.filter.JwtAuthenticationFilter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    @Value("${services.user-management-service.url:http://localhost:8081}")
    private String userManagementServiceUrl;

    @Value("${services.server-management-service.url:http://localhost:8082}")
    private String serverManagementServiceUrl;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Публичные маршруты для аутентификации (без JWT проверки)
                .route("auth-register", r -> r
                        .path("/api/v1/auth/register")
                        .and()
                        .method("POST")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "auth-register")
                        )
                        .uri(userManagementServiceUrl)
                )
                .route("auth-login", r -> r
                        .path("/api/v1/auth/login")
                        .and()
                        .method("POST")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "auth-login")
                        )
                        .uri(userManagementServiceUrl)
                )
                .route("auth-refresh", r -> r
                        .path("/api/v1/auth/refresh")
                        .and()
                        .method("POST")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "auth-refresh")
                        )
                        .uri(userManagementServiceUrl)
                )
                // Защищенные маршруты с JWT аутентификацией
                .route("auth-profile", r -> r
                        .path("/api/v1/auth/profile")
                        .and()
                        .method("GET")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "auth-profile")
                        )
                        .uri(userManagementServiceUrl)
                )
                // Общий защищенный маршрут для остальных операций с пользователями
                .route("user-management-protected", r -> r
                        .path("/api/v1/auth/**")
                        .and()
                        .not(p -> p.path("/api/v1/auth/register"))
                        .and()
                        .not(p -> p.path("/api/v1/auth/login"))
                        .and()
                        .not(p -> p.path("/api/v1/auth/refresh"))
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "user-management-protected")
                        )
                        .uri(userManagementServiceUrl)
                )
                // server-management-service пользовательские маршруты (защищенные)
                .route("vpn-management-user", r -> r
                        .path("/api/v1/servers/**", "/api/v1/sellers/**", "/api/v1/users/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "vpn-management-service")
                        )
                        .uri(serverManagementServiceUrl)
                )
                // VPN Management Service маркетплейс (публичные)
                .route("vpn-management-marketplace", r -> r
                        .path("/api/v1/marketplace/subscriptions/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "vpn-management-marketplace")
                        )
                        .uri(serverManagementServiceUrl)
                )
                // Health check маршруты для обоих сервисов
                .route("health-check-user", r -> r
                        .path("/actuator/health/user")
                        .filters(f -> f
                                .stripPrefix(2) // убираем /actuator/health/user -> /actuator/health
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                        )
                        .uri(userManagementServiceUrl)
                )
                .route("health-check-vpn", r -> r
                        .path("/actuator/health/vpn")
                        .filters(f -> f
                                .stripPrefix(2) // убираем /actuator/health/vpn -> /actuator/health
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                        )
                        .uri(serverManagementServiceUrl)
                )
                // Общий health check (проверяет user-management по умолчанию)
                .route("health-check", r -> r
                        .path("/actuator/health")
                        .filters(f -> f
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                        )
                        .uri(userManagementServiceUrl)
                )
                .build();
    }
}