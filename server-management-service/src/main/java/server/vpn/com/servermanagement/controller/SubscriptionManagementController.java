package server.vpn.com.servermanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.servermanagement.dto.request.CreateSubscriptionPlanRequest;
import server.vpn.com.servermanagement.dto.response.SubscriptionResponse;
import server.vpn.com.servermanagement.service.SubscriptionManagementService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SubscriptionManagementController {

    private final SubscriptionManagementService subscriptionManagementService;

    /**
     * Получение подписок продавца
     */
    @GetMapping("/subscriptions") //
    public ResponseEntity<List<SubscriptionResponse>> getSellerSubscriptions(Authentication authentication) {

        List<SubscriptionResponse> subscriptions = subscriptionManagementService.getSellerSubscriptions(authentication);

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Получение подписок для конкретного сервера
     */
    @GetMapping("/servers/{serverId}/subscriptions") //
    public ResponseEntity<List<SubscriptionResponse>> getServerSubscriptions(@PathVariable UUID serverId,
                                                                             Authentication authentication) {

        List<SubscriptionResponse> subscriptions = subscriptionManagementService.getServerSubscriptions(authentication, serverId);

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Создание новой подписки у сервера
     */
    @PostMapping("/servers/{serverId}/subscriptions") //
    public ResponseEntity<SubscriptionResponse> createSubscription(@PathVariable UUID serverId,
                                                                   @Valid @RequestBody CreateSubscriptionPlanRequest request,
                                                                   Authentication authentication) {

        SubscriptionResponse plan = subscriptionManagementService.createSubscription(authentication, serverId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    /**
     * Обновление подписки
     */
    @PutMapping("/servers/subscriptions/{subscriptionId}") //
    public ResponseEntity<SubscriptionResponse> updateSubscription(@PathVariable UUID subscriptionId,
                                                                   @Valid @RequestBody CreateSubscriptionPlanRequest request,
                                                                   Authentication authentication) {

        SubscriptionResponse subscription = subscriptionManagementService.updateSubscription(authentication, subscriptionId, request);

        return ResponseEntity.ok(subscription);
    }

    /**
     * Переключение статуса подписки
     */
    @PatchMapping("/servers/subscriptions/{subscriptionId}/toggle") //
    public ResponseEntity<SubscriptionResponse> toggleSubscriptionStatus(@PathVariable UUID subscriptionId,
                                                                         Authentication authentication) {

        SubscriptionResponse subscription = subscriptionManagementService.toggleSubscriptionStatus(authentication, subscriptionId);

        return ResponseEntity.ok(subscription);
    }

    /**
     * Удаление подписки
     */
    @DeleteMapping("/servers/subscriptions/{subscriptionId}") //
    public ResponseEntity<Map<String, String>> deleteSubscription(@PathVariable UUID subscriptionId,
                                                                  Authentication authentication) {

        subscriptionManagementService.deleteSubscription(authentication, subscriptionId);

        return ResponseEntity.ok(Map.of("message", "The subscription was successfully deleted"));
    }
}
