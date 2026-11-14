package com.example.app.intake.entity;

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
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipts")
public class Receipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String receiptCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id")
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "received_by_user_id")
    private User receivedBy;
    @Column(nullable = false, length = 160) private String deliveredBy;
    @Column(length = 80) private String requestNumber;
    @Column(nullable = false) private Instant receivedAt;
    @Column(length = 160) private String sampledBy;
    private Instant sampledAt;
    @Column(length = 255) private String growingArea;
    @Column(length = 255) private String packingFacility;
    @Column(length = 80) private String vehicleNumber;
    @Column(length = 80) private String trailerNumber;
    @Column(length = 80) private String containerNumber;
    @Column(nullable = false, length = 160) private String sampleName;
    @Column(length = 80) private String quantity;
    @Column(length = 80) private String packaging;
    @Column(length = 80) private String sealNumber;
    @Column(nullable = false) private boolean sealIntact;
    @Enumerated(EnumType.STRING) @Column(length = 20) private SealStatus sealStatus;
    @Column(nullable = false, length = 80) private String sampleCondition;
    @Column(columnDefinition = "TEXT") private String conditionDetail;
    @Column(columnDefinition = "TEXT") private String transportInfo;
    private Double receivedTemperature;
    @Column(length = 80) private String storageCondition;
    @Column(columnDefinition = "TEXT") private String requestedParameters;
    private LocalDate expectedDate;
    @Column(columnDefinition = "TEXT") private String specialRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AcceptanceDecision acceptanceDecision;
    @Column(columnDefinition = "TEXT") private String acceptanceReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ReceiptStatus status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sample> samples = new ArrayList<>();
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceiptAttachment> attachments = new ArrayList<>();

    protected Receipt() {}

    public Receipt(String receiptCode, Customer customer, User receivedBy, String deliveredBy, Instant receivedAt,
                   String sampleName, String quantity, String packaging, String sealNumber, boolean sealIntact,
                   String sampleCondition, String conditionDetail, String transportInfo, Double receivedTemperature,
                   String storageCondition, String requestedParameters, LocalDate expectedDate, String specialRequest,
                   AcceptanceDecision decision, String acceptanceReason) {
        this(receiptCode, customer, receivedBy, deliveredBy, receivedAt, null, null, null, null, null, null, null,
                sampleName, quantity, packaging, sealNumber, sealIntact, sampleCondition, conditionDetail,
                transportInfo, receivedTemperature, storageCondition, requestedParameters, expectedDate,
                specialRequest, decision, acceptanceReason);
    }

    public Receipt(String receiptCode, Customer customer, User receivedBy, String deliveredBy, Instant receivedAt,
                   String sampledBy, Instant sampledAt, String growingArea, String packingFacility,
                   String vehicleNumber, String trailerNumber, String containerNumber,
                   String sampleName, String quantity, String packaging, String sealNumber, boolean sealIntact,
                   String sampleCondition, String conditionDetail, String transportInfo, Double receivedTemperature,
                   String storageCondition, String requestedParameters, LocalDate expectedDate, String specialRequest,
                   AcceptanceDecision decision, String acceptanceReason) {
        this.receiptCode = receiptCode; this.customer = customer; this.receivedBy = receivedBy;
        this.deliveredBy = deliveredBy; this.receivedAt = receivedAt; this.sampleName = sampleName;
        this.sampledBy = sampledBy; this.sampledAt = sampledAt; this.growingArea = growingArea;
        this.packingFacility = packingFacility; this.vehicleNumber = vehicleNumber;
        this.trailerNumber = trailerNumber; this.containerNumber = containerNumber;
        this.quantity = quantity; this.packaging = packaging; this.sealNumber = sealNumber; this.sealIntact = sealIntact;
        this.sampleCondition = sampleCondition; this.conditionDetail = conditionDetail; this.transportInfo = transportInfo;
        this.receivedTemperature = receivedTemperature; this.storageCondition = storageCondition;
        this.requestedParameters = requestedParameters; this.expectedDate = expectedDate; this.specialRequest = specialRequest;
        this.acceptanceDecision = decision; this.acceptanceReason = acceptanceReason;
        this.status = decision == AcceptanceDecision.ACCEPTED_WITH_CONDITION ? ReceiptStatus.ACCEPTED_WITH_CONDITION
                : decision == AcceptanceDecision.REJECTED ? ReceiptStatus.REJECTED : ReceiptStatus.RECEIVED;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }

    public void addSample(Sample sample) { samples.add(sample); sample.attachTo(this); }
    public void addAttachment(ReceiptAttachment attachment) { attachments.add(attachment); attachment.attachTo(this); }
    public void markTransferred() { this.status = ReceiptStatus.TRANSFERRED; this.updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getReceiptCode() { return receiptCode; }
    public Customer getCustomer() { return customer; }
    public User getReceivedBy() { return receivedBy; }
    public String getDeliveredBy() { return deliveredBy; }
    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getSampledBy() { return sampledBy; }
    public Instant getSampledAt() { return sampledAt; }
    public String getGrowingArea() { return growingArea; }
    public String getPackingFacility() { return packingFacility; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getTrailerNumber() { return trailerNumber; }
    public String getContainerNumber() { return containerNumber; }
    public String getSampleName() { return sampleName; }
    public String getQuantity() { return quantity; }
    public String getPackaging() { return packaging; }
    public String getSealNumber() { return sealNumber; }
    public boolean isSealIntact() { return sealIntact; }
    public SealStatus getSealStatus() { return sealStatus == null ? (sealIntact ? SealStatus.INTACT : SealStatus.OPENED) : sealStatus; }
    public void setSealStatus(SealStatus sealStatus) { this.sealStatus = sealStatus; this.sealIntact = sealStatus == SealStatus.INTACT; }
    public String getSampleCondition() { return sampleCondition; }
    public String getConditionDetail() { return conditionDetail; }
    public String getTransportInfo() { return transportInfo; }
    public Double getReceivedTemperature() { return receivedTemperature; }
    public String getStorageCondition() { return storageCondition; }
    public String getRequestedParameters() { return requestedParameters; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public String getSpecialRequest() { return specialRequest; }
    public AcceptanceDecision getAcceptanceDecision() { return acceptanceDecision; }
    public String getAcceptanceReason() { return acceptanceReason; }
    public ReceiptStatus getStatus() { return status; }
    public List<Sample> getSamples() { return samples; }
    public List<ReceiptAttachment> getAttachments() { return attachments; }
}
