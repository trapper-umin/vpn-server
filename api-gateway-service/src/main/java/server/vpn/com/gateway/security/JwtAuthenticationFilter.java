package server.vpn.com.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import server.vpn.com.gateway.config.SecurityProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Processing request to path: {}", path);

        // Проверяем, является ли маршрут публичным
        if (isPublicRoute(path)) {
            log.debug("Public route detected: {}", path);
            return chain.filter(exchange);
        }

        // Извлекаем токен из заголовка Authorization
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // Валидируем токен
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
        }

        // Добавляем информацию пользователя в заголовки для передачи микросервисам
        String username = jwtUtil.extractUsername(token);
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", username)
                .build();

        log.debug("JWT token validated successfully for user: {}", username);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicRoute(String path) {
        if (securityProperties.getPublicRoutes() == null) {
            return false;
        }

        return securityProperties.getPublicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"error\": \"%s\", \"status\": %d}", message, httpStatus.value());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1; // Высший приоритет для выполнения фильтра раньше других
    }
} 