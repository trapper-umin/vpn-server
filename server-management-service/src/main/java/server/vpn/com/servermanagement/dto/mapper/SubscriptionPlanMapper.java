package server.vpn.com.servermanagement.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import server.vpn.com.servermanagement.dto.request.CreateSubscriptionPlanRequest;
import server.vpn.com.servermanagement.dto.response.SubscriptionResponse;
import server.vpn.com.servermanagement.entity.SubscriptionPlan;
import server.vpn.com.servermanagement.util.CountryCodeFlag;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionPlanMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "serverId", source = "server.id")
    @Mapping(target = "serverName", source = "server.name")
    @Mapping(target = "serverCountry", source = "server.country")
    @Mapping(target = "serverCity", source = "server.city")
    @Mapping(target = "serverFlag", source = "server.countryCode", qualifiedByName = "countryCodeToFlag")
    @Mapping(target = "type", source = "type", qualifiedByName = "planTypeToString")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetDateTimeToLocalDate")
    SubscriptionResponse toSubscriptionPlanResponse(SubscriptionPlan plan);

    List<SubscriptionResponse> toSubscriptionPlanResponseList(List<SubscriptionPlan> plans);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "server", ignore = true)
    @Mapping(target = "type", source = "type", qualifiedByName = "stringToPlanType")
    @Mapping(target = "totalSubscribers", ignore = true)
    @Mapping(target = "activeSubscribers", ignore = true)
    @Mapping(target = "totalRevenue", ignore = true)
    @Mapping(target = "monthlyRevenue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SubscriptionPlan toEntity(CreateSubscriptionPlanRequest request);

    @Named("planTypeToString")
    default String planTypeToString(SubscriptionPlan.PlanType type) {
        return type != null ? type.name().toLowerCase() : null;
    }

    @Named("stringToPlanType")
    default SubscriptionPlan.PlanType stringToPlanType(String type) {
        if (type == null) return null;
        try {
            return SubscriptionPlan.PlanType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SubscriptionPlan.PlanType.BASIC;
        }
    }

    @Named("offsetDateTimeToLocalDate")
    default LocalDate offsetDateTimeToLocalDate(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDate() : null;
    }

    @Named("countryCodeToFlag")
    default String countryCodeToFlag(String countryCode) {
        return CountryCodeFlag.getCountryFlag(countryCode);
    }
}