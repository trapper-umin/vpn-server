package server.vpn.com.servermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import server.vpn.com.servermanagement.dto.DeploymentEvent;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.request.WireGuardDeploymentRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.servermanagement.dto.response.ServerTestingResponse;
import server.vpn.com.servermanagement.dto.response.WireGuardDeploymentResponse;
import server.vpn.com.servermanagement.util.JwtUtil;
import server.vpn.com.servermanagement.util.enums.WireGuardDeploymentStage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerDeploymentProcessService {

    private final JwtUtil jwtUtil;
    private final SshConnectionService sshConnectionService;
    private final WireGuardDeploymentExecutor wireGuardDeploymentExecutor;

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

    /**
     * Запускает процесс развертывания WireGuard и стримит события в переданный SseEmitter.
     * Метод вызывается из POST /deploy-wireguard, а сам стрим читается по GET /deploy-wireguard/stream/{jobId}.
     */
    public void deployWireGuardStream(Authentication authentication,
                                      WireGuardDeploymentRequest request,
                                      SseEmitter emitter,
                                      String jobId,
                                      Runnable onFinish) {
        final UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        // Хартбит, чтобы соединение не засыпало у прокси/браузера
        final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "wg-deploy-heartbeat-" + jobId);
            t.setDaemon(true);
            return t;
        });
        heartbeat.scheduleAtFixedRate(() -> safeSend(emitter,
                SseEmitter.event().name("KEEPALIVE").data("{}")), 15, 15, TimeUnit.SECONDS);

        // Основная задача деплоя
        CompletableFuture
                .runAsync(() -> {
                    long seq = 0;
                    try {
                        // Стартовое событие (по желанию)
                        safeSend(emitter, SseEmitter.event()
                                .name(WireGuardDeploymentStage.QUEUED.name())
                                .data(DeploymentEvent.builder()
                                        .ts(LocalDateTime.now(ZoneOffset.UTC))
                                        .jobId(jobId)
                                        .seq(++seq)
                                        .stage(WireGuardDeploymentStage.QUEUED)
                                        .level(DeploymentEvent.EventLevel.INFO)
                                        .message("Задача поставлена в очередь")
                                        .progress(0)
                                        .details(Map.of("sellerId", sellerId))
                                        .build()));

                        // Запуск реального деплоя; внутри executor'а шли события в emitter
                        // РЕКОМЕНДУЮ: пробрасывать jobId внутрь, чтобы он попадал в каждое событие
                        wireGuardDeploymentExecutor.deployWireGuard(request, emitter, jobId);

                        // Финальное событие DONE
                        safeSend(emitter, SseEmitter.event()
                                .name(WireGuardDeploymentStage.DONE.name())
                                .data(DeploymentEvent.builder()
                                        .ts(LocalDateTime.now(ZoneOffset.UTC))
                                        .jobId(jobId)
                                        .seq(++seq)
                                        .stage(WireGuardDeploymentStage.DONE)
                                        .level(DeploymentEvent.EventLevel.INFO)
                                        .message("Развертывание завершено успешно")
                                        .progress(100)
                                        .build()));
                    } catch (Exception ex) {
                        log.error("Ошибка при развертывании WireGuard [jobId={}]: {}", jobId, ex.getMessage(), ex);
                        safeSend(emitter, SseEmitter.event()
                                .name(WireGuardDeploymentStage.FAILED.name())
                                .data(DeploymentEvent.builder()
                                        .ts(LocalDateTime.now(ZoneOffset.UTC))
                                        .jobId(jobId)
                                        .seq(System.currentTimeMillis()) // на случай отсутствия локального счётчика
                                        .stage(WireGuardDeploymentStage.FAILED)
                                        .level(DeploymentEvent.EventLevel.ERROR)
                                        .message(ex.getMessage())
                                        .progress(0)
                                        .build()));
                        try { emitter.completeWithError(ex); } catch (Exception ignore) {}
                        return; // finally всё равно выполнится
                    } finally {
                        try { emitter.complete(); } catch (Exception ignore) {}
                        try { heartbeat.shutdownNow(); } catch (Exception ignore) {}
                        try { onFinish.run(); } catch (Exception ignore) {}
                    }
                })
                // Перестраховка: если CF «упал», корректно завершаем
                .exceptionally(ex -> {
                    log.error("CF exception [jobId={}]: {}", jobId, ex.getMessage(), ex);
                    safeSend(emitter, SseEmitter.event()
                            .name(WireGuardDeploymentStage.FAILED.name())
                            .data(DeploymentEvent.builder()
                                    .ts(LocalDateTime.now(ZoneOffset.UTC))
                                    .jobId(jobId)
                                    .seq(System.currentTimeMillis())
                                    .stage(WireGuardDeploymentStage.FAILED)
                                    .level(DeploymentEvent.EventLevel.ERROR)
                                    .message("Неожиданная ошибка выполнения: " + ex.getMessage())
                                    .progress(0)
                                    .build()));
                    try { emitter.completeWithError(ex); } catch (Exception ignore) {}
                    try { heartbeat.shutdownNow(); } catch (Exception ignore) {}
                    try { onFinish.run(); } catch (Exception ignore) {}
                    return null;
                });
    }

    /** Безопасная отправка события в SSE — не бросает наружу и не рвёт поток. */
    private void safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException ioe) {
            // Клиент мог отключиться — фиксируем и даём верхнему уровню завершить поток
            log.warn("SSE send failed: {}", ioe.getMessage());
            try { emitter.completeWithError(ioe); } catch (Exception ignore) {}
            throw new RuntimeException(ioe);
        } catch (IllegalStateException ise) {
            // Эмиттер уже завершён
            log.warn("SSE emitter already completed: {}", ise.getMessage());
            throw new RuntimeException(ise);
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
