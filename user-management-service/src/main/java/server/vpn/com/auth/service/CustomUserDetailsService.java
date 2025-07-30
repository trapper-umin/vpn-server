package server.vpn.com.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import server.vpn.com.auth.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Загрузка пользователя по email: {}", email);
        
        return userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", email);
                    return new UsernameNotFoundException("Пользователь с email " + email + " не найден");
                });
    }
}