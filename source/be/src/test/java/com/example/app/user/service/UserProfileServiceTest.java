package com.example.app.user.service;

import com.example.app.auth.entity.RefreshSession;
import com.example.app.auth.repository.RefreshSessionRepository;
import com.example.app.auth.service.AccountSecurityService;
import com.example.app.user.dto.ChangeOwnPasswordRequest;
import com.example.app.user.dto.UpdateOwnProfileRequest;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.storage.avatar-root=target/test-storage/avatars")
class UserProfileServiceTest {
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountSecurityService accountSecurityService;
    @Autowired private RefreshSessionRepository refreshSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void updatesOwnProfileAndStoresOnlyOptimizedAvatar() throws Exception {
        User user = userRepository.save(new User(
                "profile@test.local", "Ten cu", passwordEncoder.encode("CurrentPass123"), Role.RECEPTIONIST));

        var updated = userService.updateOwnProfile(user.getEmail(), new UpdateOwnProfileRequest("Tên hiển thị mới"));
        assertThat(updated.displayName()).isEqualTo("Tên hiển thị mới");

        byte[] source = png(1800, 1000);
        var avatar = userService.updateAvatar(user.getEmail(), new MockMultipartFile(
                "file", "large.png", "image/png", source));

        assertThat(avatar.width()).isEqualTo(320);
        assertThat(avatar.height()).isEqualTo(320);
        assertThat(avatar.storedBytes()).isLessThan(source.length);
        assertThat(avatar.user().avatarUrl()).contains("/api/v1/users/" + user.getId() + "/avatar?v=");
        assertThat(userService.loadAvatar(user.getId()).bytes()).isEqualTo(avatar.storedBytes());

        var withoutAvatar = userService.deleteAvatar(user.getEmail());
        assertThat(withoutAvatar.avatarUrl()).isNull();
        assertThatThrownBy(() -> userService.loadAvatar(user.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Chưa có ảnh đại diện");
    }

    @Test
    void changesPasswordAndRevokesRefreshSessions() {
        User user = userRepository.save(new User(
                "password@test.local", "Nhan vien", passwordEncoder.encode("CurrentPass123"), Role.TECHNICIAN));
        refreshSessionRepository.save(new RefreshSession(
                "test-token-hash", user, Instant.now().plus(1, ChronoUnit.DAYS)));

        assertThatThrownBy(() -> accountSecurityService.changePassword(user.getEmail(),
                new ChangeOwnPasswordRequest("wrong-password", "NewSecurePass456")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Mật khẩu hiện tại không đúng");

        accountSecurityService.changePassword(user.getEmail(),
                new ChangeOwnPasswordRequest("CurrentPass123", "NewSecurePass456"));

        assertThat(passwordEncoder.matches("NewSecurePass456",
                userRepository.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThat(refreshSessionRepository.findByTokenHash("test-token-hash")).isEmpty();
    }

    private byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(new java.awt.GradientPaint(0, 0, new Color(32, 105, 59), width, height, Color.WHITE));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
