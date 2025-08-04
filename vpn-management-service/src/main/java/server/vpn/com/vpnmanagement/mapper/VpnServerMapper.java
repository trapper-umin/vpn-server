package server.vpn.com.vpnmanagement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import server.vpn.com.vpnmanagement.dto.CreateServerRequest;
import server.vpn.com.vpnmanagement.dto.SellerServerResponse;
import server.vpn.com.vpnmanagement.entity.VpnServer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface VpnServerMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "ip", source = "ipAddress")

    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetDateTimeToLocalDate")
    @Mapping(target = "flag", source = "countryCode", qualifiedByName = "countryCodeToFlag")
    SellerServerResponse toSellerServerResponse(VpnServer vpnServer);

    List<SellerServerResponse> toSellerServerResponseList(List<VpnServer> vpnServers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "port", ignore = true)

    @Mapping(target = "currentConnections", ignore = true)
    @Mapping(target = "ping", ignore = true)
    @Mapping(target = "uptime", ignore = true)
    @Mapping(target = "isOnline", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "totalSubscribers", ignore = true)
    @Mapping(target = "activeSubscribers", ignore = true)
    @Mapping(target = "totalRevenue", ignore = true)
    @Mapping(target = "monthlyRevenue", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "protocols", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    VpnServer toEntity(CreateServerRequest request);



    @Named("statusToString")
    default String statusToString(VpnServer.ServerStatus status) {
        return status != null ? status.name().toLowerCase() : null;
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
        
        // Mapping some common country codes to flags
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