package server.vpn.com.vpnmanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.vpnmanagement.client.UserManagementClient;
import server.vpn.com.vpnmanagement.dto.mapper.SubscriptionPlanMapper;
import server.vpn.com.vpnmanagement.dto.request.CreateSubscriptionPlanRequest;
import server.vpn.com.vpnmanagement.dto.response.SubscriptionResponse;
import server.vpn.com.vpnmanagement.entity.SubscriptionPlan;
import server.vpn.com.vpnmanagement.entity.VpnServer;
import server.vpn.com.vpnmanagement.repository.SalesRecordRepository;
import server.vpn.com.vpnmanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.vpnmanagement.repository.VpnServerRepository;
import server.vpn.com.vpnmanagement.repository.VpnSubscriptionRepository;
import server.vpn.com.vpnmanagement.util.JwtUtil;

import java.util.List;
import java.util.UUID;

import static server.vpn.com.vpnmanagement.util.enums.ServerManagementConstants.*;

@Service
@RequiredArgsConstructor
public class SubscriptionManagementService {

    private final JwtUtil jwtUtil;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerRepository vpnServerRepository;
    private final SubscriptionPlanMapper subscriptionPlanMapper;

    public List<SubscriptionResponse> getSellerSubscriptions(Authentication authentication) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        List<SubscriptionPlan> subscriptions = subscriptionPlanRepository.findBySellerIdOrderByServerNameAndSortOrder(sellerId);

        return subscriptionPlanMapper.toSubscriptionPlanResponseList(subscriptions);
    }

    public List<SubscriptionResponse> getServerSubscriptions(Authentication authentication, UUID serverId) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SERVER_NOT_FOUND));

        List<SubscriptionPlan> subscriptions = subscriptionPlanRepository.findByServer_IdOrderBySortOrderAscCreatedAtAsc(serverId);
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(subscriptions);
    }

    @Transactional
    public SubscriptionResponse createSubscription(Authentication authentication, UUID serverId, CreateSubscriptionPlanRequest request) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        VpnServer server = vpnServerRepository.findByIdAndSellerId(serverId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SERVER_NOT_FOUND));

        if (subscriptionPlanRepository.existsByServer_IdAndNameIgnoreCase(serverId, request.getName())) {
            throw new IllegalArgumentException(SUBSCRIPTION_NAME_ALREADY_EXISTS);
        }

        SubscriptionPlan subscription = subscriptionPlanMapper.toEntity(request);
        subscription.setServer(server);

        SubscriptionPlan savedSubscription = subscriptionPlanRepository.save(subscription);
        return subscriptionPlanMapper.toSubscriptionPlanResponse(savedSubscription);
    }

    @Transactional
    public SubscriptionResponse updateSubscription(Authentication authentication, UUID subscriptionId, CreateSubscriptionPlanRequest request) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        SubscriptionPlan subscription = subscriptionPlanRepository.findByIdAndSellerId(subscriptionId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getName().equalsIgnoreCase(request.getName()) &&
                subscriptionPlanRepository.existsByServer_IdAndNameIgnoreCase(subscription.getServer().getId(), request.getName())) {
            throw new IllegalArgumentException(SUBSCRIPTION_NAME_ALREADY_EXISTS);
        }

        subscription.setName(request.getName());
        subscription.setType(SubscriptionPlan.PlanType.valueOf(request.getType().toUpperCase()));
        subscription.setMonthlyPrice(request.getMonthlyPrice());
        subscription.setYearlyPrice(request.getYearlyPrice());
        subscription.setMaxConnections(request.getMaxConnections());
        subscription.setBandwidthLimit(request.getBandwidthLimit());
        subscription.setSpeedLimit(request.getSpeedLimit());
        subscription.setIsPopular(request.getIsPopular());
        subscription.setSortOrder(request.getSortOrder());
        subscription.setFeatures(request.getFeatures());
        subscription.setDescription(request.getDescription());

        SubscriptionPlan savedSubscription = subscriptionPlanRepository.save(subscription);
        return subscriptionPlanMapper.toSubscriptionPlanResponse(savedSubscription);
    }

    @Transactional
    public SubscriptionResponse toggleSubscriptionStatus(Authentication authentication, UUID subscriptionId) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        SubscriptionPlan subscription = subscriptionPlanRepository.findByIdAndSellerId(subscriptionId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        subscription.setIsActive(!subscription.getIsActive());

        SubscriptionPlan savedSubscription = subscriptionPlanRepository.save(subscription);
        return subscriptionPlanMapper.toSubscriptionPlanResponse(savedSubscription);
    }

    @Transactional
    public void deleteSubscription(Authentication authentication, UUID subscriptionId) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        SubscriptionPlan subscription = subscriptionPlanRepository.findByIdAndSellerId(subscriptionId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (subscription.getActiveSubscribers() > 0) {
            throw new IllegalArgumentException(THERE_ARE_ACTIVE_SUBSCRIBERS);
        }

        subscriptionPlanRepository.delete(subscription);
    }
}
