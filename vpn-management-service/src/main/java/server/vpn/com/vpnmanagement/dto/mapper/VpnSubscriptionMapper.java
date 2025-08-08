package server.vpn.com.vpnmanagement.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import server.vpn.com.vpnmanagement.dto.response.SellerSubscriberResponse;
import server.vpn.com.vpnmanagement.dto.response.UserSubscriptionResponse;
import server.vpn.com.vpnmanagement.entity.VpnSubscription;
import server.vpn.com.vpnmanagement.util.CountryCodeFlag;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(subscription.getPlan().getName() + \" - \" + subscription.getPlan().getServer().getName())")
    @Mapping(target = "country", source = "plan.server.country")
    @Mapping(target = "countryCode", source = "plan.server.countryCode")
    @Mapping(target = "flag", source = "plan.server.countryCode", qualifiedByName = "countryCodeToFlag")
    @Mapping(target = "server", source = "plan.server.name")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "daysLeft", source = ".", qualifiedByName = "calculateDaysLeft")
    @Mapping(target = "expiresAt", source = "endDate", qualifiedByName = "offsetDateTimeToISOString")
    @Mapping(target = "speed", source = "plan.speedLimit")
    @Mapping(target = "ping", expression = "java(generateMockPing())")
    @Mapping(target = "load", expression = "java(generateMockLoad())")
    @Mapping(target = "plan", source = "plan.type", qualifiedByName = "planTypeToString")
    @Mapping(target = "price", source = "totalPaid")
    @Mapping(target = "currency", constant = "USD")
    @Mapping(target = "planName", source = "plan.name")
    @Mapping(target = "serverName", source = "plan.server.name")
    @Mapping(target = "billingCycle", source = "billingCycle", qualifiedByName = "billingCycleToString")
    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(target = "endDate", source = "endDate", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(target = "totalPaid", source = "totalPaid")
    @Mapping(target = "planType", source = "plan.type", qualifiedByName = "planTypeToString")
    @Mapping(target = "maxConnections", source = "plan.maxConnections")
    @Mapping(target = "bandwidthLimit", source = "plan.bandwidthLimit")
    @Mapping(target = "speedLimit", source = "plan.speedLimit")
    UserSubscriptionResponse toUserSubscriptionResponse(VpnSubscription subscription);

    List<UserSubscriptionResponse> toUserSubscriptionResponseList(List<VpnSubscription> subscriptions);

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
        return CountryCodeFlag.getCountryFlag(countryCode);
    }

    @Named("calculateDaysLeft")
    default Integer calculateDaysLeft(VpnSubscription subscription) {
        if (subscription.getEndDate() == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(OffsetDateTime.now(), subscription.getEndDate());
        return Math.max(0, (int) days);
    }

    @Named("offsetDateTimeToISOString")
    default String offsetDateTimeToISOString(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDate().toString() : null;
    }

    @Named("planTypeToString")
    default String planTypeToString(server.vpn.com.vpnmanagement.entity.SubscriptionPlan.PlanType planType) {
        return planType != null ? planType.name().toLowerCase() : null;
    }

    default Integer generateMockPing() {
        return (int) (Math.random() * 50) + 10; // 10-60ms
    }

    default Integer generateMockLoad() {
        return (int) (Math.random() * 80) + 10; // 10-90%
    }
}