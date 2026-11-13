package com.example.app.testing.dto;

import com.example.app.testing.entity.TestRecordStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TestingRecordResponse(
        Long id,
        String recordCode,
        Long transferItemId,
        String transferCode,
        String receiptCode,
        Long sampleId,
        String sampleCode,
        String sampleName,
        String quantity,
        String requestedParameters,
        String testMethod,
        LocalDate receivedDate,
        LocalDate dueDate,
        Long analystId,
        String analyst,
        String checkedBy,
        Instant analysisStart,
        Instant analysisEnd,
        String calibrationCurve,
        String calculationFormula,
        BigDecimal recoveryPercent,
        String qcChartNote,
        String analystNote,
        String reviewNote,
        TestRecordStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        Instant createdAt,
        List<EquipmentResponse> equipment,
        List<ReagentResponse> reagents,
        List<MeasurementResponse> measurements
) {}
