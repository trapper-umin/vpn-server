package server.vpn.com.vpnmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatsResponse {

    private Integer totalServers;
    private Integer activeServers;
    private Integer totalSubscribers;
    private Integer activeSubscribers;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private BigDecimal conversionRate;
    private BigDecimal churnRate;
}