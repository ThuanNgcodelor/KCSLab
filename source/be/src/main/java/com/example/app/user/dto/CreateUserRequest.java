package com.example.app.user.dto;

import com.example.app.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotNull Role role
) {
}
