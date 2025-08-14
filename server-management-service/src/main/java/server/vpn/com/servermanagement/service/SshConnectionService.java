package server.vpn.com.servermanagement.service;

import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;

/**
 * Сервис для тестирования SSH подключения к серверам
 */
public interface SshConnectionService {
    
    /**
     * Тестирует подключение к серверу по SSH
     * 
     * @param request конфигурация SSH подключения
     * @return результат тестирования подключения
     */
    ServerConnectionResponse testConnection(ServerConnectionRequest request);
}
