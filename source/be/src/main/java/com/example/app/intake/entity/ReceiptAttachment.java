package com.example.app.intake.entity;

import com.example.app.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "receipt_attachments")
public class ReceiptAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receipt_id") private Receipt receipt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ReceiptAttachmentCategory category;
    @Column(length = 255) private String description;
    @Column(nullable = false, length = 255) private String originalName;
    @Column(nullable = false, length = 120) private String contentType;
    @Column(nullable = false, length = 255) private String storageName;
    @Column(nullable = false) private long size;
    @Column(nullable = false, length = 64) private String checksum;
    private Instant capturedAt;
    @Column(nullable = false) private Instant uploadedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploaded_by_user_id") private User uploadedBy;
    protected ReceiptAttachment() {}
    public ReceiptAttachment(ReceiptAttachmentCategory category, String description, String originalName,
                             String contentType, String storageName, long size, String checksum,
                             Instant capturedAt, User uploadedBy) {
        this.category = category;
        this.description = description;
        this.originalName = originalName; this.contentType = contentType; this.storageName = storageName;
        this.size = size; this.checksum = checksum; this.capturedAt = capturedAt;
        this.uploadedBy = uploadedBy; this.uploadedAt = Instant.now();
    }
    void attachTo(Receipt receipt) { this.receipt = receipt; }
    public Long getId() { return id; }
    public Receipt getReceipt() { return receipt; }
    public ReceiptAttachmentCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public String getStorageName() { return storageName; }
    public long getSize() { return size; }
    public String getChecksum() { return checksum; }
    public Instant getCapturedAt() { return capturedAt; }
    public Instant getUploadedAt() { return uploadedAt; }
    public User getUploadedBy() { return uploadedBy; }
}
