package server.vpn.com.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";

    public AuthResponse(String token) {
        this.token = token;
    }
} 