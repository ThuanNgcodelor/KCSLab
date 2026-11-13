package com.example.app.testing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EquipmentRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 120) String serialNumber,
        @Size(max = 160) String manufacturer,
        @Size(max = 120) String identificationCode,
        LocalDate calibrationDate
) {}
