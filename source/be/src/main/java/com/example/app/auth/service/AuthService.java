package com.example.app.auth.service;

import com.example.app.user.entity.User;
import com.example.app.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class AuthService {
    private static final Duration REMEMBERED_TTL = Duration.ofDays(30);
    private static final Duration SESSION_TTL = Duration.ofDays(1);
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshSessionStore refreshSessionStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UserService userService,
                       TokenService tokenService, RefreshSessionStore refreshSessionStore) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshSessionStore = refreshSessionStore;
    }

    public LoginResult login(String email, String password, boolean rememberMe) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email.trim().toLowerCase(), password));
        User user = (User) authentication.getPrincipal();
        return issue(user, rememberMe);
    }

    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh session không hợp lệ");
        }
        RefreshSessionStore.SessionData session = refreshSessionStore.consume(hash(rawRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Refresh session không hợp lệ"));
        User user = userService.getByEmail(session.email());
        if (!user.getId().equals(session.userId()) || !user.isEnabled()) {
            throw new IllegalArgumentException("Refresh session đã hết hạn hoặc bị thu hồi");
        }
        return issue(user, session.remembered());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshSessionStore.delete(hash(rawRefreshToken));
    }

    private LoginResult issue(User user, boolean rememberMe) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration ttl = rememberMe ? REMEMBERED_TTL : SESSION_TTL;
        refreshSessionStore.save(hash(refreshToken), user.getId(), user.getEmail(), rememberMe, ttl);
        return new LoginResult(tokenService.createAccessToken(user), refreshToken, user, rememberMe);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    public record LoginResult(String accessToken, String refreshToken, User user, boolean rememberMe) {
    }
}
