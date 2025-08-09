package server.vpn.com.servermanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.servermanagement.dto.response.ServerTestingResponse;
import server.vpn.com.servermanagement.dto.response.WireGuardDeploymentResponse;
import server.vpn.com.servermanagement.service.ServerDeploymentProcessService;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerDeploymentProcessController {

    private final ServerDeploymentProcessService serverDeploymentProcessService;

    /**
     * Проверка подключения к серверу
     */
    @PostMapping("/test-connection") //
    public ResponseEntity<ServerConnectionResponse> testServerConnection(@Valid @RequestBody ServerConnectionRequest request,
                                                                         Authentication authentication) {

        ServerConnectionResponse result = serverDeploymentProcessService.testServerConnection(authentication, request);

        return ResponseEntity.ok(result);
    }

    /**
     * Развертывание WireGuard на сервере
     */
    @PostMapping("/deploy-wireguard") //
    public ResponseEntity<WireGuardDeploymentResponse> deployWireGuard(@RequestParam String serverIp,
                                                                       Authentication authentication) {

        WireGuardDeploymentResponse result = serverDeploymentProcessService.deployWireGuard(authentication, serverIp);

        return ResponseEntity.ok(result);
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
