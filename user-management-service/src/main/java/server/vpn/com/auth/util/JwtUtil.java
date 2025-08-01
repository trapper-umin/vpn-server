package server.vpn.com.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import server.vpn.com.auth.entity.BlacklistedToken;
import server.vpn.com.auth.entity.User;
import server.vpn.com.auth.repository.BlacklistedTokenRepository;
import server.vpn.com.auth.repository.UserRepository;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

import static server.vpn.com.auth.util.enums.Constant.INVALID_EMAIL_MESSAGE;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final UserRepository userRepository;

    private static final String TOKEN_VERSION = "token_version";

    @Value("${jwt.secret:myDefaultSecretKeyThatShouldBeReplacedInProduction123456789}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 часа в миллисекундах
    private Long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(UserDetails userDetails, Long tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_VERSION, tokenVersion);

        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        String jti = UUID.randomUUID().toString();
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setId(jti)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !isTokenBlacklisted(token) && isVersionMatch(username, token));
    }

    private boolean isVersionMatch(String username, String token) {
        User user = userRepository.findByEmailAndIsActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_EMAIL_MESSAGE));
        Long tokenVersion = extractClaim(token, claims -> claims.get(TOKEN_VERSION, Long.class));

        return Objects.equals(user.getTokenVersion(), tokenVersion);
    }


    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token) && !isTokenBlacklisted(token));
    }

    public Boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token) && !isTokenBlacklisted(token);
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            String jti = extractJti(token);
            return jti != null && blacklistedTokenRepository.existsByJti(jti);
        } catch (Exception e) {
            log.warn("Ошибка при проверке blacklist для токена: {}", e.getMessage());
            return true; // В случае ошибки считаем токен недействительным
        }
    }

    public void addTokenToBlacklist(String token) {
        String jti = extractJti(token);
        String username = extractUsername(token);
        Date expiration = extractExpiration(token);

        if (jti != null && username != null && expiration != null) {
            var blacklistedToken = BlacklistedToken.builder()
                    .jti(jti)
                    .userEmail(username)
                    .expiresAt(expiration.toInstant().atOffset(java.time.ZoneOffset.UTC))
                    .build();

            blacklistedTokenRepository.save(blacklistedToken);
        }
    }
}