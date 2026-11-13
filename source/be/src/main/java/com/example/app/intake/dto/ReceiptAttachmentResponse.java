package com.example.app.intake.dto;

import java.time.Instant;

public record ReceiptAttachmentResponse(Long id, String originalName, String contentType, long size, String checksum, Instant uploadedAt) {}
