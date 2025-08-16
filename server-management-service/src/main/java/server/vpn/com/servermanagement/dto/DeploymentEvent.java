package server.vpn.com.servermanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import server.vpn.com.servermanagement.util.enums.WireGuardDeploymentStage;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentEvent {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime ts;
    
    private String jobId;
    
    private Long seq;
    
    private WireGuardDeploymentStage stage;
    
    private EventLevel level;
    
    private String message;
    
    private Integer progress;
    
    private Map<String, Object> details;
    
    @Getter
    @RequiredArgsConstructor
    public enum EventLevel {
        INFO("INFO"),
        WARNING("WARNING"),
        ERROR("ERROR"),
        DEBUG("DEBUG");
        
        private final String value;
    }
}
