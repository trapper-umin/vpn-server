package server.vpn.com.vpnmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO для ответа при покупке плана подписки
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private String subscriptionId;
    private String message;
    
    // Информация о плане
    private PlanInfo plan;
    
    // Информация о сервере
    private ServerInfo server;
    
    // Информация о подписке
    private SubscriptionInfo subscription;
    
    // Платежная информация
    private PaymentInfo payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanInfo {
        private String id;
        private String name;
        private String type;
        private Integer maxConnections;
        private String bandwidthLimit;
        private String speedLimit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private String id;
        private String name;
        private String country;
        private String city;
        private String flag;
        private String ipAddress;
        private Integer ping;
        private BigDecimal uptime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionInfo {
        private String billingCycle;
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private Integer daysTotal;
        private Boolean isActive;
        private String userEmail;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private BigDecimal amount;
        private String currency;
        private String billingCycle;
        private BigDecimal savings; // Экономия при годовой подписке
        private OffsetDateTime paymentDate;
    }
}