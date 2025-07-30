package server.vpn.com.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.vpn.com.auth.dto.*;
import server.vpn.com.auth.entity.User;
import server.vpn.com.auth.exception.InvalidCredentialsException;
import server.vpn.com.auth.exception.UserAlreadyExistsException;
import server.vpn.com.auth.mapper.UserMapper;
import server.vpn.com.auth.repository.UserRepository;
import server.vpn.com.auth.util.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация нового пользователя с автоматической авторизацией
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Попытка регистрации пользователя с email: {}", request.getEmail());
        
        // Проверяем, существует ли пользователь с таким email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
        }

        // Создаем нового пользователя
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        User savedUser = userRepository.save(user);
        log.info("Пользователь успешно зарегистрирован: {}", savedUser.getEmail());
        
        // Автоматически авторизуем пользователя после регистрации
        String token = jwtUtil.generateToken(savedUser);
        UserProfileResponse userProfile = userMapper.toProfileResponse(savedUser);
        
        log.info("Пользователь {} автоматически авторизован после регистрации", savedUser.getEmail());
        return new LoginResponse(token, userProfile);
    }

    /**
     * Аутентификация пользователя
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Попытка входа пользователя с email: {}", request.getEmail());
        
        try {
            // Аутентифицируем пользователя
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), 
                            request.getPassword()
                    )
            );

            // Получаем пользователя из базы данных
            User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                    .orElseThrow(() -> new InvalidCredentialsException("Неверный email или пароль"));

            // Генерируем JWT токен
            String token = jwtUtil.generateToken(user);

            // Создаем ответ
            UserProfileResponse userProfile = userMapper.toProfileResponse(user);
            
            log.info("Пользователь {} успешно вошел в систему", user.getEmail());
            return new LoginResponse(token, userProfile);
            
        } catch (Exception e) {
            log.warn("Неудачная попытка входа для email: {}", request.getEmail());
            throw new InvalidCredentialsException("Неверный email или пароль");
        }
    }

    /**
     * Получение профиля пользователя по токену
     */
    public UserProfileResponse getProfile(String email) {
        log.info("Получение профиля пользователя: {}", email);
        
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        
        return userMapper.toProfileResponse(user);
    }
}