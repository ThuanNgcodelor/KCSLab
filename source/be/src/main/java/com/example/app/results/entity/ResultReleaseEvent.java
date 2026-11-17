package com.example.app.results.entity;

import com.example.app.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/** Append-only history of a result dossier. */
@Entity
@Table(name = "result_release_events")
public class ResultReleaseEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "release_id", updatable = false)
    private ResultRelease release;
    @Column(nullable = false, length = 30, updatable = false) private String eventType;
    @Column(length = 30, updatable = false) private String fromStatus;
    @Column(length = 30, updatable = false) private String toStatus;
    @Column(length = 500, updatable = false) private String note;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_user_id", updatable = false)
    private User actor;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected ResultReleaseEvent() {}
    public ResultReleaseEvent(ResultRelease release, String eventType, ResultReleaseStatus from,
                              ResultReleaseStatus to, String note, User actor) {
        this.release = release; this.eventType = eventType;
        this.fromStatus = from == null ? null : from.name(); this.toStatus = to == null ? null : to.name();
        this.note = note; this.actor = actor; this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public User getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
}
