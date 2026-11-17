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

/** Append-only history of coding actions. Rows are never updated or deleted. */
@Entity
@Table(name = "sample_code_events")
public class SampleCodeEvent {
    public enum Type { SUBMITTED, VERIFIED, REJECTED, CORRECTED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sample_id", updatable = false)
    private Sample sample;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false) private Type eventType;
    @Column(length = 80, updatable = false) private String oldCode;
    @Column(length = 80, updatable = false) private String newCode;
    @Column(length = 500, updatable = false) private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_user_id", updatable = false)
    private User actor;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected SampleCodeEvent() {}

    public SampleCodeEvent(Sample sample, Type eventType, String oldCode, String newCode, String reason, User actor) {
        this.sample = sample; this.eventType = eventType; this.oldCode = oldCode; this.newCode = newCode;
        this.reason = reason; this.actor = actor; this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Sample getSample() { return sample; }
    public Type getEventType() { return eventType; }
    public String getOldCode() { return oldCode; }
    public String getNewCode() { return newCode; }
    public String getReason() { return reason; }
    public User getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
