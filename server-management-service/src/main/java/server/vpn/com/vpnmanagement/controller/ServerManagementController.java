package server.vpn.com.vpnmanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.vpnmanagement.dto.request.CreateServerRequest;
import server.vpn.com.vpnmanagement.dto.response.SellerServerResponse;
import server.vpn.com.vpnmanagement.service.ServerManagementService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerManagementController {

    private final ServerManagementService serverManagementService;

    /**
     * Создание нового сервера
     */
    @PostMapping //
    public ResponseEntity<SellerServerResponse> createServer(@Valid @RequestBody CreateServerRequest request,
                                                             Authentication authentication) {
        SellerServerResponse server = serverManagementService.createServer(authentication, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(server);
    }

    /**
     * Переключение статуса сервера
     */
    @PatchMapping("/{serverId}/toggle") //
    public ResponseEntity<SellerServerResponse> toggleServerStatus(@PathVariable UUID serverId,
                                                                   Authentication authentication) {
        SellerServerResponse server = serverManagementService.toggleServerStatus(authentication, serverId);

        return ResponseEntity.ok(server);
    }

    /**
     * Удаление сервера
     */
    @DeleteMapping("/{serverId}") //
    public ResponseEntity<Map<String, String>> deleteServer(@PathVariable UUID serverId, Authentication authentication) {

        serverManagementService.deleteServer(authentication, serverId);

        return ResponseEntity.ok(Map.of("message", "The server was successfully deleted"));
    }

    /**
     * Обновление сервера
     */
    @PutMapping("/{serverId}") //
    public ResponseEntity<SellerServerResponse> updateServer(@PathVariable UUID serverId,
                                                             @Valid @RequestBody CreateServerRequest request,
                                                             Authentication authentication) {
        SellerServerResponse server = serverManagementService.updateServer(authentication, serverId, request);

        return ResponseEntity.ok(server);
    }
}
