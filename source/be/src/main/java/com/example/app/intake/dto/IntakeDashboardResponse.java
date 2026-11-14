package com.example.app.intake.dto;

import java.time.LocalDate;
import java.util.List;

public record IntakeDashboardResponse(
        LocalDate from,
        LocalDate to,
        long totalReceipts,
        long totalSamples,
        long accepted,
        long conditional,
        long rejected,
        long awaitingTransfer,
        List<IntakeDashboardReceiptResponse> recentReceipts
) {}
