package com.example.app.transfer.dto;

public record TransferItemResponse(
        Long id,
        Long sampleId,
        String sampleName,
        String sampleCode,
        String weight,
        String quantity,
        String sampleCondition,
        String requestedParameters,
        String testMethod
) {}
