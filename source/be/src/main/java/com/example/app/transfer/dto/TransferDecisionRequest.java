package com.example.app.transfer.dto;

import com.example.app.transfer.entity.TransferStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TransferDecisionRequest(
        @NotNull TransferStatus decision,
        String receivingNote,
        String labOpinion,
        boolean labRetain,
        LocalDate labRetentionUntil
) {}
