package server.vpn.com.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import server.vpn.com.auth.dto.*;
import server.vpn.com.auth.service.AuthService;

import java.util.List;
import java.util.Map;

import static server.vpn.com.auth.util.HeadersUtil.*;
import static server.vpn.com.auth.util.enums.Constant.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletRequest httpRequest) {
        log.info(REGISTER_REQUEST_MESSAGE, request.getEmail());
        
        LoginResponse response = authService.register(request, httpRequest);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        log.info(LOGIN_REQUEST_MESSAGE, request.getEmail());
        
        LoginResponse response = authService.login(request, httpRequest);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        log.info(PROFILE_REQUEST_MESSAGE, authentication.getName());

        UserProfileResponse profile = authService.getProfile(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(profile);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> request,
                                                      HttpServletRequest httpRequest) {
        log.info(LOGOUT_REQUEST_MESSAGE);

        authService.logout(request, httpRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("message", "Successful logout"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(Authentication authentication, HttpServletRequest httpRequest) {
        log.info(LOGOUT_ALL_REQUEST_MESSAGE, authentication.getName());

        authService.logoutAll(authentication.getName(), httpRequest);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("message", "All devices are logged out"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info(REFRESH_TOKEN_REQUEST_MESSAGE);

        LoginResponse response = authService.refreshToken(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getActiveSessions(Authentication authentication, HttpServletRequest httpRequest) {
        log.info(GET_SESSIONS_REQUEST_MESSAGE, authentication.getName());

        List<SessionResponse> sessions = authService.getActiveSessions(authentication, httpRequest);
        
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> revokeSession(@PathVariable Long sessionId, Authentication authentication) {
        log.info(REVOKE_SESSION_REQUEST_MESSAGE, sessionId, authentication.getName());
        
        authService.revokeSession(authentication.getName(), sessionId);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("message", "Session revoked"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication, HttpServletRequest httpRequest) {
        log.info(DELETE_ACCOUNT_REQUEST_MESSAGE, authentication.getName());
        
        authService.deleteAccount(authentication.getName(), httpRequest);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("message", ACCOUNT_DELETED_MESSAGE));
    }

    @PostMapping("/become-seller")
    public ResponseEntity<UserProfileResponse> becomeSeller(@Valid @RequestBody BecomeSellerRequest request,
                                                           Authentication authentication) {
        log.info(BECOME_SELLER_REQUEST_MESSAGE, authentication.getName());
        
        UserProfileResponse response = authService.becomeSeller(authentication.getName(), request);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}