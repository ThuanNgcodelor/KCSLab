package com.example.app.transfer.dto;

import com.example.app.transfer.entity.TransferStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TransferResponse(
        Long id,
        String transferCode,
        Long receiptId,
        String receiptCode,
        String customerName,
        String receivingDepartment,
        String deliveredBy,
        String receivedBy,
        LocalDate transferDate,
        LocalDate resultsDueDate,
        boolean returnToCustomer,
        LocalDate customerReturnDate,
        String changesNote,
        TransferStatus status,
        String receivingNote,
        String labOpinion,
        boolean labRetain,
        LocalDate labRetentionUntil,
        Instant submittedAt,
        Instant receivedAt,
        Instant createdAt,
        List<TransferItemResponse> items
) {}
