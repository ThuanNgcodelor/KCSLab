package com.example.app.intake.dto;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.ReceiptStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReceiptResponse(
        Long id, String receiptCode, String customerName, String customerPhone,
        Long receivedById, String receivedByName, String deliveredBy,
        String sampledBy, Instant sampledAt, String growingArea, String packingFacility,
        String vehicleNumber, String trailerNumber, String containerNumber,
        Instant receivedAt, String sampleName, String quantity, String packaging, String sealNumber,
        boolean sealIntact, String sampleCondition, String conditionDetail, String transportInfo,
        Double receivedTemperature, String storageCondition, String requestedParameters, LocalDate expectedDate,
        String specialRequest, AcceptanceDecision acceptanceDecision, String acceptanceReason, ReceiptStatus status,
        List<SampleResponse> samples, List<ReceiptAttachmentResponse> attachments
) {}
