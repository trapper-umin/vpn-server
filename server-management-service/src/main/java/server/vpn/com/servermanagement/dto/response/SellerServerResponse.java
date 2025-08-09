package server.vpn.com.servermanagement.dto.response;

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
public class SellerServerResponse {

    private String id;
    private String name;
    private String country;
    private String countryCode;
    private String flag;
    private String city;
    private String ip;
    private Integer port;

    private Integer maxConnections;
    private Integer currentConnections;
    private String bandwidth;
    private String speed;
    private Integer ping;
    private BigDecimal uptime;
    private Boolean isOnline;
    private Boolean isActive;
    private LocalDate createdAt;
    private Integer totalSubscribers;
    private Integer activeSubscribers;
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private String status;
    private List<String> features;
    private List<String> protocols;
    private String description;
}