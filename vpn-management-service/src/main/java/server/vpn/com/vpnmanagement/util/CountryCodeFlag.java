package server.vpn.com.vpnmanagement.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CountryCodeFlag {

    public static String getCountryFlag(String countryCode) {
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
