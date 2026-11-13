package com.example.app.transfer.entity;

import com.example.app.intake.entity.Receipt;
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
@Table(name = "sample_transfers")
public class SampleTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String transferCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivered_by_user_id")
    private User deliveredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id")
    private User receivedBy;

    @Column(nullable = false, length = 120)
    private String receivingDepartment;

    @Column(nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false)
    private LocalDate resultsDueDate;

    @Column(nullable = false)
    private boolean returnToCustomer;

    private LocalDate customerReturnDate;

    @Column(columnDefinition = "TEXT")
    private String changesNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status;

    @Column(columnDefinition = "TEXT")
    private String receivingNote;

    @Column(columnDefinition = "TEXT")
    private String labOpinion;

    @Column(nullable = false)
    private boolean labRetain;

    private LocalDate labRetentionUntil;
    private Instant submittedAt;
    private Instant receivedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SampleTransferItem> items = new ArrayList<>();

    protected SampleTransfer() {}

    public SampleTransfer(String transferCode, Receipt receipt, User deliveredBy, String receivingDepartment,
                          LocalDate transferDate,
                          LocalDate resultsDueDate, boolean returnToCustomer, LocalDate customerReturnDate,
                          String changesNote) {
        this.transferCode = transferCode;
        this.receipt = receipt;
        this.deliveredBy = deliveredBy;
        this.receivingDepartment = receivingDepartment;
        this.transferDate = transferDate;
        this.resultsDueDate = resultsDueDate;
        this.returnToCustomer = returnToCustomer;
        this.customerReturnDate = customerReturnDate;
        this.changesNote = changesNote;
        this.status = TransferStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addItem(SampleTransferItem item) {
        items.add(item);
        item.attachTo(this);
    }

    public void replaceItems(List<SampleTransferItem> replacement) {
        items.clear();
        replacement.forEach(this::addItem);
    }

    public void updateDraft(String receivingDepartment, LocalDate transferDate, LocalDate resultsDueDate,
                            boolean returnToCustomer, LocalDate customerReturnDate, String changesNote) {
        this.receivingDepartment = receivingDepartment;
        this.transferDate = transferDate;
        this.resultsDueDate = resultsDueDate;
        this.returnToCustomer = returnToCustomer;
        this.customerReturnDate = customerReturnDate;
        this.changesNote = changesNote;
        this.updatedAt = Instant.now();
    }

    public void submit() {
        this.status = TransferStatus.PENDING;
        this.submittedAt = Instant.now();
        this.updatedAt = this.submittedAt;
    }

    public void decide(TransferStatus decision, User receiver, String receivingNote, String labOpinion,
                       boolean labRetain, LocalDate labRetentionUntil) {
        this.status = decision;
        this.receivedBy = receiver;
        this.receivingNote = receivingNote;
        this.labOpinion = labOpinion;
        this.labRetain = labRetain;
        this.labRetentionUntil = labRetentionUntil;
        this.receivedAt = Instant.now();
        this.updatedAt = this.receivedAt;
    }

    public Long getId() { return id; }
    public String getTransferCode() { return transferCode; }
    public Receipt getReceipt() { return receipt; }
    public User getDeliveredBy() { return deliveredBy; }
    public User getReceivedBy() { return receivedBy; }
    public String getReceivingDepartment() { return receivingDepartment; }
    public LocalDate getTransferDate() { return transferDate; }
    public LocalDate getResultsDueDate() { return resultsDueDate; }
    public boolean isReturnToCustomer() { return returnToCustomer; }
    public LocalDate getCustomerReturnDate() { return customerReturnDate; }
    public String getChangesNote() { return changesNote; }
    public TransferStatus getStatus() { return status; }
    public String getReceivingNote() { return receivingNote; }
    public String getLabOpinion() { return labOpinion; }
    public boolean isLabRetain() { return labRetain; }
    public LocalDate getLabRetentionUntil() { return labRetentionUntil; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<SampleTransferItem> getItems() { return items; }
}
