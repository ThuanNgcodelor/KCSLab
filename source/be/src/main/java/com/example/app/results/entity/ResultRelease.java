package com.example.app.results.entity;

import com.example.app.testing.entity.TestingRecord;
import com.example.app.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "result_releases")
public class ResultRelease {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "testing_record_id") private TestingRecord testingRecord;
    @Column(nullable = false) private int version;
    @Column(nullable = false, length = 30) private String status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id") private User createdBy;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected ResultRelease() {}
    public ResultRelease(TestingRecord testingRecord, User createdBy) {
        this.testingRecord = testingRecord; this.createdBy = createdBy;
        this.version = 1; this.status = "DRAFT"; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public Long getId() { return id; }
    public TestingRecord getTestingRecord() { return testingRecord; }
    public int getVersion() { return version; }
    public String getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
