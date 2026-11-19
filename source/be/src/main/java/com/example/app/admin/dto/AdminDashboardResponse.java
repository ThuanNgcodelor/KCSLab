package com.example.app.admin.dto;

import java.time.LocalDate;

public record AdminDashboardResponse(
        LocalDate from,
        LocalDate to,
        long receiptsInPeriod,
        long samplesInPeriod,
        long transfersPending,
        long transfersAccepted,
        long transfersRejected,
        long testingDraft,
        long testingSubmitted,
        long testingReviewed,
        long resultDraft,
        long resultSubmitted,
        long resultPublished,
        long resultRevoked
) {}
