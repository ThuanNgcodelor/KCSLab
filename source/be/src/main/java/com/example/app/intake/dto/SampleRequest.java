package com.example.app.intake.dto;

import jakarta.validation.constraints.NotBlank;

public record SampleRequest(
        @NotBlank String sourceCode,
        @NotBlank String productType,
        @NotBlank String name,
        String quantity,
        String packaging,
        String condition,
        String requestedParameters
) {}
