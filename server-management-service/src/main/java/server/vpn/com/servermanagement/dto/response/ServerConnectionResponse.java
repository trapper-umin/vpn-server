package server.vpn.com.servermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConnectionResponse {

    private Boolean success;
    
    private ConnectionChecks checks;
    
    private ConnectionDetails details;
    
    @Builder.Default
    private List<String> warnings = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionChecks {
        private Boolean dns;
        private Boolean tcp;
        private Boolean sshHandshake;
        private Boolean auth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionDetails {
        private String resolvedIp;
        private Long latencyMs;
        private String sshBanner;
        private HostKeyInfo hostKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HostKeyInfo {
        private String type;
        private String fingerprintSha256;
    }
}