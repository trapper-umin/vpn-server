package server.vpn.com.vpnmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionStatusResponse {

    private Boolean isActive;
    private Integer daysLeft;
    private String expiresAt;
    private List<UserSubscriptionResponse> subscriptions;
    private Integer totalSubscriptions;
    private Integer activeSubscriptions;
    private Integer uniqueCountries;
}