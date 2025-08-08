package server.vpn.com.vpnmanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.vpnmanagement.dto.request.PurchasePlanRequest;
import server.vpn.com.vpnmanagement.dto.response.PurchaseResponse;
import server.vpn.com.vpnmanagement.dto.response.SubscriptionResponse;
import server.vpn.com.vpnmanagement.service.MarketplaceManagementService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace/subscriptions")
@RequiredArgsConstructor
public class MarketplaceManagementController {

    private final MarketplaceManagementService marketplaceManagementService;

    /**
     * Получение всех подписок
     */
    @GetMapping //
    public ResponseEntity<List<SubscriptionResponse>> getMarketplaceSubscriptions() {

        List<SubscriptionResponse> subscriptions = marketplaceManagementService.getMarketplaceSubscriptions();

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Получение популярных подписок
     */
    @GetMapping("/popular")
    public ResponseEntity<List<SubscriptionResponse>> getPopularSubscriptions() {

        List<SubscriptionResponse> subscriptions = marketplaceManagementService.getPopularSubscriptions();

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Покупка плана подписки
     */
    @PostMapping("/{subscriptionId}/purchase") //
    public ResponseEntity<PurchaseResponse> purchaseSubscription(@PathVariable UUID subscriptionId,
                                                                 @Valid @RequestBody PurchasePlanRequest request,
                                                                 Authentication authentication,
                                                                 HttpServletRequest httpRequest) {
        PurchaseResponse response =
                marketplaceManagementService.purchaseSubscription(authentication, subscriptionId, request.getBillingCycle(), httpRequest);

        return ResponseEntity.ok(response);
    }
}
