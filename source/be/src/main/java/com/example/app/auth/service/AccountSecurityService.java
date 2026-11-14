package com.example.app.auth.service;

import com.example.app.auth.repository.RefreshSessionRepository;
import com.example.app.user.dto.ChangeOwnPasswordRequest;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountSecurityService {
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountSecurityService(UserRepository userRepository,
                                  RefreshSessionRepository refreshSessionRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(String email, ChangeOwnPasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshSessionRepository.deleteAllByUser_Id(user.getId());
    }
}
