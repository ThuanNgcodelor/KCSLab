package com.example.app.intake.entity;

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

import com.example.app.user.entity.User;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "samples")
public class Sample {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 80) private String sampleCode;
    @Column(nullable = false, length = 20) private String sourceCode;
    @Column(nullable = false, length = 80) private String productType;
    @Column(nullable = false) private LocalDate receivedDate;
    @Column(nullable = false) private Integer dailySequence;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 80) private String quantity;
    @Column(length = 80) private String packaging;
    @Column(name = "sample_condition", length = 80) private String sampleCondition;
    @Column(columnDefinition = "TEXT") private String requestedParameters;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ReceiptStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "receipt_id") private Receipt receipt;
    @Enumerated(EnumType.STRING) @Column(name = "coding_status", nullable = false, length = 30)
    private SampleCodingStatus codingStatus = SampleCodingStatus.PENDING_CODING;
    @Column(name = "coding_legacy", nullable = false) private boolean codingLegacy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coded_by_user_id") private User codedBy;
    private Instant codedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "verified_by_user_id") private User verifiedBy;
    private Instant verifiedAt;

    protected Sample() {}
    public Sample(String sampleCode, String sourceCode, String productType, LocalDate receivedDate, Integer dailySequence,
                   String name, String quantity, String packaging, String condition, String requestedParameters) {
        this.sampleCode = sampleCode; this.sourceCode = sourceCode; this.productType = productType;
        this.receivedDate = receivedDate; this.dailySequence = dailySequence; this.name = name; this.quantity = quantity;
        this.packaging = packaging; this.sampleCondition = condition; this.requestedParameters = requestedParameters;
        this.status = ReceiptStatus.RECEIVED;
    }
    void attachTo(Receipt receipt) { this.receipt = receipt; }
    public void markTransferPending() { this.status = ReceiptStatus.TRANSFER_PENDING; }
    public void markTransferred() { this.status = ReceiptStatus.TRANSFERRED; }
    public void markTransferRejected() { this.status = ReceiptStatus.RECEIVED; }
    public void markInTesting() { this.status = ReceiptStatus.IN_TESTING; }
    public void markAwaitingReview() { this.status = ReceiptStatus.AWAITING_REVIEW; }
    public void markTestReviewed() { this.status = ReceiptStatus.TEST_REVIEWED; }
    public void submitCoding(User coder) {
        this.codingStatus = SampleCodingStatus.PENDING_VERIFICATION;
        this.codedBy = coder; this.codedAt = Instant.now();
        this.verifiedBy = null; this.verifiedAt = null;
    }
    public void verifyCoding(User verifier) {
        this.codingStatus = SampleCodingStatus.VERIFIED;
        this.verifiedBy = verifier; this.verifiedAt = Instant.now();
    }
    public void rejectCoding() {
        this.codingStatus = SampleCodingStatus.CORRECTION_REQUIRED;
        this.verifiedBy = null; this.verifiedAt = null;
    }
    /** Replaces the code after a rejected verification. The caller records the old code in an event. */
    public void applyCorrection(String sampleCode, String sourceCode, String productType, Integer dailySequence) {
        this.sampleCode = sampleCode; this.sourceCode = sourceCode; this.productType = productType;
        this.dailySequence = dailySequence;
        this.codingStatus = SampleCodingStatus.PENDING_CODING;
        this.codedBy = null; this.codedAt = null; this.verifiedBy = null; this.verifiedAt = null;
    }
    /** Marks a sample coded before the gate existed (V7 migration semantics). */
    public Sample markLegacyVerified() { this.codingStatus = SampleCodingStatus.VERIFIED; this.codingLegacy = true; return this; }
    public boolean isCodingVerified() { return codingStatus == SampleCodingStatus.VERIFIED; }
    public Long getId() { return id; }
    public String getSampleCode() { return sampleCode; }
    public String getSourceCode() { return sourceCode; }
    public String getProductType() { return productType; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public Integer getDailySequence() { return dailySequence; }
    public String getName() { return name; }
    public String getQuantity() { return quantity; }
    public String getPackaging() { return packaging; }
    public String getCondition() { return sampleCondition; }
    public String getRequestedParameters() { return requestedParameters; }
    public ReceiptStatus getStatus() { return status; }
    public Receipt getReceipt() { return receipt; }
    public SampleCodingStatus getCodingStatus() { return codingStatus; }
    public boolean isCodingLegacy() { return codingLegacy; }
    public User getCodedBy() { return codedBy; }
    public Instant getCodedAt() { return codedAt; }
    public User getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
