package com.example.app.intake.entity;

import com.example.app.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/** Append-only history of edits made to a receipt after it was saved. */
@Entity
@Table(name = "receipt_edit_logs")
public class ReceiptEditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receipt_id") private Receipt receipt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "edited_by_user_id") private User editedBy;
    @Column(nullable = false) private Instant editedAt;
    @Column(length = 500) private String reason;
    @Column(nullable = false) private boolean afterTransfer;
    @Column(nullable = false, columnDefinition = "TEXT") private String summary;

    protected ReceiptEditLog() {}
    public ReceiptEditLog(Receipt receipt, User editedBy, String reason, boolean afterTransfer, String summary) {
        this.receipt = receipt; this.editedBy = editedBy; this.reason = reason; this.afterTransfer = afterTransfer;
        this.summary = summary; this.editedAt = Instant.now();
    }
    public Long getId() { return id; }
    public User getEditedBy() { return editedBy; }
    public Instant getEditedAt() { return editedAt; }
    public String getReason() { return reason; }
    public boolean isAfterTransfer() { return afterTransfer; }
    public String getSummary() { return summary; }
}
