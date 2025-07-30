package server.vpn.com.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.auth.dto.LoginRequest;
import server.vpn.com.auth.dto.LoginResponse;
import server.vpn.com.auth.dto.RegisterRequest;
import server.vpn.com.auth.dto.UserProfileResponse;
import server.vpn.com.auth.service.AuthService;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Регистрация нового пользователя с автоматической авторизацией
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - регистрация пользователя: {}", request.getEmail());
        
        LoginResponse response = authService.register(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Вход пользователя в систему
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - вход пользователя: {}", request.getEmail());
        
        LoginResponse response = authService.login(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Получение профиля текущего пользователя
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        log.info("GET /api/auth/profile - получение профиля пользователя: {}", authentication.getName());
        
        UserProfileResponse profile = authService.getProfile(authentication.getName());
        
        return ResponseEntity.ok(profile);
    }
}