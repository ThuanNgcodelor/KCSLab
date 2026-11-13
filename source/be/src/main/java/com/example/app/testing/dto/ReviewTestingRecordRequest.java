package com.example.app.testing.dto;

import com.example.app.testing.entity.TestRecordStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewTestingRecordRequest(
        @NotNull TestRecordStatus decision,
        String reviewNote
) {}
