package com.example.app.testing.dto;

import java.time.LocalDate;

public record ReagentResponse(
        Long id,
        String name,
        String code,
        String origin,
        LocalDate preparationDate,
        LocalDate expiryDate,
        String storageCondition,
        String concentration
) {}
