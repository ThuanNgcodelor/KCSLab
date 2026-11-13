package com.example.app.intake.dto;

import com.example.app.intake.entity.ReceiptStatus;

public record SampleResponse(String sampleCode, String sourceCode, String productType, String name, ReceiptStatus status) {}
