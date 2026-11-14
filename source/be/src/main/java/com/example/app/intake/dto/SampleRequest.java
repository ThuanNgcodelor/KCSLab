package com.example.app.intake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SampleRequest(
        @NotBlank @Size(max = 20) String sourceCode,
        @NotBlank @Size(max = 80) String productType,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 80) String quantity,
        @Size(max = 80) String packaging,
        @Size(max = 80) String condition,
        String requestedParameters
) {}
