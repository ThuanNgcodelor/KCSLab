package com.example.app.intake.dto;

import com.example.app.intake.entity.AcceptanceDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateReceiptRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @Email String customerEmail,
        String customerAddress,
        @Size(max = 160) String sampledBy,
        Instant sampledAt,
        @Size(max = 255) String growingArea,
        @Size(max = 255) String packingFacility,
        @Size(max = 80) String vehicleNumber,
        @Size(max = 80) String trailerNumber,
        @Size(max = 80) String containerNumber,
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
