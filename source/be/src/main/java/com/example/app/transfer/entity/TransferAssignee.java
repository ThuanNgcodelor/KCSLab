package com.example.app.transfer.entity;

import com.example.app.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/** A technician tagged on a transfer to run its tests. */
@Entity
@Table(name = "transfer_assignees")
public class TransferAssignee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transfer_id") private SampleTransfer transfer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by_user_id") private User assignedBy;
    @Column(nullable = false) private Instant assignedAt;

    protected TransferAssignee() {}
    public TransferAssignee(SampleTransfer transfer, User user, User assignedBy) {
        this.transfer = transfer; this.user = user; this.assignedBy = assignedBy; this.assignedAt = Instant.now();
    }
    public Long getId() { return id; }
    public SampleTransfer getTransfer() { return transfer; }
    public User getUser() { return user; }
    public User getAssignedBy() { return assignedBy; }
    public Instant getAssignedAt() { return assignedAt; }
}
