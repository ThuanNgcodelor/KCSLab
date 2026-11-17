package com.example.app.results.entity;

import com.example.app.testing.entity.TestingRecord;
import com.example.app.user.entity.User;
import jakarta.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Entity
@Table(name = "result_releases")
public class ResultRelease {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "testing_record_id") private TestingRecord testingRecord;
    @Column(nullable = false) private int version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ResultReleaseStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id") private User createdBy;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Column(length = 32) private String lookupCode;
    @Column(length = 40) private String resultNumber;
    private Instant submittedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submitted_by_user_id") private User submittedBy;
    private Instant decidedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decided_by_user_id") private User decidedBy;
    @Column(length = 500) private String decisionNote;
    private Instant publishedAt;
    private Instant revokedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revoked_by_user_id") private User revokedBy;
    @Column(length = 500) private String revokeReason;
    @Column(length = 120) private String qualityHeadName;
    @Column(length = 120) private String directorName;

    protected ResultRelease() {}
    public ResultRelease(TestingRecord testingRecord, User createdBy) { this(testingRecord, createdBy, 1); }
    public ResultRelease(TestingRecord testingRecord, User createdBy, int version) {
        this.testingRecord = testingRecord; this.createdBy = createdBy;
        this.version = version; this.status = ResultReleaseStatus.DRAFT;
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }

    public boolean isEditable() { return status == ResultReleaseStatus.DRAFT || status == ResultReleaseStatus.RETURNED; }

    public void submit(User actor) {
        require(isEditable(), "Chỉ hồ sơ nháp hoặc đã trả lại mới được gửi duyệt");
        this.status = ResultReleaseStatus.SUBMITTED; this.submittedBy = actor; this.submittedAt = Instant.now();
        this.decidedBy = null; this.decidedAt = null; this.decisionNote = null; touch();
    }
    public void giveBack(User actor, String note) {
        require(status == ResultReleaseStatus.SUBMITTED, "Hồ sơ chưa ở trạng thái chờ duyệt");
        this.status = ResultReleaseStatus.RETURNED; this.decidedBy = actor; this.decidedAt = Instant.now();
        this.decisionNote = note; touch();
    }
    public void publish(User actor, String lookupCode, String resultNumber) {
        require(status == ResultReleaseStatus.SUBMITTED, "Hồ sơ chưa ở trạng thái chờ duyệt");
        this.status = ResultReleaseStatus.PUBLISHED; this.decidedBy = actor; this.decidedAt = Instant.now();
        this.publishedAt = this.decidedAt; this.lookupCode = lookupCode; this.resultNumber = resultNumber; touch();
    }
    public void revoke(User actor, String reason) {
        require(status == ResultReleaseStatus.PUBLISHED, "Chỉ hồ sơ đã phát hành mới được thu hồi");
        this.status = ResultReleaseStatus.REVOKED; this.revokedBy = actor; this.revokedAt = Instant.now();
        this.revokeReason = reason; touch();
    }
    public void setSignatories(String qualityHeadName, String directorName) {
        this.qualityHeadName = qualityHeadName; this.directorName = directorName; touch();
    }
    public void touch() { this.updatedAt = Instant.now(); }

    private static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public Long getId() { return id; }
    public TestingRecord getTestingRecord() { return testingRecord; }
    public int getVersion() { return version; }
    public ResultReleaseStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getLookupCode() { return lookupCode; }
    public String getResultNumber() { return resultNumber; }
    public Instant getSubmittedAt() { return submittedAt; }
    public User getSubmittedBy() { return submittedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public User getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public User getRevokedBy() { return revokedBy; }
    public String getRevokeReason() { return revokeReason; }
    public String getQualityHeadName() { return qualityHeadName; }
    public String getDirectorName() { return directorName; }
}
