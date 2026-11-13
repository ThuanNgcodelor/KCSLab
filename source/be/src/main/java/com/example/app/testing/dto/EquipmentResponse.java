package com.example.app.testing.dto;

import java.time.LocalDate;

public record EquipmentResponse(
        Long id,
        String name,
        String serialNumber,
        String manufacturer,
        String identificationCode,
        LocalDate calibrationDate
) {}
