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
public class WireGuardDeploymentResponse {

    private Boolean success;
    private List<DeploymentStep> steps;
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeploymentStep {
        private String id;
        private String name;
        private String status; // 'pending', 'running', 'completed', 'error'
        private String details;
        private List<String> logs;
        private String error;
    }
}