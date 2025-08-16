package server.vpn.com.servermanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.request.WireGuardDeploymentRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.servermanagement.dto.response.ServerTestingResponse;
import server.vpn.com.servermanagement.dto.response.WireGuardDeploymentResponse;
import server.vpn.com.servermanagement.service.ServerDeploymentProcessService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerDeploymentProcessController {

    private final ServerDeploymentProcessService serverDeploymentProcessService;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Проверка подключения к серверу
     */
    @PostMapping("/test-connection") //
    public ResponseEntity<ServerConnectionResponse> testServerConnection(@Valid @RequestBody ServerConnectionRequest request,
                                                                         Authentication authentication) {

        ServerConnectionResponse result = serverDeploymentProcessService.testServerConnection(authentication, request);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/deploy-wireguard")
    public ResponseEntity<Map<String, String>> start(@Valid @RequestBody WireGuardDeploymentRequest request,
                                                     Authentication auth) {
        String jobId = UUID.randomUUID().toString();
        // создаём emitter и кладём его в карту, но сам стрим отдаём через GET
        SseEmitter emitter = new SseEmitter(0L); // без таймаута (или поставь 300_000L + heartbeat)
        emitters.put(jobId, emitter);

        // запускаем фоновые шаги; внутри сервиса эмитим события в emitter
        serverDeploymentProcessService.deployWireGuardStream(auth, request, emitter, jobId, () -> {
            // по завершении убираем emitter
            emitters.remove(jobId);
        });

        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @GetMapping(value = "/deploy-wireguard/stream/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String jobId) {

        SseEmitter emitter = emitters.computeIfAbsent(jobId, k -> new SseEmitter(0L));
        // если фронт подключился раньше, чем мы положили emitter — создаём и кладём
        // небольшой ping, чтобы браузер «увидел» открытие
        try {
            emitter.send(SseEmitter.event().name("OPEN").data("{}"));
        } catch (IOException ignored) {}
        return emitter;
    }

    /**
     * Тестирование сервера после развертывания
     */
    @PostMapping("/test-readiness") //
    public ResponseEntity<ServerTestingResponse> testServerReadiness(@RequestParam String serverIp,
                                                                     Authentication authentication) {

        ServerTestingResponse result = serverDeploymentProcessService.testServerReadiness(authentication, serverIp);

        return ResponseEntity.ok(result);
    }
}
