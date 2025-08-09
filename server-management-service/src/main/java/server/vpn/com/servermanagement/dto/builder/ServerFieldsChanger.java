package server.vpn.com.servermanagement.dto.builder;

import lombok.experimental.UtilityClass;
import server.vpn.com.servermanagement.dto.request.CreateServerRequest;
import server.vpn.com.servermanagement.entity.VpnServer;

@UtilityClass
public class ServerFieldsChanger {

    public static void changeServerFields(VpnServer server, CreateServerRequest request) {
        server.setName(request.getName());
        server.setCountry(request.getCountry());
        server.setCountryCode(request.getCountryCode());
        server.setCity(request.getCity());
        server.setMaxConnections(request.getMaxConnections());
        server.setBandwidth(request.getBandwidth());
        server.setSpeed(request.getSpeed());
        server.setDescription(request.getDescription());
        server.setFeatures(request.getFeatures());
    }
}
