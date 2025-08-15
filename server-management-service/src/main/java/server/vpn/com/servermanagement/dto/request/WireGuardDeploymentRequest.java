package server.vpn.com.servermanagement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;

/**
 * DTO для запроса развертывания WireGuard на сервере
 */
@Data
public class WireGuardDeploymentRequest {
    
    /**
     * SSH данные для подключения к серверу
     */
    @NotNull(message = "SSH данные обязательны")
    @Valid
    private ServerConnectionRequest.SshConfig ssh;
    
    /**
     * Конфигурация WireGuard
     */
    @NotNull(message = "Конфигурация WireGuard обязательна")
    @Valid
    private WireGuardConfigRequest wg;
    
    /**
     * DTO для конфигурации WireGuard
     */
    @Data
    public static class WireGuardConfigRequest {
        
        /**
         * UDP порт для прослушивания
         */
        @NotNull(message = "Порт прослушивания обязателен")
        private Integer listenPort;
        
        /**
         * Сетевая конфигурация
         */
        @NotNull(message = "Сетевая конфигурация обязательна")
        @Valid
        private NetworkConfig network;
        
        /**
         * IP адрес сервера в VPN сети
         */
        @NotNull(message = "IP адрес сервера обязателен")
        private String serverAddress;
        
        /**
         * Публичный endpoint сервера
         */
        @NotNull(message = "Endpoint сервера обязателен")
        private String endpoint;
        
        /**
         * DNS серверы для клиентов
         */
        @NotNull(message = "DNS серверы обязательны")
        private String[] dns;
        
        /**
         * Настройки маршрутизации
         */
        @NotNull(message = "Настройки маршрутизации обязательны")
        @Valid
        private RoutingConfig routing;
        
        /**
         * DTO для сетевой конфигурации
         */
        @Data
        public static class NetworkConfig {
            
            /**
             * CIDR подсети VPN
             */
            @NotNull(message = "CIDR подсети обязателен")
            private String cidr;
        }
        
        /**
         * DTO для настроек маршрутизации
         */
        @Data
        public static class RoutingConfig {
            
            /**
             * Разрешить доступ в интернет через VPN
             */
            @NotNull(message = "Настройка доступа в интернет обязательна")
            private Boolean allowInternet;
        }
    }
}

