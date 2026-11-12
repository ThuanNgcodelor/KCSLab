package com.example.app.auth.service;

import com.example.app.auth.entity.RefreshSession;
import com.example.app.auth.repository.RefreshSessionRepository;
import com.example.app.user.entity.User;
import com.example.app.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshSessionRepository refreshSessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UserService userService,
                       TokenService tokenService, RefreshSessionRepository refreshSessionRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshSessionRepository = refreshSessionRepository;
    }

    @Transactional
    public LoginResult login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email.trim().toLowerCase(), password));
        User user = (User) authentication.getPrincipal();
        return issue(user);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh session không hợp lệ");
        }
        RefreshSession session = refreshSessionRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Refresh session không hợp lệ"));
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())
                || !session.getUser().isEnabled()) {
            throw new IllegalArgumentException("Refresh session đã hết hạn hoặc bị thu hồi");
        }
        session.revoke();
        return issue(session.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshSessionRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(session -> {
            session.revoke();
            refreshSessionRepository.save(session);
        });
    }

    private LoginResult issue(User user) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshSessionRepository.save(new RefreshSession(hash(refreshToken), user,
                Instant.now().plus(30, ChronoUnit.DAYS)));
        return new LoginResult(tokenService.createAccessToken(user), refreshToken, user);
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

    public record LoginResult(String accessToken, String refreshToken, User user) {
    }
}
