package com.example.app.testing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaveTestingRecordRequest(
        @NotNull Long transferItemId,
        Instant analysisStart,
        Instant analysisEnd,
        String calibrationCurve,
        String calculationFormula,
        @DecimalMin("0.0") @DecimalMax("999.9999") BigDecimal recoveryPercent,
        String qcChartNote,
        String analystNote,
        @NotNull List<@Valid EquipmentRequest> equipment,
        @NotNull List<@Valid ReagentRequest> reagents,
        @NotEmpty List<@Valid MeasurementRequest> measurements
) {}
