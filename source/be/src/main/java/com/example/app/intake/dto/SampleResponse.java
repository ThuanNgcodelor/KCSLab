package com.example.app.intake.dto;

import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.intake.entity.SampleCodingStatus;

import java.time.Instant;

public record SampleResponse(
        Long id,
        String sampleCode,
        String sourceCode,
        String productType,
        String name,
        String quantity,
        String packaging,
        String sampleCondition,
        String requestedParameters,
        ReceiptStatus status,
        SampleCodingStatus codingStatus,
        Instant codingVerifiedAt
) {}
