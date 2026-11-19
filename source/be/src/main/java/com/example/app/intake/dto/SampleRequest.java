package com.example.app.intake.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SampleRequest(
        @Size(max = 80) String sampleCode,
        @NotBlank @Size(max = 20) String sourceCode,
        @NotBlank @Size(max = 80) String productType,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 80) String quantity,
        @Size(max = 80) String packaging,
        @Size(max = 255) String condition,
        String requestedParameters
) {
    public SampleRequest(String sourceCode, String productType, String name, String quantity,
                         String packaging, String condition, String requestedParameters) {
        this(null, sourceCode, productType, name, quantity, packaging, condition, requestedParameters);
    }
}
