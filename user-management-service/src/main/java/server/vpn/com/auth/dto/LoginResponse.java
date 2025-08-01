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
    private String refreshToken;
    private UserProfileResponse user;

    public LoginResponse(String token, String refreshToken, UserProfileResponse user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
        this.type = "Bearer";
    }
}