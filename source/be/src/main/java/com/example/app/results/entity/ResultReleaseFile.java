package com.example.app.results.entity;

import com.example.app.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "result_release_files")
public class ResultReleaseFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "release_id", updatable = false)
    private ResultRelease release;
    @Column(nullable = false, length = 255, updatable = false) private String originalName;
    @Column(nullable = false, length = 120, updatable = false) private String contentType;
    @Column(nullable = false, length = 10, updatable = false) private String extension;
    @Column(nullable = false, length = 80, updatable = false) private String storageName;
    @Column(nullable = false, updatable = false) private long size;
    @Column(nullable = false, length = 64, updatable = false) private String checksum;
    @Column(nullable = false) private boolean publicDownload;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "uploaded_by_user_id", updatable = false)
    private User uploadedBy;
    @Column(nullable = false, updatable = false) private Instant uploadedAt;
    private Instant removedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "removed_by_user_id") private User removedBy;

    protected ResultReleaseFile() {}
    public ResultReleaseFile(ResultRelease release, String originalName, String contentType, String extension,
                             String storageName, long size, String checksum, User uploadedBy) {
        this.release = release; this.originalName = originalName; this.contentType = contentType;
        this.extension = extension; this.storageName = storageName; this.size = size; this.checksum = checksum;
        this.uploadedBy = uploadedBy; this.uploadedAt = Instant.now();
    }

    public void remove(User actor) { this.removedAt = Instant.now(); this.removedBy = actor; this.publicDownload = false; }
    public void setPublicDownload(boolean value) { this.publicDownload = value; }
    public boolean isRemoved() { return removedAt != null; }

    public Long getId() { return id; }
    public ResultRelease getRelease() { return release; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public String getExtension() { return extension; }
    public String getStorageName() { return storageName; }
    public long getSize() { return size; }
    public String getChecksum() { return checksum; }
    public boolean isPublicDownload() { return publicDownload; }
    public User getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getRemovedAt() { return removedAt; }
}
