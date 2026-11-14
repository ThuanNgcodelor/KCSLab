package com.example.app.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(min = 2, max = 120, message = "Tên hiển thị phải từ 2 đến 120 ký tự")
        String displayName
) {
}
