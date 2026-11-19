package com.example.app.sampling.entity;

import com.example.app.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_requests")
public class TestRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 80) private String requestNumber;
    @Column(nullable = false) private LocalDate requestDate;
    @Column(nullable = false, length = 200) private String customerName;
    @Column(length = 40) private String taxCode;
    @Column(length = 160) private String contactPerson;
    @Column(length = 500) private String customerAddress;
    @Column(nullable = false, length = 30) private String customerPhone;
    @Column(length = 200) private String customerEmail;
    @Column(nullable = false, length = 200) private String sampleName;
    @Column(length = 100) private String lotCode;
    @Column(length = 120) private String variety;
    @Column(length = 255) private String origin;
    private LocalDate harvestDate;
    private LocalDate packingDate;
    @Column(nullable = false, length = 30) private String samplingMode;
    @Column(length = 500) private String purpose;
    private LocalDate expectedDate;
    @Column(length = 120) private String resultDeliveryMethod;
    @Column(columnDefinition = "TEXT") private String serviceConditions;
    @Column(length = 120) private String delayNotice;
    @Column(nullable = false, length = 20) private String status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_user_id") private User createdBy;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "testRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestRequestItem> items = new ArrayList<>();

    protected TestRequest() {}
    public TestRequest(String requestNumber, User createdBy) {
        this.requestNumber = requestNumber; this.createdBy = createdBy; this.status = "DRAFT";
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public void update(LocalDate requestDate, String customerName, String taxCode, String contactPerson,
                       String customerAddress, String customerPhone, String customerEmail, String sampleName,
                       String lotCode, String variety, String origin, LocalDate harvestDate, LocalDate packingDate,
                       String samplingMode, String purpose, LocalDate expectedDate, String resultDeliveryMethod,
                       String serviceConditions, String delayNotice, boolean completed) {
        this.requestDate=requestDate; this.customerName=customerName; this.taxCode=taxCode; this.contactPerson=contactPerson;
        this.customerAddress=customerAddress; this.customerPhone=customerPhone; this.customerEmail=customerEmail;
        this.sampleName=sampleName; this.lotCode=lotCode; this.variety=variety; this.origin=origin;
        this.harvestDate=harvestDate; this.packingDate=packingDate; this.samplingMode=samplingMode;
        this.purpose=purpose; this.expectedDate=expectedDate; this.resultDeliveryMethod=resultDeliveryMethod;
        this.serviceConditions=serviceConditions; this.delayNotice=delayNotice; this.status=completed?"COMPLETED":"DRAFT";
        this.updatedAt=Instant.now();
    }
    public void replaceItems(List<TestRequestItem> values) { items.clear(); values.forEach(this::addItem); updatedAt=Instant.now(); }
    private void addItem(TestRequestItem item) { items.add(item); item.attach(this); }
    public Long getId(){return id;} public String getRequestNumber(){return requestNumber;} public LocalDate getRequestDate(){return requestDate;}
    public String getCustomerName(){return customerName;} public String getTaxCode(){return taxCode;} public String getContactPerson(){return contactPerson;}
    public String getCustomerAddress(){return customerAddress;} public String getCustomerPhone(){return customerPhone;} public String getCustomerEmail(){return customerEmail;}
    public String getSampleName(){return sampleName;} public String getLotCode(){return lotCode;} public String getVariety(){return variety;} public String getOrigin(){return origin;}
    public LocalDate getHarvestDate(){return harvestDate;} public LocalDate getPackingDate(){return packingDate;} public String getSamplingMode(){return samplingMode;}
    public String getPurpose(){return purpose;} public LocalDate getExpectedDate(){return expectedDate;} public String getResultDeliveryMethod(){return resultDeliveryMethod;}
    public String getServiceConditions(){return serviceConditions;} public String getDelayNotice(){return delayNotice;} public String getStatus(){return status;}
    public User getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public List<TestRequestItem> getItems(){return items;}
}
