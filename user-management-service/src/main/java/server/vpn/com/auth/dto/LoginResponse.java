package server.vpn.com.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private UserProfileResponse user;

    public LoginResponse(String token, UserProfileResponse user) {
        this.token = token;
        this.user = user;
    }
}