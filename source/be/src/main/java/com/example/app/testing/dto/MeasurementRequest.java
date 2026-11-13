package com.example.app.testing.dto;

import com.example.app.testing.entity.MeasurementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MeasurementRequest(
        @NotNull MeasurementType type,
        @NotBlank @Size(max = 40) String replicate,
        @Size(max = 80) String sampleAmount,
        @Size(max = 80) String finalVolume,
        @Size(max = 80) String dilutionFactor,
        @Size(max = 120) String measuredConcentration,
        @Size(max = 120) String resultPerReplicate,
        @Size(max = 120) String result,
        @Size(max = 120) String deviation
) {}
