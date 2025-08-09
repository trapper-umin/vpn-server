package server.vpn.com.vpnmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import server.vpn.com.vpnmanagement.dto.response.SalesDataResponse;
import server.vpn.com.vpnmanagement.dto.response.SellerServerResponse;
import server.vpn.com.vpnmanagement.dto.response.SellerStatsResponse;
import server.vpn.com.vpnmanagement.dto.response.SellerSubscriberResponse;
import server.vpn.com.vpnmanagement.entity.SalesRecord;
import server.vpn.com.vpnmanagement.entity.VpnServer;
import server.vpn.com.vpnmanagement.entity.VpnSubscription;
import server.vpn.com.vpnmanagement.dto.mapper.SalesRecordMapper;
import server.vpn.com.vpnmanagement.dto.mapper.VpnServerMapper;
import server.vpn.com.vpnmanagement.dto.mapper.VpnSubscriptionMapper;
import server.vpn.com.vpnmanagement.repository.SalesRecordRepository;
import server.vpn.com.vpnmanagement.repository.SubscriptionPlanRepository;
import server.vpn.com.vpnmanagement.repository.VpnServerRepository;
import server.vpn.com.vpnmanagement.repository.VpnSubscriptionRepository;
import server.vpn.com.vpnmanagement.util.JwtUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final JwtUtil jwtUtil;
    private final VpnServerRepository vpnServerRepository;
    private final VpnSubscriptionRepository vpnSubscriptionRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final VpnServerMapper vpnServerMapper;
    private final VpnSubscriptionMapper vpnSubscriptionMapper;
    private final SalesRecordMapper salesRecordMapper;

    public SellerStatsResponse getSellerStats(Authentication authentication) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        long totalServers = vpnServerRepository.countBySellerIdAndIsActiveTrue(sellerId);
        long activeServers = vpnServerRepository.countBySellerIdAndIsActiveTrueAndIsOnlineTrue(sellerId);
        BigDecimal totalRevenue = subscriptionPlanRepository.getTotalRevenueBySellerIdAndIsActiveTrue(sellerId);
        BigDecimal monthlyRevenue = subscriptionPlanRepository.getMonthlyRevenueBySellerIdAndIsActiveTrue(sellerId);
        Integer totalSubscribers = subscriptionPlanRepository.getTotalSubscribersBySellerIdAndIsActiveTrue(sellerId);
        Integer activeSubscribers = subscriptionPlanRepository.getActiveSubscribersBySellerIdAndIsActiveTrue(sellerId);

        BigDecimal averageRating = BigDecimal.valueOf(0);
        Integer totalReviews = 0;
        BigDecimal conversionRate = BigDecimal.valueOf(0);
        BigDecimal churnRate = BigDecimal.valueOf(0);

        return SellerStatsResponse.builder()
                .totalServers((int) totalServers)
                .activeServers((int) activeServers)
                .totalSubscribers(totalSubscribers != null ? totalSubscribers : 0)
                .activeSubscribers(activeSubscribers != null ? activeSubscribers : 0)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .conversionRate(conversionRate)
                .churnRate(churnRate)
                .build();
    }

    public List<SellerServerResponse> getSellerServers(Authentication authentication) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<VpnServer> servers = vpnServerRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        return vpnServerMapper.toSellerServerResponseList(servers);
    }

    public List<SellerSubscriberResponse> getSellerSubscribers(Authentication authentication) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());
        List<VpnSubscription> subscriptions = vpnSubscriptionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
        return vpnSubscriptionMapper.toSellerSubscriberResponseList(subscriptions);
    }

    public List<SalesDataResponse> getSalesData(Authentication authentication, int days) {
        UUID sellerId = jwtUtil.extractUserIdFromEmail(authentication.getName());

        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        List<SalesRecord> salesRecords = salesRecordRepository.findBySellerIdAndSaleDateAfterOrderBySaleDateAsc(sellerId, startDate);
        return salesRecordMapper.toSalesDataResponseList(salesRecords);
    }

}
