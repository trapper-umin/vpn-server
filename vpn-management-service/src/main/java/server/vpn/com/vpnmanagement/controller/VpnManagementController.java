package server.vpn.com.vpnmanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.vpnmanagement.dto.*;
import server.vpn.com.vpnmanagement.service.VpnManagementService;
import server.vpn.com.vpnmanagement.service.SubscriptionPlanService;
import server.vpn.com.vpnmanagement.util.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
public class VpnManagementController {

    private final VpnManagementService vpnManagementService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final JwtUtil jwtUtil;

    /**
     * Получение статистики продавца
     */
    @GetMapping("/seller/stats")
    public ResponseEntity<SellerStatsResponse> getSellerStats(Authentication authentication) {
        log.info("Запрос статистики продавца: {}", authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SellerStatsResponse stats = vpnManagementService.getSellerStats(sellerId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Получение списка серверов продавца
     */
    @GetMapping("/seller/servers")
    public ResponseEntity<List<SellerServerResponse>> getSellerServers(Authentication authentication) {
        log.info("Запрос серверов продавца: {}", authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<SellerServerResponse> servers = vpnManagementService.getSellerServers(sellerId);
        
        return ResponseEntity.ok(servers);
    }

    /**
     * Получение подписчиков продавца
     */
    @GetMapping("/seller/subscribers")
    public ResponseEntity<List<SellerSubscriberResponse>> getSellerSubscribers(Authentication authentication) {
        log.info("Запрос подписчиков продавца: {}", authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<SellerSubscriberResponse> subscribers = vpnManagementService.getSellerSubscribers(sellerId);
        
        return ResponseEntity.ok(subscribers);
    }

    /**
     * Получение данных о продажах за период
     */
    @GetMapping("/seller/sales")
    public ResponseEntity<List<SalesDataResponse>> getSalesData(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        log.info("Запрос данных о продажах для продавца: {} за {} дней", authentication.getName(), days);
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<SalesDataResponse> salesData = vpnManagementService.getSalesData(sellerId, days);
        
        return ResponseEntity.ok(salesData);
    }

    /**
     * Создание нового сервера
     */
    @PostMapping("/seller/servers")
    public ResponseEntity<SellerServerResponse> createServer(
            @Valid @RequestBody CreateServerRequest request,
            Authentication authentication) {
        log.info("Создание сервера для продавца: {}", authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SellerServerResponse server = vpnManagementService.createServer(sellerId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(server);
    }

    /**
     * Переключение статуса сервера
     */
    @PatchMapping("/seller/servers/{serverId}/toggle")
    public ResponseEntity<SellerServerResponse> toggleServerStatus(
            @PathVariable UUID serverId,
            Authentication authentication) {
        log.info("Переключение статуса сервера {} для продавца: {}", serverId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SellerServerResponse server = vpnManagementService.toggleServerStatus(sellerId, serverId);
        
        return ResponseEntity.ok(server);
    }

    /**
     * Удаление сервера
     */
    @DeleteMapping("/seller/servers/{serverId}")
    public ResponseEntity<Map<String, String>> deleteServer(
            @PathVariable UUID serverId,
            Authentication authentication) {
        log.info("Удаление сервера {} для продавца: {}", serverId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        vpnManagementService.deleteServer(sellerId, serverId);
        
        return ResponseEntity.ok(Map.of("message", "Сервер успешно удален"));
    }

    /**
     * Обновление сервера
     */
    @PutMapping("/seller/servers/{serverId}")
    public ResponseEntity<SellerServerResponse> updateServer(
            @PathVariable UUID serverId,
            @Valid @RequestBody CreateServerRequest request,
            Authentication authentication) {
        log.info("Обновление сервера {} для продавца: {}", serverId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SellerServerResponse server = vpnManagementService.updateServer(sellerId, serverId, request);
        
        return ResponseEntity.ok(server);
    }

    // ==================== УПРАВЛЕНИЕ СЕРВЕРАМИ ====================

    /**
     * Проверка подключения к серверу
     */
    @PostMapping("/seller/servers/test-connection")
    public ResponseEntity<ServerConnectionResponse> testServerConnection(
            @Valid @RequestBody ServerConnectionRequest request,
            Authentication authentication) {
        log.info("Проверка подключения к серверу {} для продавца: {}", request.getIp(), authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        ServerConnectionResponse result = vpnManagementService.testServerConnection(sellerId, request);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Развертывание WireGuard на сервере
     */
    @PostMapping("/seller/servers/deploy-wireguard")
    public ResponseEntity<WireGuardDeploymentResponse> deployWireGuard(
            @RequestParam String serverIp,
            Authentication authentication) {
        log.info("Развертывание WireGuard на сервере {} для продавца: {}", serverIp, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        WireGuardDeploymentResponse result = vpnManagementService.deployWireGuard(sellerId, serverIp);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Тестирование сервера после развертывания
     */
    @PostMapping("/seller/servers/test-readiness")
    public ResponseEntity<ServerTestingResponse> testServerReadiness(
            @RequestParam String serverIp,
            Authentication authentication) {
        log.info("Тестирование готовности сервера {} для продавца: {}", serverIp, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        ServerTestingResponse result = vpnManagementService.testServerReadiness(sellerId, serverIp);
        
        return ResponseEntity.ok(result);
    }

    // ==================== ПЛАНЫ ПОДПИСКИ ====================

    /**
     * Получение планов подписки продавца
     */
    @GetMapping("/seller/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getSellerPlans(Authentication authentication) {
        log.info("Запрос планов подписки продавца: {}", authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<SubscriptionPlanResponse> plans = subscriptionPlanService.getSellerPlans(sellerId);
        
        return ResponseEntity.ok(plans);
    }

    /**
     * Получение планов подписки для конкретного сервера
     */
    @GetMapping("/seller/servers/{serverId}/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getServerPlans(
            @PathVariable UUID serverId,
            Authentication authentication) {
        log.info("Запрос планов для сервера {} продавца: {}", serverId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<SubscriptionPlanResponse> plans = subscriptionPlanService.getServerPlans(sellerId, serverId);
        
        return ResponseEntity.ok(plans);
    }

    /**
     * Создание нового плана подписки
     */
    @PostMapping("/seller/servers/{serverId}/plans")
    public ResponseEntity<SubscriptionPlanResponse> createPlan(
            @PathVariable UUID serverId,
            @Valid @RequestBody CreateSubscriptionPlanRequest request,
            Authentication authentication) {
        log.info("Создание плана для сервера {} продавца: {}", serverId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SubscriptionPlanResponse plan = subscriptionPlanService.createPlan(sellerId, serverId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    /**
     * Обновление плана подписки
     */
    @PutMapping("/seller/plans/{planId}")
    public ResponseEntity<SubscriptionPlanResponse> updatePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateSubscriptionPlanRequest request,
            Authentication authentication) {
        log.info("Обновление плана {} для продавца: {}", planId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SubscriptionPlanResponse plan = subscriptionPlanService.updatePlan(sellerId, planId, request);
        
        return ResponseEntity.ok(plan);
    }

    /**
     * Переключение статуса плана
     */
    @PatchMapping("/seller/plans/{planId}/toggle")
    public ResponseEntity<SubscriptionPlanResponse> togglePlanStatus(
            @PathVariable UUID planId,
            Authentication authentication) {
        log.info("Переключение статуса плана {} для продавца: {}", planId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        SubscriptionPlanResponse plan = subscriptionPlanService.togglePlanStatus(sellerId, planId);
        
        return ResponseEntity.ok(plan);
    }

    /**
     * Удаление плана подписки
     */
    @DeleteMapping("/seller/plans/{planId}")
    public ResponseEntity<Map<String, String>> deletePlan(
            @PathVariable UUID planId,
            Authentication authentication) {
        log.info("Удаление плана {} для продавца: {}", planId, authentication.getName());
        
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        subscriptionPlanService.deletePlan(sellerId, planId);
        
        return ResponseEntity.ok(Map.of("message", "План успешно удален"));
    }

    // ==================== МАРКЕТПЛЕЙС ====================

    /**
     * Получение планов для маркетплейса
     */
    @GetMapping("/marketplace/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getMarketplacePlans() {
        log.info("Запрос планов для маркетплейса");
        
        List<SubscriptionPlanResponse> plans = subscriptionPlanService.getMarketplacePlans();
        
        return ResponseEntity.ok(plans);
    }

    /**
     * Получение популярных планов
     */
    @GetMapping("/marketplace/plans/popular")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPopularPlans() {
        log.info("Запрос популярных планов");
        
        List<SubscriptionPlanResponse> plans = subscriptionPlanService.getPopularPlans();
        
        return ResponseEntity.ok(plans);
    }

    /**
     * Покупка плана подписки
     */
    @PostMapping("/marketplace/plans/{planId}/purchase")
    public ResponseEntity<Map<String, String>> purchasePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody PurchasePlanRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        log.info("Покупка плана {} пользователем: {}", planId, authentication.getName());
        
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        String authHeader = httpRequest.getHeader("Authorization");
        subscriptionPlanService.purchasePlan(userId, planId, request.getBillingCycle(), authHeader);
        
        return ResponseEntity.ok(Map.of("message", "План успешно приобретен"));
    }
}