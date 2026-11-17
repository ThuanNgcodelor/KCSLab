package com.example.app.results.dto;

import com.example.app.results.entity.ResultRelease;
import com.example.app.results.entity.ResultReleaseEvent;
import com.example.app.results.entity.ResultReleaseFile;
import com.example.app.results.entity.ResultReleaseStatus;
import com.example.app.user.entity.User;

import java.time.Instant;
import java.util.List;

public record ResultReleaseDetailResponse(Long id, Long testingRecordId, String testingRecordCode, String sampleCode,
        int version, String status, String resultNumber, String lookupCode,
        Long createdByUserId, String createdByName, Instant createdAt, Instant updatedAt,
        Instant submittedAt, String submittedByName, Instant decidedAt, String decidedByName, String decisionNote,
        Instant publishedAt, Instant revokedAt, String revokedByName, String revokeReason, List<FileResponse> files) {

    public record FileResponse(Long id, String originalName, String contentType, long size, String checksum,
            boolean publicDownload, String uploadedByName, Instant uploadedAt, String contentUrl) {
        public static FileResponse from(ResultReleaseFile file) {
            return new FileResponse(file.getId(), file.getOriginalName(), file.getContentType(), file.getSize(),
                    file.getChecksum(), file.isPublicDownload(), file.getUploadedBy().getDisplayName(),
                    file.getUploadedAt(), "/api/v1/result-releases/%d/files/%d/content".formatted(
                            file.getRelease().getId(), file.getId()));
        }
    }

    public record EventResponse(Long id, String eventType, String fromStatus, String toStatus, String note,
            String actor, Instant createdAt) {
        public static EventResponse from(ResultReleaseEvent event) {
            return new EventResponse(event.getId(), event.getEventType(), event.getFromStatus(), event.getToStatus(),
                    event.getNote(), event.getActor().getDisplayName(), event.getCreatedAt());
        }
    }

    public static ResultReleaseDetailResponse from(ResultRelease release, List<ResultReleaseFile> files) {
        var record = release.getTestingRecord();
        boolean issued = release.getStatus() == ResultReleaseStatus.PUBLISHED
                || release.getStatus() == ResultReleaseStatus.REVOKED;
        return new ResultReleaseDetailResponse(release.getId(), record.getId(), record.getRecordCode(),
                record.getTransferItem().getSampleCode(), release.getVersion(), release.getStatus().name(),
                release.getResultNumber(), issued ? release.getLookupCode() : null,
                release.getCreatedBy().getId(), release.getCreatedBy().getDisplayName(), release.getCreatedAt(),
                release.getUpdatedAt(), release.getSubmittedAt(), name(release.getSubmittedBy()),
                release.getDecidedAt(), name(release.getDecidedBy()), release.getDecisionNote(),
                release.getPublishedAt(), release.getRevokedAt(), name(release.getRevokedBy()),
                release.getRevokeReason(), files.stream().map(FileResponse::from).toList());
    }

    private static String name(User user) { return user == null ? null : user.getDisplayName(); }
}
