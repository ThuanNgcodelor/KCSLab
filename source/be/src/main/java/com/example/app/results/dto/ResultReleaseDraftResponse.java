package com.example.app.results.dto;

import com.example.app.results.entity.ResultRelease;
import java.time.Instant;

public record ResultReleaseDraftResponse(Long id, Long testingRecordId, String testingRecordCode,
        int version, String status, Long createdByUserId, String createdByName, Instant createdAt) {
    public static ResultReleaseDraftResponse from(ResultRelease release) {
        return new ResultReleaseDraftResponse(release.getId(), release.getTestingRecord().getId(),
                release.getTestingRecord().getRecordCode(), release.getVersion(), release.getStatus().name(),
                release.getCreatedBy().getId(), release.getCreatedBy().getDisplayName(), release.getCreatedAt());
    }
}
