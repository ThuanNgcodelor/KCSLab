package com.example.app.auth.service;

import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void rotatesRememberedRefreshTokenAndRejectsReuse() {
        String email = "refresh-rotation@test.local";
        userRepository.save(new User(email, "Refresh test",
                passwordEncoder.encode("CurrentPass123"), Role.RECEPTIONIST));

        AuthService.LoginResult login = authService.login(email, "CurrentPass123", true);
        assertThat(login.rememberMe()).isTrue();

        AuthService.LoginResult refreshed = authService.refresh(login.refreshToken());
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());
        assertThat(refreshed.rememberMe()).isTrue();

        assertThatThrownBy(() -> authService.refresh(login.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không hợp lệ");

        authService.logout(refreshed.refreshToken());
        assertThatThrownBy(() -> authService.refresh(refreshed.refreshToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không hợp lệ");
    }

    @Test
    void preservesBrowserSessionModeWhenRefreshing() {
        String email = "browser-session@test.local";
        userRepository.save(new User(email, "Session test",
                passwordEncoder.encode("CurrentPass123"), Role.TECHNICIAN));

        AuthService.LoginResult login = authService.login(email, "CurrentPass123", false);
        AuthService.LoginResult refreshed = authService.refresh(login.refreshToken());

        assertThat(login.rememberMe()).isFalse();
        assertThat(refreshed.rememberMe()).isFalse();
    }
}
