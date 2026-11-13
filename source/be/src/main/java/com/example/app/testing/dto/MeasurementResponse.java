package com.example.app.testing.dto;

import com.example.app.testing.entity.MeasurementType;

public record MeasurementResponse(
        Long id,
        MeasurementType type,
        Integer rowOrder,
        String replicate,
        String sampleAmount,
        String finalVolume,
        String dilutionFactor,
        String measuredConcentration,
        String resultPerReplicate,
        String result,
        String deviation
) {}
