package server.vpn.com.vpnmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionResponse {

    private String id;
    private String name;
    private String country;
    private String countryCode;
    private String flag;
    private String server;
    private Boolean isActive;
    private Integer daysLeft;
    private String expiresAt;
    private String speed;
    private Integer ping;
    private Integer load;
    private String plan;
    private BigDecimal price;
    private String currency;
    
    // Дополнительные поля для расширенной информации
    private String planName;
    private String serverName;
    private String billingCycle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalPaid;
    private String planType;
    private Integer maxConnections;
    private String bandwidthLimit;
    private String speedLimit;
}