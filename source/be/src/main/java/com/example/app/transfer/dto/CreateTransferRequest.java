package com.example.app.transfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateTransferRequest(
        @NotNull Long receiptId,
        @NotNull LocalDate transferDate,
        @NotNull LocalDate resultsDueDate,
        boolean returnToCustomer,
        LocalDate customerReturnDate,
        String changesNote,
        @NotEmpty List<@Valid CreateTransferItemRequest> items
) {}
