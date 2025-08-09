package server.vpn.com.vpnmanagement.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.vpnmanagement.client.UserManagementClient;
import server.vpn.com.vpnmanagement.dto.UserProfileDto;
import server.vpn.com.vpnmanagement.dto.mapper.SubscriptionPlanMapper;
import server.vpn.com.vpnmanagement.dto.response.PurchaseResponse;
import server.vpn.com.vpnmanagement.dto.response.SubscriptionResponse;
import server.vpn.com.vpnmanagement.entity.SalesRecord;
import server.vpn.com.vpnmanagement.entity.SubscriptionPlan;
import server.vpn.com.vpnmanagement.entity.VpnServer;
import server.vpn.com.vpnmanagement.entity.VpnSubscription;
import server.vpn.com.vpnmanagement.repository.SalesRecordRepository;
import server.vpn.com.vpnmanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.vpnmanagement.repository.VpnServerRepository;
import server.vpn.com.vpnmanagement.repository.VpnSubscriptionRepository;
import server.vpn.com.vpnmanagement.util.JwtUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static server.vpn.com.vpnmanagement.util.CountryCodeFlag.getCountryFlag;
import static server.vpn.com.vpnmanagement.util.enums.ServerManagementConstants.*;

@Service
@RequiredArgsConstructor
public class MarketplaceManagementService {

    private final JwtUtil jwtUtil;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerRepository vpnServerRepository;
    private final VpnSubscriptionRepository vpnSubscriptionRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final UserManagementClient userManagementClient;
    private final SubscriptionPlanMapper subscriptionPlanMapper;

    public List<SubscriptionResponse> getMarketplaceSubscriptions() {
        List<SubscriptionPlan> subscriptions = subscriptionPlanRepository.findActiveMarketplacePlans();
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(subscriptions);
    }

    public List<SubscriptionResponse> getPopularSubscriptions() {
        List<SubscriptionPlan> subscriptions = subscriptionPlanRepository.findByIsPopularTrueAndIsActiveTrueOrderBySortOrderAscCreatedAtAsc();
        return subscriptionPlanMapper.toSubscriptionPlanResponseList(subscriptions);
    }

    @Transactional
    public PurchaseResponse purchaseSubscription(Authentication authentication, UUID subscriptionId, String billingCycle,
                                                 HttpServletRequest httpRequest) {
        UUID userId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);

