package server.vpn.com.servermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.request.WireGuardDeploymentRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.servermanagement.dto.response.ServerTestingResponse;
import server.vpn.com.servermanagement.dto.response.WireGuardDeploymentResponse;
import server.vpn.com.servermanagement.util.JwtUtil;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerDeploymentProcessService {

    private final JwtUtil jwtUtil;
    private final SshConnectionService sshConnectionService;

    public ServerConnectionResponse testServerConnection(Authentication authentication, ServerConnectionRequest request) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        try {

            return sshConnectionService.testConnection(request);

        } catch (Exception e) {
            return ServerConnectionResponse.builder()
                    .success(false)
                    .checks(ServerConnectionResponse.ConnectionChecks.builder()
                            .dns(false)
                            .tcp(false)
                            .sshHandshake(false)
                            .auth(false)
                            .build())
                    .details(new ServerConnectionResponse.ConnectionDetails())
                    .warnings(List.of("Внутренняя ошибка сервера: " + e.getMessage()))
                    .build();
        }
    }

    public WireGuardDeploymentResponse deployWireGuard(Authentication authentication, WireGuardDeploymentRequest request) {
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
            log.error("Ошибка при развертывании WireGuard: {}", e.getMessage(), e);
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
