package server.vpn.com.vpnmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConnectionResponse {

    private Boolean success;
    private String error;
    private ServerInfo serverInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        private String ip;
        private String os;
        private String region;
        private String provider;
    }
}