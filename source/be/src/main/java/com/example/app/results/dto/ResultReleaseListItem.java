package com.example.app.results.dto;

import com.example.app.results.entity.ResultRelease;

import java.time.Instant;

public record ResultReleaseListItem(Long id, String testingRecordCode, String sampleCode, int version,
        String status, String resultNumber, String createdByName, Instant createdAt, Instant updatedAt) {
    public static ResultReleaseListItem from(ResultRelease release) {
        return new ResultReleaseListItem(release.getId(), release.getTestingRecord().getRecordCode(),
                release.getTestingRecord().getTransferItem().getSampleCode(), release.getVersion(),
                release.getStatus().name(), release.getResultNumber(), release.getCreatedBy().getDisplayName(),
                release.getCreatedAt(), release.getUpdatedAt());
    }
}
