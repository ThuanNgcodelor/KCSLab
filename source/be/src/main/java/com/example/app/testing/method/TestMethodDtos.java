package com.example.app.testing.method;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TestMethodDtos {
    private TestMethodDtos() {}

    public record SaveRequest(
            @NotBlank(message = "Mã phương pháp không được để trống") @Size(max = 80)
            @Pattern(regexp = "^[\\p{L}0-9][\\p{L}0-9 ._:/()-]*$", message = "Mã phương pháp chỉ gồm chữ, số và . _ : / ( ) -")
            String code,
            @Size(max = 255) String name,
            Boolean active) {}

    public record Response(Long id, String code, String name, boolean active, Instant createdAt, Instant updatedAt) {
        static Response from(TestMethod method) {
            return new Response(method.getId(), method.getCode(), method.getName(), method.isActive(),
                    method.getCreatedAt(), method.getUpdatedAt());
        }
    }
}
