package com.example.app.user.dto;

import com.example.app.user.entity.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 120) String displayName,
        Role role,
        Boolean enabled,
        @Size(min = 12, max = 128) String password
) {
}
