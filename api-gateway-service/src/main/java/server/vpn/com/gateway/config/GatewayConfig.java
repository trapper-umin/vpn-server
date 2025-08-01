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

    @Value("${services.user-management.url:http://localhost:8081}")
    private String userManagementServiceUrl;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Настройка маршрутов Gateway с JWT фильтрами");
        
        return builder.routes()
                // Публичные маршруты для аутентификации (без JWT проверки)
                .route("auth-register", r -> r
                        .path("/api/auth/register")
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
                        .path("/api/auth/login")
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
                        .path("/api/auth/refresh")
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
                        .path("/api/auth/profile")
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
                        .path("/api/auth/**")
                        .and()
                        .not(p -> p.path("/api/auth/register"))
                        .and()
                        .not(p -> p.path("/api/auth/login"))
                        .and()
                        .not(p -> p.path("/api/auth/refresh"))
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .stripPrefix(0)
                                .addRequestHeader("X-Gateway-Source", "api-gateway")
                                .addRequestHeader("X-Route-Name", "user-management-protected")
                        )
                        .uri(userManagementServiceUrl)
                )
                // Health check маршрут
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