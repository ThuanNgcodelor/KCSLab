package com.example.app.user.dto;

public record AvatarUploadResponse(
        UserResponse user,
        long storedBytes,
        int width,
        int height
) {
}
