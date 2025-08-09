package server.vpn.com.vpnmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private String id;
    private String name;
    private String serverId;
    private String serverName;
    private String serverCountry;
    private String serverCity;
    private String serverFlag;
    private String type;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private Integer maxConnections;
    private String bandwidthLimit;
    private String speedLimit;
    private Boolean isActive;
    private Boolean isPopular;
    private Integer sortOrder;
    private List<String> features;
    private String description;
    private Integer totalSubscribers;
    private Integer activeSubscribers;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private LocalDate createdAt;
}