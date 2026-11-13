package com.example.app.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTransferItemRequest(
        @NotNull Long sampleId,
        String weight,
        String quantity,
        String sampleCondition,
        String requestedParameters,
        @NotBlank String testMethod
) {}
