package com.example.app.testing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReagentRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 120) String code,
        @Size(max = 120) String origin,
        LocalDate preparationDate,
        LocalDate expiryDate,
        @Size(max = 160) String storageCondition,
        @Size(max = 120) String concentration
) {}
