package com.example.app.testing.entity;

import com.example.app.transfer.entity.SampleTransferItem;
import com.example.app.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "testing_records")
public class TestingRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40) private String recordCode;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "transfer_item_id", unique = true)
    private SampleTransferItem transferItem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "analyst_user_id")
    private User analyst;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "checked_by_user_id")
    private User checkedBy;
    private Instant analysisStart;
    private Instant analysisEnd;
    @Column(columnDefinition = "TEXT") private String calibrationCurve;
    @Column(columnDefinition = "TEXT") private String calculationFormula;
    @Column(precision = 12, scale = 4) private BigDecimal recoveryPercent;
    @Column(columnDefinition = "TEXT") private String qcChartNote;
    @Column(columnDefinition = "TEXT") private String analystNote;
    @Column(columnDefinition = "TEXT") private String reviewNote;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TestRecordStatus status;
    private Instant submittedAt;
    private Instant reviewedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "testingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestingEquipment> equipment = new ArrayList<>();
    @OneToMany(mappedBy = "testingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestingReagent> reagents = new ArrayList<>();
    @OneToMany(mappedBy = "testingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestingMeasurement> measurements = new ArrayList<>();

    protected TestingRecord() {}

    public TestingRecord(String recordCode, SampleTransferItem transferItem, User analyst) {
        this.recordCode = recordCode;
        this.transferItem = transferItem;
        this.analyst = analyst;
        this.status = TestRecordStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateContent(Instant analysisStart, Instant analysisEnd, String calibrationCurve,
                              String calculationFormula, BigDecimal recoveryPercent, String qcChartNote,
                              String analystNote) {
        this.analysisStart = analysisStart;
        this.analysisEnd = analysisEnd;
        this.calibrationCurve = calibrationCurve;
        this.calculationFormula = calculationFormula;
        this.recoveryPercent = recoveryPercent;
        this.qcChartNote = qcChartNote;
        this.analystNote = analystNote;
        this.updatedAt = Instant.now();
    }

    public void replaceDetails(List<TestingEquipment> newEquipment, List<TestingReagent> newReagents,
                               List<TestingMeasurement> newMeasurements) {
        equipment.clear();
        reagents.clear();
        measurements.clear();
        newEquipment.forEach(this::addEquipment);
        newReagents.forEach(this::addReagent);
        newMeasurements.forEach(this::addMeasurement);
        this.updatedAt = Instant.now();
    }

    public void submit() {
        status = TestRecordStatus.SUBMITTED;
        submittedAt = Instant.now();
        updatedAt = submittedAt;
    }

    public void review(TestRecordStatus decision, User checker, String note) {
        status = decision;
        checkedBy = checker;
        reviewNote = note;
        reviewedAt = Instant.now();
        updatedAt = reviewedAt;
    }

    private void addEquipment(TestingEquipment item) { equipment.add(item); item.attachTo(this); }
    private void addReagent(TestingReagent item) { reagents.add(item); item.attachTo(this); }
    private void addMeasurement(TestingMeasurement item) { measurements.add(item); item.attachTo(this); }

    public Long getId() { return id; }
    public String getRecordCode() { return recordCode; }
    public SampleTransferItem getTransferItem() { return transferItem; }
    public User getAnalyst() { return analyst; }
    public User getCheckedBy() { return checkedBy; }
    public Instant getAnalysisStart() { return analysisStart; }
    public Instant getAnalysisEnd() { return analysisEnd; }
    public String getCalibrationCurve() { return calibrationCurve; }
    public String getCalculationFormula() { return calculationFormula; }
    public BigDecimal getRecoveryPercent() { return recoveryPercent; }
    public String getQcChartNote() { return qcChartNote; }
    public String getAnalystNote() { return analystNote; }
    public String getReviewNote() { return reviewNote; }
    public TestRecordStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TestingEquipment> getEquipment() { return equipment; }
    public List<TestingReagent> getReagents() { return reagents; }
    public List<TestingMeasurement> getMeasurements() { return measurements; }
}
