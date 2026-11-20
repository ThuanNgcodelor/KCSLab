package com.example.app.testing.method;

import jakarta.persistence.*;

import java.time.Instant;

/** Catalog entry for a test method code such as {@code CFC-TN-16-2026}. */
@Entity
@Table(name = "test_methods")
public class TestMethod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String code;
    @Column(length = 255) private String name;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected TestMethod() {}
    public TestMethod(String code, String name, boolean active) {
        this.code = code; this.name = name; this.active = active;
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public void update(String code, String name, boolean active) {
        this.code = code; this.name = name; this.active = active; this.updatedAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
