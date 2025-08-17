package server.vpn.com.servermanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import server.vpn.com.servermanagement.dto.DeploymentEvent;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.request.WireGuardDeploymentRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.servermanagement.dto.response.ServerTestingResponse;
import server.vpn.com.servermanagement.service.ServerDeploymentProcessService;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerDeploymentProcessController {

    private final ServerDeploymentProcessService serverDeploymentProcessService;

    private static final int REPLAY_SIZE = 500;
    private static final String THREAD_NAME = "wg-deploy";

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Deque<DeploymentEvent>> eventBuffers = new ConcurrentHashMap<>();
    private final ExecutorService deployExecutor =
            new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(200),
                    r -> { Thread t = new Thread(r, THREAD_NAME); t.setDaemon(true); return t; });

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
    public ResponseEntity<Map<String,String>> start(@Valid @RequestBody WireGuardDeploymentRequest request,
                                                    Authentication auth) {
        final String jobId = UUID.randomUUID().toString();

        // заранее создаём буфер и эмиттер c таймаутом
        eventBuffers.put(jobId, new ConcurrentLinkedDeque<>());
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        wireUpEmitter(jobId, emitter);
        emitters.put(jobId, emitter);

        // запускаем задачу на отдельном executor’е
        CompletableFuture.runAsync(() ->
                serverDeploymentProcessService.deployWireGuardStream(auth, request, emitter, jobId, () -> {
                    emitters.remove(jobId);
                    // буфер можно оставить N минут для догонки поздними клиентами или чистить сразу
                }), deployExecutor
        );

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "sse", "/deploy-wireguard/stream/" + jobId,
                "status", "/deploy-wireguard/" + jobId + "/status"
        ));
    }

    // фронт может передать Last-Event-ID для догонки пропущенных событий
    @GetMapping(value = "/deploy-wireguard/stream/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String jobId,
                             @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
//        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
//        wireUpEmitter(jobId, emitter);
//
//        // кладём/заменяем текущий эмиттер (старый закроется onCompletion)
//        SseEmitter old = emitters.put(jobId, emitter);
//        if (old != null) { try { old.complete(); } catch (Exception ignored) {} }
//
//        // реплей пропущенных событий
//        Deque<DeploymentEvent> buf = eventBuffers.computeIfAbsent(jobId, k -> new ConcurrentLinkedDeque<>());
//        long last = parseLongSafe(lastEventId, 0L);
//        buf.stream().filter(e -> e.getSeq() != null && e.getSeq() > last).forEach(ev -> sendReplay(emitter, ev));
//
//        // «открытие» + heartbeat
//        safeSend(emitter, SseEmitter.event().name("OPEN").data("{}"));
//        return emitter;
        // 1) Берём существующий эмиттер, созданный в POST
        SseEmitter emitter = emitters.get(jobId);

        // 2) Если по какой-то причине его нет — создаём и кладём (редкий кейс)
        if (emitter == null) {
            emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
            wireUpEmitter(jobId, emitter);
            emitters.put(jobId, emitter);
        }

        // 3) (опционально) отрендерить уже накопленные события, если вы их буферите
        // replayFromBuffer(jobId, lastEventId, emitter); // если буфер будет

        // 4) Отправим OPEN, чтобы фронт понял, что соединение открыто
        safeSend(emitter, SseEmitter.event().name("OPEN").data("{}"));
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

    private void wireUpEmitter(String jobId, SseEmitter emitter) {
        emitter.onTimeout(() -> { try { emitter.complete(); } catch (Exception ignore) {} emitters.remove(jobId);});
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onError(ex -> emitters.remove(jobId));
    }

    private void sendReplay(SseEmitter emitter, DeploymentEvent ev) {
        safeSend(emitter, SseEmitter.event().id(String.valueOf(ev.getSeq()))
                .name(ev.getStage().getCode()).data(ev));
    }

    // общий safeSend — можно вынести в утиль. Исключения — наружу не текут.
    private void safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE send failed (non-fatal): {}", e.toString());
        }
    }


    private long parseLongSafe(String s, long def) {
        try { return s == null ? def : Long.parseLong(s); } catch (Exception e) { return def; }
    }

    // Хук для записи событий в буфер — вызовите из вашего sendEvent(...)
    public void bufferEvent(String jobId, DeploymentEvent ev) {
        Deque<DeploymentEvent> q = eventBuffers.computeIfAbsent(jobId, k -> new ConcurrentLinkedDeque<>());
        q.addLast(ev);
        while (q.size() > REPLAY_SIZE) q.pollFirst();
    }
}
