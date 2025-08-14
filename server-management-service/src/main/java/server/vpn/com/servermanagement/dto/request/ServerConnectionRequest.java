package server.vpn.com.servermanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConnectionRequest {

    @NotNull(message = "SSH конфигурация обязательна")
    @Valid
    private SshConfig ssh;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SshConfig {
        
        @NotNull(message = "Хост обязателен")
        private String host;
        
        @Builder.Default
        private Integer port = 22;
        
        @NotNull(message = "Пользователь обязателен")
        private String user;
        
        @NotNull(message = "Тип аутентификации обязателен")
        private AuthType auth;
        
        // Для auth = "password"
        private String password;
        
        // Для auth = "key"
        private String privateKey;
        private String passphrase;
        
        // Дополнительно для sudo операций
        private String sudoPassword;
        
        // Настройки проверки хост-ключа
        @Valid
        @Builder.Default
        private HostKeyConfig hostKey = HostKeyConfig.builder()
                .verify(HostKeyVerifyType.accept_new)
                .build();
    }
    
    public enum AuthType {
        password, key
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HostKeyConfig {
        
        @Builder.Default
        private HostKeyVerifyType verify = HostKeyVerifyType.accept_new;
        
        private String fingerprintSha256;
    }
    
    public enum HostKeyVerifyType {
        @JsonProperty("accept-new")
        accept_new,
        
        @JsonProperty("strict") 
        strict,
        
        @JsonProperty("insecure")
        insecure
    }
}