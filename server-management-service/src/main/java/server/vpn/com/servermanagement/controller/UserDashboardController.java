package server.vpn.com.servermanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.servermanagement.dto.response.UserSubscriptionResponse;
import server.vpn.com.servermanagement.dto.response.UserSubscriptionStatusResponse;
import server.vpn.com.servermanagement.service.UserDashboardService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/subscriptions")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    /**
     * Получение подписок пользователя
     */
    @GetMapping //
    public ResponseEntity<List<UserSubscriptionResponse>> getUserSubscriptions(Authentication authentication) {

        List<UserSubscriptionResponse> subscriptions = userDashboardService.getUserSubscriptions(authentication);

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Получение статуса подписок пользователя
     */
    @GetMapping("/status") //
    public ResponseEntity<UserSubscriptionStatusResponse> getUserSubscriptionStatus(Authentication authentication) {

        UserSubscriptionStatusResponse status = userDashboardService.getUserSubscriptionStatus(authentication);

        return ResponseEntity.ok(status);
    }

    /**
     * Продление подписки пользователя
     */
    @PostMapping("/{subscriptionId}/extend") //
    public ResponseEntity<Map<String, String>> extendUserSubscription(@PathVariable UUID subscriptionId,
                                                                      @RequestParam(defaultValue = "1") int months,
                                                                      Authentication authentication) {

        userDashboardService.extendUserSubscription(authentication, subscriptionId, months);

        return ResponseEntity.ok(Map.of("message", "Subscription successfully renewed"));
    }

    /**
     * Получение VPN конфигурации для подписки
     */
    @GetMapping("/{subscriptionId}/config") //
    public ResponseEntity<String> getVpnConfig(@PathVariable UUID subscriptionId, Authentication authentication) {

        String config = userDashboardService.generateVpnConfig(authentication, subscriptionId);

        return ResponseEntity.ok()
                .header("Content-Type", "text/plain")
                .header("Content-Disposition", "attachment; filename=vpn-config-" + subscriptionId + ".conf")
                .body(config);
    }

    /**
     * Перегенерация VPN ключей для подписки
     */
    @PostMapping("/{subscriptionId}/regenerate") //
    public ResponseEntity<Map<String, String>> regenerateVpnConfig(@PathVariable UUID subscriptionId,
                                                                   Authentication authentication) {

        userDashboardService.regenerateVpnConfig(authentication, subscriptionId);

        return ResponseEntity.ok(Map.of("message", "VPN keys have been successfully regenerated"));
    }

    /**
     * Отмена подписки пользователем
     */
    @DeleteMapping("/{subscriptionId}/cancel") //
    public ResponseEntity<Map<String, String>> cancelSubscription(@PathVariable UUID subscriptionId,
                                                                  Authentication authentication) {

        userDashboardService.cancelSubscription(authentication, subscriptionId);

        return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully"));
    }
}
