package com.example.app.testing.entity;

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

@Entity
@Table(name = "testing_measurements")
public class TestingMeasurement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "testing_record_id")
    private TestingRecord testingRecord;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MeasurementType type;
    @Column(nullable = false) private Integer rowOrder;
    @Column(nullable = false, length = 40) private String replicate;
    @Column(length = 80) private String sampleAmount;
    @Column(length = 80) private String finalVolume;
    @Column(length = 80) private String dilutionFactor;
    @Column(length = 120) private String measuredConcentration;
    @Column(length = 120) private String resultPerReplicate;
    @Column(length = 120) private String result;
    @Column(length = 120) private String deviation;

    protected TestingMeasurement() {}
    public TestingMeasurement(MeasurementType type, Integer rowOrder, String replicate, String sampleAmount,
                              String finalVolume, String dilutionFactor, String measuredConcentration,
                              String resultPerReplicate, String result, String deviation) {
        this.type = type; this.rowOrder = rowOrder; this.replicate = replicate; this.sampleAmount = sampleAmount;
        this.finalVolume = finalVolume; this.dilutionFactor = dilutionFactor;
        this.measuredConcentration = measuredConcentration; this.resultPerReplicate = resultPerReplicate;
        this.result = result; this.deviation = deviation;
    }
    void attachTo(TestingRecord record) { this.testingRecord = record; }
    public Long getId() { return id; }
    public MeasurementType getType() { return type; }
    public Integer getRowOrder() { return rowOrder; }
    public String getReplicate() { return replicate; }
    public String getSampleAmount() { return sampleAmount; }
    public String getFinalVolume() { return finalVolume; }
    public String getDilutionFactor() { return dilutionFactor; }
    public String getMeasuredConcentration() { return measuredConcentration; }
    public String getResultPerReplicate() { return resultPerReplicate; }
    public String getResult() { return result; }
    public String getDeviation() { return deviation; }
}
