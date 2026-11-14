package com.example.app.intake.dto;

import com.example.app.intake.entity.ReceiptStatus;

import java.time.Instant;
import java.time.LocalDate;

public record IntakeDashboardReceiptResponse(
        Long id,
        String receiptCode,
        String customerName,
        String sampleName,
        int sampleCount,
        String receivedByName,
        Instant receivedAt,
        LocalDate expectedDate,
        ReceiptStatus status
) {}
