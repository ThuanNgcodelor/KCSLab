package com.example.app.testing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "testing_reagents")
public class TestingReagent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "testing_record_id")
    private TestingRecord testingRecord;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 120) private String code;
    @Column(length = 120) private String origin;
    private LocalDate preparationDate;
    private LocalDate expiryDate;
    @Column(length = 160) private String storageCondition;
    @Column(length = 120) private String concentration;

    protected TestingReagent() {}
    public TestingReagent(String name, String code, String origin, LocalDate preparationDate,
                          LocalDate expiryDate, String storageCondition, String concentration) {
        this.name = name; this.code = code; this.origin = origin; this.preparationDate = preparationDate;
        this.expiryDate = expiryDate; this.storageCondition = storageCondition; this.concentration = concentration;
    }
    void attachTo(TestingRecord record) { this.testingRecord = record; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getOrigin() { return origin; }
    public LocalDate getPreparationDate() { return preparationDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getStorageCondition() { return storageCondition; }
    public String getConcentration() { return concentration; }
}
