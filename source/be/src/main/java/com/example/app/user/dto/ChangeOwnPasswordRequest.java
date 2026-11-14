package com.example.app.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordRequest(
        @NotBlank(message = "Cần nhập mật khẩu hiện tại") String currentPassword,
        @NotBlank(message = "Cần nhập mật khẩu mới")
        @Size(min = 10, max = 128, message = "Mật khẩu mới phải từ 10 đến 128 ký tự")
        String newPassword
) {
}
