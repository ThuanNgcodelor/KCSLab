package com.example.app.intake.dto;

import com.example.app.intake.entity.Sample;
import com.example.app.intake.entity.SampleCodingStatus;

import java.time.Instant;

public record SampleCodingResponse(Long sampleId, String sampleCode, SampleCodingStatus codingStatus,
        boolean legacy, String codedBy, Instant codedAt, String verifiedBy, Instant verifiedAt) {
    public static SampleCodingResponse from(Sample sample) {
        return new SampleCodingResponse(sample.getId(), sample.getSampleCode(), sample.getCodingStatus(),
                sample.isCodingLegacy(),
                sample.getCodedBy() == null ? null : sample.getCodedBy().getDisplayName(), sample.getCodedAt(),
                sample.getVerifiedBy() == null ? null : sample.getVerifiedBy().getDisplayName(),
                sample.getVerifiedAt());
    }
}
