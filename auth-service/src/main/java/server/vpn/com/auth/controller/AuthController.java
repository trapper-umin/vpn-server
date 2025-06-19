package server.vpn.com.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.auth.model.dto.AuthResponse;
import server.vpn.com.auth.model.dto.LoginRequest;
import server.vpn.com.auth.model.dto.RegisterRequest;
import server.vpn.com.auth.model.dto.UserProfileResponse;
import server.vpn.com.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse response = authService.getUserProfile(email);
        return ResponseEntity.ok(response);
    }
} 