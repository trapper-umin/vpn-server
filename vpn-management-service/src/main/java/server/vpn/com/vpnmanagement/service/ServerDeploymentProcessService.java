package server.vpn.com.vpnmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import server.vpn.com.vpnmanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.vpnmanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.vpnmanagement.dto.response.ServerTestingResponse;
import server.vpn.com.vpnmanagement.dto.response.WireGuardDeploymentResponse;
import server.vpn.com.vpnmanagement.util.JwtUtil;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServerDeploymentProcessService {

    private final JwtUtil jwtUtil;

    public ServerConnectionResponse testServerConnection(Authentication authentication, ServerConnectionRequest request) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        // Здесь должна быть реальная логика подключения к серверу
        // Пока возвращаем мок данные
        try {
            // Симуляция проверки подключения
            Thread.sleep(2000); // Имитация задержки

            ServerConnectionResponse.ServerInfo serverInfo = ServerConnectionResponse.ServerInfo.builder()
                    .ip(request.getIp())
                    .os("Ubuntu 22.04 LTS")
                    .region("Unknown")
                    .provider("Generic VPS")
                    .build();

            return ServerConnectionResponse.builder()
                    .success(true)
                    .serverInfo(serverInfo)
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ServerConnectionResponse.builder()
                    .success(false)
                    .error("Прервано тестирование подключения")
                    .build();
        } catch (Exception e) {
            return ServerConnectionResponse.builder()
                    .success(false)
                    .error("Не удалось подключиться к серверу: " + e.getMessage())
                    .build();
        }
    }

    public WireGuardDeploymentResponse deployWireGuard(Authentication authentication, String serverIp) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        // Здесь должна быть реальная логика развертывания WireGuard
        // Пока возвращаем мок данные
        try {
            List<WireGuardDeploymentResponse.DeploymentStep> steps = List.of(
                    WireGuardDeploymentResponse.DeploymentStep.builder()
                            .id("system_update")
                            .name("Обновление системы")
                            .status("completed")
                            .details("Система успешно обновлена")
                            .logs(List.of("apt update", "apt upgrade -y"))
                            .build(),
                    WireGuardDeploymentResponse.DeploymentStep.builder()
                            .id("wireguard_install")
                            .name("Установка WireGuard")
                            .status("completed")
                            .details("WireGuard успешно установлен")
                            .logs(List.of("apt install wireguard -y"))
                            .build(),
                    WireGuardDeploymentResponse.DeploymentStep.builder()
                            .id("config_setup")
                            .name("Настройка конфигурации")
                            .status("completed")
                            .details("Конфигурация настроена")
                            .logs(List.of("Generated server keys", "Created wg0.conf"))
                            .build()
            );

            return WireGuardDeploymentResponse.builder()
                    .success(true)
                    .steps(steps)
                    .build();

        } catch (Exception e) {
            return WireGuardDeploymentResponse.builder()
                    .success(false)
                    .error("Ошибка развертывания: " + e.getMessage())
                    .build();
        }
    }


    public ServerTestingResponse testServerReadiness(Authentication authentication, String serverIp) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        // Здесь должна быть реальная логика тестирования сервера
        // Пока возвращаем мок данные
        try {
            List<ServerTestingResponse.TestResult> tests = List.of(
                    ServerTestingResponse.TestResult.builder()
                            .id("connectivity")
                            .name("Проверка подключения")
                            .status("passed")
                            .details("Сервер доступен")
                            .logs(List.of("ping successful", "ssh connection ok"))
                            .build(),
                    ServerTestingResponse.TestResult.builder()
                            .id("wireguard_status")
                            .name("Статус WireGuard")
                            .status("passed")
                            .details("WireGuard запущен и работает")
                            .logs(List.of("systemctl status wg-quick@wg0", "interface wg0 is up"))
                            .build(),
                    ServerTestingResponse.TestResult.builder()
                            .id("port_check")
                            .name("Проверка портов")
                            .status("passed")
                            .details("Порт 51820 доступен")
                            .logs(List.of("netstat -an | grep 51820", "port is listening"))
                            .build()
            );

            return ServerTestingResponse.builder()
                    .success(true)
                    .tests(tests)
                    .overallStatus("passed")
                    .build();

        } catch (Exception e) {
            return ServerTestingResponse.builder()
                    .success(false)
                    .overallStatus("failed")
                    .tests(List.of())
                    .build();
        }
    }
}
