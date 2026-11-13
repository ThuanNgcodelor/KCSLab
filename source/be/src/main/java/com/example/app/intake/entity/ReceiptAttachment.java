package com.example.app.intake.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    @Column(nullable = false, length = 255) private String originalName;
    @Column(nullable = false, length = 120) private String contentType;
    @Column(nullable = false, length = 255) private String storageName;
    @Column(nullable = false) private long size;
    @Column(nullable = false, length = 64) private String checksum;
    @Column(nullable = false) private Instant uploadedAt;
    protected ReceiptAttachment() {}
    public ReceiptAttachment(String originalName, String contentType, String storageName, long size, String checksum) {
        this.originalName = originalName; this.contentType = contentType; this.storageName = storageName;
        this.size = size; this.checksum = checksum; this.uploadedAt = Instant.now();
    }
    void attachTo(Receipt receipt) { this.receipt = receipt; }
    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public String getChecksum() { return checksum; }
    public Instant getUploadedAt() { return uploadedAt; }
}
