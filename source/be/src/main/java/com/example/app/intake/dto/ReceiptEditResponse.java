package com.example.app.intake.dto;

import java.time.Instant;

public record ReceiptEditResponse(Long id, String editedBy, Instant editedAt, String reason, boolean afterTransfer, String summary) {}
