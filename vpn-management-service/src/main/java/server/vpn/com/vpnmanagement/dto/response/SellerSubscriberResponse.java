package server.vpn.com.vpnmanagement.dto.response;

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
public class SellerSubscriberResponse {

    private String id;
    private String email;
    private String planId;
    private String planName;
    private String serverId;
    private String serverName;
    private String billingCycle;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private BigDecimal totalPaid;
    private LocalDate lastLogin;
    private String country;
    private String flag;
}