package server.vpn.com.vpnmanagement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import server.vpn.com.vpnmanagement.dto.SellerSubscriberResponse;
import server.vpn.com.vpnmanagement.entity.VpnSubscription;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface VpnSubscriptionMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "userEmail")
    @Mapping(target = "planId", source = "plan.id")
    @Mapping(target = "planName", source = "plan.name")
    @Mapping(target = "serverId", source = "plan.server.id")
    @Mapping(target = "serverName", source = "plan.server.name")
    @Mapping(target = "billingCycle", source = "billingCycle", qualifiedByName = "billingCycleToString")
    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(target = "endDate", source = "endDate", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(target = "lastLogin", source = "lastLogin", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(target = "flag", source = "countryCode", qualifiedByName = "countryCodeToFlag")
    SellerSubscriberResponse toSellerSubscriberResponse(VpnSubscription subscription);

    List<SellerSubscriberResponse> toSellerSubscriberResponseList(List<VpnSubscription> subscriptions);

    @Named("billingCycleToString")
    default String billingCycleToString(VpnSubscription.BillingCycle billingCycle) {
        return billingCycle != null ? billingCycle.name().toLowerCase() : null;
    }

    @Named("offsetDateTimeToLocalDate")
    default LocalDate offsetDateTimeToLocalDate(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDate() : null;
    }

    @Named("countryCodeToFlag")
    default String countryCodeToFlag(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "🏳️";
        }
        
        return switch (countryCode.toUpperCase()) {
            case "US" -> "🇺🇸";
            case "DE" -> "🇩🇪";
            case "GB" -> "🇬🇧";
            case "JP" -> "🇯🇵";
            case "CA" -> "🇨🇦";
            case "FR" -> "🇫🇷";
            case "NL" -> "🇳🇱";
            case "CH" -> "🇨🇭";
            case "SG" -> "🇸🇬";
            case "AU" -> "🇦🇺";
            case "SE" -> "🇸🇪";
            case "NO" -> "🇳🇴";
            case "DK" -> "🇩🇰";
            case "FI" -> "🇫🇮";
            case "IT" -> "🇮🇹";
            case "ES" -> "🇪🇸";
            case "BR" -> "🇧🇷";
            case "IN" -> "🇮🇳";
            case "KR" -> "🇰🇷";
            case "HK" -> "🇭🇰";
            case "RU" -> "🇷🇺";
            default -> "🏳️";
        };
    }
}