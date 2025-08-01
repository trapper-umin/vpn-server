package server.vpn.com.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.auth.dto.*;
import server.vpn.com.auth.entity.RefreshToken;
import server.vpn.com.auth.entity.User;
import server.vpn.com.auth.exception.InvalidCredentialsException;
import server.vpn.com.auth.exception.UserAlreadyExistsException;
import server.vpn.com.auth.mapper.RefreshTokenMapper;
import server.vpn.com.auth.mapper.UserMapper;
import server.vpn.com.auth.repository.UserRepository;
import server.vpn.com.auth.util.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static server.vpn.com.auth.util.HeadersUtil.*;
import static server.vpn.com.auth.util.enums.Constant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RefreshTokenMapper  refreshTokenMapper;
    private final AuthenticationManager authenticationManager;
    private final SessionManagementService sessionManagementService;

    @Transactional
    public LoginResponse register(RegisterRequest request, HttpServletRequest httpRequest) {

        String deviceInfo = extractDeviceInfo(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(USER_ALREADY_EXIST_MESSAGE);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        String accessToken = jwtUtil.generateToken(savedUser, savedUser.getTokenVersion());
        RefreshToken refreshToken = sessionManagementService.createRefreshToken(savedUser, deviceInfo, ipAddress);
        UserProfileResponse userProfile = userMapper.toProfileResponse(savedUser);

        return new LoginResponse(accessToken, refreshToken.getToken(), userProfile);
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest  httpRequest) {
        String deviceInfo = extractDeviceInfo(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_EMAIL_MESSAGE));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException(INVALID_PASSWORD_MESSAGE);
        }

        String accessToken = jwtUtil.generateToken(user,  user.getTokenVersion());
        RefreshToken refreshToken = sessionManagementService.createRefreshToken(user, deviceInfo, ipAddress);

        UserProfileResponse userProfile = userMapper.toProfileResponse(user);

        return new LoginResponse(accessToken, refreshToken.getToken(), userProfile);
    }

    public UserProfileResponse getProfile(String email) {

        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_EMAIL_MESSAGE));

        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public void logout(Map<String, String> request, HttpServletRequest httpRequest) {
        String refreshToken = request.get(REFRESH_TOKEN);
        String accessToken = extractAccessToken(httpRequest);

        try {
            if (refreshToken != null) {
                sessionManagementService.revokeRefreshToken(refreshToken);
            }

            if (accessToken != null) {
                jwtUtil.addTokenToBlacklist(accessToken);
            }
        } catch (Exception e) {
            log.warn(SOMETHING_WRONG_MESSAGE, e.getMessage());
        }
    }

    @Transactional
    public void logoutAll(String email, HttpServletRequest httpRequest) {
        String accessToken = extractAccessToken(httpRequest);

        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_EMAIL_MESSAGE));

        try {
            sessionManagementService.revokeAllRefreshTokens(user);

            if (accessToken != null) {
                userRepository.incrementTokenVersion(user.getId());
            }
        } catch (Exception e) {
            log.warn(SOMETHING_WRONG_MESSAGE, e.getMessage());
        }
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = sessionManagementService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtUtil.generateToken(user);

        RefreshToken newRefreshToken = sessionManagementService.createRefreshToken(
            user, 
            refreshToken.getDeviceInfo(), 
            refreshToken.getIpAddress()
        );

        sessionManagementService.revokeRefreshToken(request.getRefreshToken());
        
        UserProfileResponse userProfile = userMapper.toProfileResponse(user);

        return new LoginResponse(newAccessToken, newRefreshToken.getToken(), userProfile);
    }

    public List<SessionResponse> getActiveSessions(Authentication authentication, HttpServletRequest httpRequest) {
        String currentAccessToken = extractAccessToken(httpRequest);
        String email = authentication.getName();
        
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_EMAIL_MESSAGE));
        
        List<RefreshToken> activeSessions = sessionManagementService.getActiveSessions(user);

        String currentRefreshTokenId = null;
        if (currentAccessToken != null) {
            try {
                currentRefreshTokenId = activeSessions.stream()
                        .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                        .map(RefreshToken::getToken)
                        .orElse(null);
            } catch (Exception e) {
                log.warn("Ошибка определения текущей сессии: {}", e.getMessage()); //todo сделать нормальное определение текущей сессии
            }
        }
        
        final String finalCurrentRefreshTokenId = currentRefreshTokenId;
        return activeSessions.stream()
                .map(token ->
                        refreshTokenMapper.toSessionResponse(token, token.getToken().equals(finalCurrentRefreshTokenId)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(String email, Long sessionId) {
        
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_EMAIL_MESSAGE));
        
        List<RefreshToken> userSessions = sessionManagementService.getActiveSessions(user);
        RefreshToken sessionToRevoke = userSessions.stream()
                .filter(session -> session.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException(SESSION_NOT_FOUND_MESSAGE));
        
        sessionManagementService.revokeRefreshToken(sessionToRevoke);
    }
}