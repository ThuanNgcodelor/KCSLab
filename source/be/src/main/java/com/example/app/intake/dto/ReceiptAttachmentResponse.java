package com.example.app.intake.dto;

import com.example.app.intake.entity.ReceiptAttachmentCategory;

import java.time.Instant;

public record ReceiptAttachmentResponse(
        Long id,
        ReceiptAttachmentCategory category,
        String description,
        String originalName,
        String contentType,
        long size,
        String checksum,
        Instant capturedAt,
        Instant uploadedAt,
        Long uploadedById,
        String uploadedByName,
        String contentUrl
) {}
