package com.example.app.intake.dto;

import com.example.app.intake.entity.AcceptanceDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateReceiptRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @Email String customerEmail,
        String customerAddress,
        @NotBlank String deliveredBy,
        Instant receivedAt,
        @NotBlank String sampleName,
        String quantity,
        String packaging,
        String sealNumber,
        boolean sealIntact,
        @NotBlank String sampleCondition,
        String conditionDetail,
        String transportInfo,
        Double receivedTemperature,
        String storageCondition,
        String requestedParameters,
        LocalDate expectedDate,
        String specialRequest,
        AcceptanceDecision acceptanceDecision,
        String acceptanceReason,
        @NotEmpty List<@Valid SampleRequest> samples
) {}