        SubscriptionPlan subscription = subscriptionPlanRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(SUBSCRIPTION_NOT_FOUND));

        if (userId.equals(subscription.getServer().getSellerId())) {
            throw new IllegalArgumentException(CANNOT_BUY_OWN_SUBSCRIPTION);
        }

        if (!subscription.getIsActive()) {
            throw new IllegalArgumentException(IS_NOT_AVAILABLE_FOR_PURCHASE);
        }

        if (subscription.getActiveSubscribers() >= subscription.getMaxConnections()) {
            throw new IllegalArgumentException(MAXIMUM_NUMBER_OF_SUBSCRIBERS_FOR_THIS_SUBSCRIPTION);
        }

        List<VpnSubscription> existingSubscriptions = vpnSubscriptionRepository
                .findByUserIdAndPlan_IdAndIsActiveTrue(userId, subscriptionId);
        if (!existingSubscriptions.isEmpty()) {
            throw new IllegalArgumentException(ALREADY_HAVE_THIS_SUBSCRIPTION);
        }

        VpnSubscription.BillingCycle cycle = VpnSubscription.BillingCycle.valueOf(billingCycle.toUpperCase());
        BigDecimal price = cycle == VpnSubscription.BillingCycle.MONTHLY ?
                subscription.getMonthlyPrice() : subscription.getYearlyPrice();

        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = cycle == VpnSubscription.BillingCycle.MONTHLY ?
                startDate.plusMonths(1) : startDate.plusYears(1);

        UserProfileDto userProfile = userManagementClient.getUserProfile(authHeader);
        if (userProfile == null || !userProfile.getIsActive()) {
            throw new IllegalArgumentException(USER_NOT_FOUND);
        }

        String userEmail = userProfile.getEmail();

        // Создаем подписку
        VpnSubscription vpnSubscription = VpnSubscription.builder()
                .userId(userId)
                .userEmail(userEmail)
                .plan(subscription)
                .billingCycle(cycle)
                .startDate(startDate)
                .endDate(endDate)
                .totalPaid(price)
                .isActive(true)
                .build();

        vpnSubscriptionRepository.save(vpnSubscription);

        subscription.setTotalSubscribers(subscription.getTotalSubscribers() + 1);
        subscription.setActiveSubscribers(subscription.getActiveSubscribers() + 1);
        subscription.setTotalRevenue(subscription.getTotalRevenue().add(price));
        if (cycle == VpnSubscription.BillingCycle.MONTHLY) {
            subscription.setMonthlyRevenue(subscription.getMonthlyRevenue().add(price));
        }
        subscriptionPlanRepository.save(subscription);

        VpnServer server = subscription.getServer();
        server.setTotalSubscribers(server.getTotalSubscribers() + 1);
        server.setActiveSubscribers(server.getActiveSubscribers() + 1);
        server.setTotalRevenue(server.getTotalRevenue().add(price));
        if (cycle == VpnSubscription.BillingCycle.MONTHLY) {
            server.setMonthlyRevenue(server.getMonthlyRevenue().add(price));
        }
        vpnServerRepository.save(server);

        LocalDate today = LocalDate.now();
        UUID sellerId = server.getSellerId();

        SalesRecord salesRecord = salesRecordRepository.findBySellerIdAndSaleDate(sellerId, today)
                .orElse(SalesRecord.builder()
                        .sellerId(sellerId)
                        .saleDate(today)
                        .revenue(BigDecimal.ZERO)
                        .newSubscribers(0)
                        .refunds(0)
                        .build());

        salesRecord.setRevenue(salesRecord.getRevenue().add(price));
        salesRecord.setNewSubscribers(salesRecord.getNewSubscribers() + 1);
        salesRecordRepository.save(salesRecord);

        return buildPurchaseResponse(vpnSubscription, subscription, server, userProfile, price, cycle);
    }

    private PurchaseResponse buildPurchaseResponse(VpnSubscription subscription, SubscriptionPlan plan,
                                                   VpnServer server, UserProfileDto userProfile,
                                                   BigDecimal price, VpnSubscription.BillingCycle cycle) {

        BigDecimal savings = BigDecimal.ZERO;
        if (cycle == VpnSubscription.BillingCycle.YEARLY) {
            savings = plan.getMonthlyPrice().multiply(BigDecimal.valueOf(12)).subtract(plan.getYearlyPrice());
        }

        int daysTotal = (int) ChronoUnit.DAYS.between(subscription.getStartDate(), subscription.getEndDate());

        return PurchaseResponse.builder()
                .subscriptionId(subscription.getId().toString())
                .message("Поздравляем! VPN подписка успешно активирована")
                .plan(PurchaseResponse.PlanInfo.builder()
                        .id(plan.getId().toString())
                        .name(plan.getName())
                        .type(plan.getType().name().toLowerCase())
                        .maxConnections(plan.getMaxConnections())
                        .bandwidthLimit(plan.getBandwidthLimit())
                        .speedLimit(plan.getSpeedLimit())
                        .build())
                .server(PurchaseResponse.ServerInfo.builder()
                        .id(server.getId().toString())
                        .name(server.getName())
                        .country(server.getCountry())
                        .city(server.getCity())
                        .flag(getCountryFlag(server.getCountryCode()))
                        .ipAddress(server.getIpAddress())
                        .ping(server.getPing())
                        .uptime(server.getUptime())
                        .build())
                .subscription(PurchaseResponse.SubscriptionInfo.builder()
                        .billingCycle(cycle.name().toLowerCase())
                        .startDate(subscription.getStartDate())
                        .endDate(subscription.getEndDate())
                        .daysTotal(daysTotal)
                        .isActive(subscription.getIsActive())
                        .userEmail(userProfile.getEmail())
                        .build())
                .payment(PurchaseResponse.PaymentInfo.builder()
                        .amount(price)
                        .currency("USD")
                        .billingCycle(cycle.name().toLowerCase())
                        .savings(savings)
                        .paymentDate(OffsetDateTime.now())
                        .build())
                .build();
    }
}
