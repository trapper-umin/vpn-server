package server.vpn.com.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import server.vpn.com.gateway.util.JwtUtil;

import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    // Список публичных путей, которые не требуют аутентификации
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/health",
            "/actuator/prometheus"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            log.debug("JWT фильтр: обработка запроса {}", path);

            // Пропускаем публичные пути
            if (isPublicPath(path)) {
                log.debug("Публичный путь, пропускаем аутентификацию: {}", path);
                return chain.filter(exchange);
            }

            // Извлекаем JWT токен из заголовка
            String token = extractJwtFromRequest(request);
            
            if (!StringUtils.hasText(token)) {
                log.warn("JWT токен отсутствует для защищенного пути: {}", path);
                return onError(exchange, "JWT токен отсутствует", HttpStatus.UNAUTHORIZED);
            }

            try {
                // Валидируем токен
                if (!jwtUtil.isTokenValid(token)) {
                    log.warn("Недействительный JWT токен для пути: {}", path);
                    return onError(exchange, "Недействительный JWT токен", HttpStatus.UNAUTHORIZED);
                }

                // Извлекаем имя пользователя и добавляем в заголовки
                String username = jwtUtil.extractUsername(token);
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Email", username)
                        .header("X-Authenticated", "true")
                        .build();

                log.debug("JWT аутентификация успешна для пользователя: {}", username);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                
            } catch (Exception e) {
                log.error("Ошибка при валидации JWT токена: ", e);
                return onError(exchange, "Ошибка аутентификации", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractJwtFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorBody = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"statusCode\":%d,\"timestamp\":\"%s\"}",
                httpStatus.getReasonPhrase(),
                err,
                httpStatus.value(),
                java.time.LocalDateTime.now()
        );
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBody.getBytes())));
    }

    public static class Config {
        // Конфигурационные параметры фильтра (если понадобятся)
    }
}