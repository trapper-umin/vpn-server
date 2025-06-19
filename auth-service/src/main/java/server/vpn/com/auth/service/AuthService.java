package server.vpn.com.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import server.vpn.com.auth.model.User;
import server.vpn.com.auth.model.dto.AuthResponse;
import server.vpn.com.auth.model.dto.LoginRequest;
import server.vpn.com.auth.model.dto.RegisterRequest;
import server.vpn.com.auth.model.dto.UserProfileResponse;
import server.vpn.com.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // Проверяем, не существует ли пользователь с таким email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует");
        }

        // Создаем нового пользователя
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedBy("system");
        user.setChangedBy("system");

        // Сохраняем пользователя
        userRepository.save(user);

        // Генерируем JWT токен
        String jwtToken = jwtService.generateToken(user.getEmail());
        
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // Проверяем учетные данные
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Ищем пользователя
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль"));

        // Генерируем JWT токен
        String jwtToken = jwtService.generateToken(user.getEmail());
        
        return new AuthResponse(jwtToken);
    }

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        UserProfileResponse response = new UserProfileResponse();
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        
        return response;
    }
} 