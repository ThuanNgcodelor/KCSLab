package com.example.app.transfer.receiving;

import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.entity.ReceiptAttachmentCategory;

import java.time.Instant;

public record ReceivingEvidenceResponse(Long id, ReceiptAttachmentCategory category, String description,
                                       String originalName, String contentType, long size,
                                       Instant capturedAt, Instant uploadedAt, String uploadedByName,
                                       String contentUrl) {
    public static ReceivingEvidenceResponse from(Long transferId, ReceiptAttachmentResponse attachment) {
        return new ReceivingEvidenceResponse(attachment.id(), attachment.category(), attachment.description(),
                attachment.originalName(), attachment.contentType(), attachment.size(), attachment.capturedAt(),
                attachment.uploadedAt(), attachment.uploadedByName(),
                "/api/v1/receiving/transfers/" + transferId + "/evidence/" + attachment.id() + "/content");
    }
}
