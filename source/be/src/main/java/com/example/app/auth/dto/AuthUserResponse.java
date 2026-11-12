package com.example.app.auth.dto;

import com.example.app.user.entity.Role;

public record AuthUserResponse(Long id, String email, String displayName, Role role) {
}
