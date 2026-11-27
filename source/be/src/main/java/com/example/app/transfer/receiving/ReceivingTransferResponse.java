package com.example.app.transfer.receiving;

import com.example.app.transfer.dto.TransferItemResponse;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.entity.TransferStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Transfer projection for KCS: never serialize the customer fields from TransferResponse. */
public record ReceivingTransferResponse(
        Long id, String transferCode, String receiptCode, String requestNumber, String receivingDepartment,
        String deliveredBy, String receivedBy, LocalDate transferDate, LocalDate resultsDueDate,
        boolean returnToCustomer, LocalDate customerReturnDate, String changesNote,
        TransferStatus status, String receivingNote, String labOpinion, boolean labRetain,
        LocalDate labRetentionUntil, Instant submittedAt, Instant receivedAt, Instant createdAt,
        List<TransferItemResponse> items, List<Assignee> assignees) {
    /** A technician tagged on the transfer to run the tests. */
    public record Assignee(Long id, String displayName, String avatarUrl) {}

    public ReceivingTransferResponse withAssignees(List<Assignee> list) {
        return new ReceivingTransferResponse(id, transferCode, receiptCode, requestNumber, receivingDepartment, deliveredBy,
                receivedBy, transferDate, resultsDueDate, returnToCustomer, customerReturnDate, changesNote, status,
                receivingNote, labOpinion, labRetain, labRetentionUntil, submittedAt, receivedAt, createdAt, items, list);
    }

    public static ReceivingTransferResponse from(TransferResponse transfer) {
        return new ReceivingTransferResponse(transfer.id(), transfer.transferCode(), transfer.receiptCode(),
                transfer.requestNumber(), transfer.receivingDepartment(), transfer.deliveredBy(), transfer.receivedBy(),
                transfer.transferDate(), transfer.resultsDueDate(), transfer.returnToCustomer(),
                transfer.customerReturnDate(), transfer.changesNote(), transfer.status(),
                transfer.receivingNote(), transfer.labOpinion(), transfer.labRetain(),
                transfer.labRetentionUntil(), transfer.submittedAt(), transfer.receivedAt(),
                transfer.createdAt(), transfer.items(), List.of());
    }
}
