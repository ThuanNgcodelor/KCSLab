package com.example.app.intake.entity;

/** Coding verification state of a sample (CFC-TN-14). Independent of {@link ReceiptStatus}. */
public enum SampleCodingStatus {
    PENDING_CODING,
    PENDING_VERIFICATION,
    VERIFIED,
    CORRECTION_REQUIRED
}
