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
public class ServerTestingResponse {

    private Boolean success;
    private List<TestResult> tests;
    private String overallStatus; // 'running', 'passed', 'failed'

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        private String id;
        private String name;
        private String status; // 'pending', 'running', 'passed', 'failed'
        private String details;
        private List<String> logs;
        private String error;
    }
}