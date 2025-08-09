package server.vpn.com.servermanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.servermanagement.dto.mapper.VpnSubscriptionMapper;
import server.vpn.com.servermanagement.dto.response.UserSubscriptionResponse;
import server.vpn.com.servermanagement.dto.response.UserSubscriptionStatusResponse;
import server.vpn.com.servermanagement.entity.SubscriptionPlan;
import server.vpn.com.servermanagement.entity.VpnServer;
import server.vpn.com.servermanagement.entity.VpnSubscription;
import server.vpn.com.servermanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.servermanagement.repository.VpnServerRepository;
import server.vpn.com.servermanagement.repository.VpnSubscriptionRepository;
import server.vpn.com.servermanagement.util.JwtUtil;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static server.vpn.com.servermanagement.util.enums.ServerManagementConstants.*;

@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private final JwtUtil jwtUtil;
    private final VpnSubscriptionRepository vpnSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerRepository vpnServerRepository;
    private final VpnSubscriptionMapper vpnSubscriptionMapper;
    private final ConnectionManagementService connectionManagementService;

    public List<UserSubscriptionResponse> getUserSubscriptions(Authentication authentication) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        List<VpnSubscription> subscriptions = vpnSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return vpnSubscriptionMapper.toUserSubscriptionResponseList(subscriptions);
    }

    public UserSubscriptionStatusResponse getUserSubscriptionStatus(Authentication authentication) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        List<VpnSubscription> allSubscriptions = vpnSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<VpnSubscription> activeSubscriptions = vpnSubscriptionRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);

        List<UserSubscriptionResponse> subscriptionResponses = vpnSubscriptionMapper.toUserSubscriptionResponseList(allSubscriptions);

        boolean hasActiveSubscriptions = !activeSubscriptions.isEmpty();
        int minDaysLeft = 0;
        String earliestExpiresAt = null;

        if (hasActiveSubscriptions) {
            VpnSubscription soonestToExpire = activeSubscriptions.stream()
                    .min((s1, s2) -> s1.getEndDate().compareTo(s2.getEndDate()))
                    .orElse(null);

            minDaysLeft = vpnSubscriptionMapper.calculateDaysLeft(soonestToExpire);
            earliestExpiresAt = soonestToExpire.getEndDate().toLocalDate().toString();
        }

        Set<String> uniqueCountries = activeSubscriptions.stream()
                .map(s -> s.getPlan().getServer().getCountry())
                .collect(Collectors.toSet());

        return UserSubscriptionStatusResponse.builder()
                .isActive(hasActiveSubscriptions)
                .daysLeft(minDaysLeft)
                .expiresAt(earliestExpiresAt)
                .subscriptions(subscriptionResponses)
                .totalSubscriptions(allSubscriptions.size())
                .activeSubscriptions(activeSubscriptions.size())
                .uniqueCountries(uniqueCountries.size())
                .build();
    }

    @Transactional
    public void extendUserSubscription(Authentication authentication, UUID subscriptionId, int months) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnSubscription subscription = vpnSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_DOES_NOT_BELONG_TO_THE_USER);
        }

        OffsetDateTime newEndDate = subscription.getEndDate().plusMonths(months);
        subscription.setEndDate(newEndDate);
        subscription.setIsActive(true);

        BigDecimal additionalPayment = subscription.getPlan().getMonthlyPrice().multiply(BigDecimal.valueOf(months));
        subscription.setTotalPaid(subscription.getTotalPaid().add(additionalPayment));

        vpnSubscriptionRepository.save(subscription);
    }


    public String generateVpnConfig(Authentication authentication, UUID subscriptionId) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnSubscription subscription = vpnSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_DOES_NOT_BELONG_TO_THE_USER);
        }

        if (!subscription.getIsActive()) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_IS_INACTIVE);
        }

        String serverName = subscription.getPlan().getServer().getName().toLowerCase().replaceAll("[^a-z0-9]", "-");

        return String.format("""
                # VPN config for %s
                # Server: %s
                # Country: %s
                # Plan: %s
                
                [Interface]
                PrivateKey = mock_private_key_%s_%d
                Address = 10.0.0.2/24
                
                [Peer]
                PublicKey = mock_server_public_key_%s
                Endpoint = %s.vpn.example.com:51820
                AllowedIPs = 0.0.0.0/0
                
                # Generated: %s
                # Subscription: %s
                """,
                subscription.getPlan().getName(),
                subscription.getPlan().getServer().getName(),
                subscription.getPlan().getServer().getCountry(),
                subscription.getPlan().getType().name(),
                subscriptionId,
                System.currentTimeMillis(),
                subscriptionId,
                serverName,
                OffsetDateTime.now().toString(),
                subscription.getPlan().getName()
        );
    }


    @Transactional
    public void regenerateVpnConfig(Authentication authentication, UUID subscriptionId) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnSubscription subscription = vpnSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_DOES_NOT_BELONG_TO_THE_USER);
        }

        if (!subscription.getIsActive()) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_IS_INACTIVE);
        }

        subscription.setUpdatedAt(OffsetDateTime.now());
        vpnSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void cancelSubscription(Authentication authentication, UUID subscriptionId) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnSubscription subscription = vpnSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_DOES_NOT_BELONG_TO_THE_USER);
        }

        if (!subscription.getIsActive()) {
            throw new IllegalArgumentException(THE_SUBSCRIPTION_IS_INACTIVE);
        }

        subscription.setIsActive(false);
        vpnSubscriptionRepository.save(subscription);

        SubscriptionPlan plan = subscription.getPlan();
        plan.setActiveSubscribers(Math.max(0, plan.getActiveSubscribers() - 1));
        subscriptionPlanRepository.save(plan);

        VpnServer server = plan.getServer();
        server.setActiveSubscribers(Math.max(0, server.getActiveSubscribers() - 1));

        connectionManagementService.decrementServerConnections(server);
        
        vpnServerRepository.save(server);
    }
}
