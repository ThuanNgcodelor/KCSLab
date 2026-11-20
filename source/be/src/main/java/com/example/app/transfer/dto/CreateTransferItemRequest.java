package com.example.app.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTransferItemRequest(
        @NotNull Long sampleId,
        String weight,
        String quantity,
        String sampleCondition,
        String requestedParameters,
        @NotBlank @Size(max = 255, message = "Phương pháp thử tối đa 255 ký tự") String testMethod
) {}
