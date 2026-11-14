package com.example.app.user.service;

import com.example.app.user.dto.CreateUserRequest;
import com.example.app.user.dto.UpdateUserRequest;
import com.example.app.user.dto.UpdateOwnProfileRequest;
import com.example.app.user.dto.UserResponse;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AvatarStorageService avatarStorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }
        User user = new User(email, request.displayName().trim(),
                passwordEncoder.encode(request.password()), request.role());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getById(id);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.updateProfile(request.displayName().trim(),
                    request.role() == null ? user.getRole() : request.role(), request.enabled());
        } else {
            user.updateProfile(user.getDisplayName(),
                    request.role() == null ? user.getRole() : request.role(), request.enabled());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.changePassword(passwordEncoder.encode(request.password()));
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    @Transactional
    public UserResponse updateOwnProfile(String email, UpdateOwnProfileRequest request) {
        User user = getByEmail(email);
        user.updateProfile(request.displayName().trim(), user.getRole(), user.isEnabled());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AvatarUpdate updateAvatar(String email, MultipartFile file) {
        User user = getByEmail(email);
        AvatarStorageService.StoredAvatar stored = avatarStorageService.store(user.getId(), file);
        user.markAvatarUpdated();
        UserResponse response = toResponse(userRepository.save(user));
        return new AvatarUpdate(response, stored.bytes(), stored.width(), stored.height());
    }

    @Transactional
    public UserResponse deleteAvatar(String email) {
        User user = getByEmail(email);
        avatarStorageService.delete(user.getId());
        user.clearAvatar();
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AvatarStorageService.AvatarContent loadAvatar(Long id) {
        User user = getById(id);
        if (user.getAvatarUpdatedAt() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có ảnh đại diện");
        }
        return avatarStorageService.load(id);
    }

    private User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.isEnabled(), avatarUrl(user), user.getCreatedAt(), user.getUpdatedAt());
    }

    public String avatarUrl(User user) {
        return user.getAvatarUpdatedAt() == null ? null
                : "/api/v1/users/%d/avatar?v=%d".formatted(
                        user.getId(), user.getAvatarUpdatedAt().toEpochMilli());
    }

    public record AvatarUpdate(UserResponse user, long storedBytes, int width, int height) { }
}
