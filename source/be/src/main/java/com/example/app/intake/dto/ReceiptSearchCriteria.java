package com.example.app.intake.dto;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.ReceiptStatus;

import java.time.LocalDate;

public record ReceiptSearchCriteria(
        String query,
        LocalDate from,
        LocalDate to,
        ReceiptStatus status,
        AcceptanceDecision decision,
        String productType,
        Long receivedBy
) {
    public static ReceiptSearchCriteria empty() {
        return new ReceiptSearchCriteria(null, null, null, null, null, null, null);
    }
}
