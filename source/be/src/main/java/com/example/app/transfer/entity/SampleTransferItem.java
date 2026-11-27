package com.example.app.transfer.entity;

import com.example.app.intake.entity.Sample;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sample_transfer_items")
public class SampleTransferItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id")
    private SampleTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sample_id")
    private Sample sample;

    @Column(nullable = false, length = 160)
    private String sampleName;

    @Column(nullable = false, length = 80)
    private String sampleCode;

    @Column(length = 80)
    private String weight;

    @Column(length = 80)
    private String quantity;

    @Column(length = 255)
    private String sampleCondition;

    @Column(columnDefinition = "TEXT")
    private String requestedParameters;

    @Column(nullable = false, length = 255)
    private String testMethod;

    protected SampleTransferItem() {}

    public SampleTransferItem(Sample sample, String weight, String quantity, String sampleCondition,
                              String requestedParameters, String testMethod) {
        this.sample = sample;
        this.sampleName = sample.getName();
        this.sampleCode = sample.getSampleCode();
        this.weight = weight;
        this.quantity = quantity;
        this.sampleCondition = sampleCondition;
        this.requestedParameters = requestedParameters;
        this.testMethod = testMethod;
    }

    void attachTo(SampleTransfer transfer) { this.transfer = transfer; }

    public Long getId() { return id; }
    public SampleTransfer getTransfer() { return transfer; }
    public Sample getSample() { return sample; }
    /**
     * Brings the Phiếu 4 snapshot in line with a corrected sample. The name always follows; quantity, weight,
     * condition and parameters follow only when the transfer row still equals the old sample value, so values that
     * were edited on the transfer itself are kept.
     */
    public void syncFromSample(Sample sample, String oldQuantity, String oldWeight, String oldCondition, String oldParameters) {
        this.sampleName = sample.getName();
        if (java.util.Objects.equals(quantity, oldQuantity)) this.quantity = sample.getQuantity();
        if (java.util.Objects.equals(weight, oldWeight)) this.weight = sample.getWeight();
        if (java.util.Objects.equals(sampleCondition, oldCondition)) this.sampleCondition = sample.getCondition();
        if (java.util.Objects.equals(requestedParameters, oldParameters)) this.requestedParameters = sample.getRequestedParameters();
    }
    public String getSampleName() { return sampleName; }
    public String getSampleCode() { return sampleCode; }
    public String getWeight() { return weight; }
    public String getQuantity() { return quantity; }
    public String getSampleCondition() { return sampleCondition; }
    public String getRequestedParameters() { return requestedParameters; }
    public String getTestMethod() { return testMethod; }
}
