package com.example.app.user.dto;

import com.example.app.user.entity.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        boolean enabled,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
