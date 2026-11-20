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
        Instant publishedAt, Instant revokedAt, String revokedByName, String revokeReason, List<FileResponse> files,
        List<ItemResponse> items, String qualityHeadName, String directorName,
        Reference reference, List<ItemResponse> suggestedItems, String documentNumber, List<PhotoResponse> samplePhotos) {

    /** Sample photo taken at intake (Phiếu 3); printed on the "Hình ảnh mẫu" page of the KQKN. */
    public record PhotoResponse(Long id, String originalName, String category, String caption, Instant capturedAt) {}

    public record Measurement(String type, String replicate, String resultPerReplicate, String result, String deviation) {}
    public record Reference(Long testingRecordId, String recordCode, Instant analysisStart, Instant analysisEnd,
            java.math.BigDecimal recoveryPercent, String requestedParameters, String testMethod, List<Measurement> measurements) {}

    public record ItemResponse(Long id, int rowOrder, String parameter, String unit, String method, String result, String note) {}

    public record FileResponse(Long id, String originalName, String contentType, long size, String checksum,
            boolean publicDownload, String uploadedByName, Instant uploadedAt, String contentUrl, String kind) {
        public static FileResponse from(ResultReleaseFile file) {
            return new FileResponse(file.getId(), file.getOriginalName(), file.getContentType(), file.getSize(),
                    file.getChecksum(), file.isPublicDownload(), file.getUploadedBy().getDisplayName(),
                    file.getUploadedAt(), "/api/v1/result-releases/%d/files/%d/content".formatted(
                            file.getRelease().getId(), file.getId()), file.getKind());
        }
    }

    public record EventResponse(Long id, String eventType, String fromStatus, String toStatus, String note,
            String actor, Instant createdAt) {
        public static EventResponse from(ResultReleaseEvent event) {
            return new EventResponse(event.getId(), event.getEventType(), event.getFromStatus(), event.getToStatus(),
                    event.getNote(), event.getActor().getDisplayName(), event.getCreatedAt());
        }
    }

    public static ResultReleaseDetailResponse from(ResultRelease release, List<ResultReleaseFile> files,
            List<com.example.app.results.entity.ResultReleaseItem> rows, String defaultQualityHead, String defaultDirector,
            Reference reference, List<ItemResponse> suggestedItems, List<PhotoResponse> samplePhotos) {
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
                release.getRevokeReason(), files.stream().map(FileResponse::from).toList(),
                rows.stream().map(row -> new ItemResponse(row.getId(), row.getRowOrder(), row.getParameterName(), row.getUnit(),
                        row.getMethod(), row.getResultValue(), row.getNote())).toList(),
                blank(release.getQualityHeadName()) ? defaultQualityHead : release.getQualityHeadName(),
                blank(release.getDirectorName()) ? defaultDirector : release.getDirectorName(), reference, suggestedItems,
                release.getDocumentNumber(), samplePhotos);
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String name(User user) { return user == null ? null : user.getDisplayName(); }
}
