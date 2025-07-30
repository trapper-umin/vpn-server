package server.vpn.com.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Slf4j
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        log.info("Настройка CORS конфигурации");
        
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Разрешенные origins (для разработки и продакшена)
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",    // React dev server
                "http://localhost:3001",    // Alternative React port
                "https://*.guardex.com",    // Production domain
                "https://guardex.com"       // Production domain without subdomain
        ));
        
        // Разрешенные методы
        corsConfig.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.PATCH.name()
        ));
        
        // Разрешенные заголовки
        corsConfig.setAllowedHeaders(Arrays.asList(
                HttpHeaders.ORIGIN,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.AUTHORIZATION,
                "X-Requested-With",
                "X-Auth-Token"
        ));
        
        // Заголовки, которые будут доступны клиенту
        corsConfig.setExposedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                "X-Total-Count"
        ));
        
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L); // 1 час кеширования preflight запросов
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }

    /**
     * Дополнительный фильтр для обработки preflight запросов
     */
    @Bean
    public WebFilter corsResponseHeaderFilter() {
        return new WebFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                
                // Обработка preflight OPTIONS запросов
                if (HttpMethod.OPTIONS.equals(request.getMethod())) {
                    log.debug("Обработка preflight запроса: {}", request.getURI());
                    
                    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, 
                            request.getHeaders().getFirst(HttpHeaders.ORIGIN));
                    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, 
                            "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, 
                            "Origin, Content-Type, Accept, Authorization, X-Requested-With, X-Auth-Token");
                    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
                    
                    response.setStatusCode(HttpStatus.OK);
                    return response.setComplete();
                }
                
                return chain.filter(exchange);
            }
        };
    }
}