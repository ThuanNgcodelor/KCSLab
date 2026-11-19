package com.example.app.sampling.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="field_sampling_forms")
public class FieldSampling {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="test_request_id",unique=true) private TestRequest testRequest;
 @Column(nullable=false,unique=true,length=80) private String formNumber; @Column(nullable=false) private LocalDate formDate;
 private String representative; private String sampleCollector; private String collectorPhone; private Instant samplingStart; private Instant samplingEnd;
 @Column(length=500) private String samplingLocation; private String growingArea; private String packingFacility; private String vehicleNumber; private String trailerNumber; private String containerNumber;
 private String sampleCondition; private String storageCondition; private String sampleSealNumber; @Column(nullable=false) private boolean sealPresent;
 private String samplingMethod; private String sampleQuantity; @Column(columnDefinition="TEXT") private String fieldNotes;
 @Column(nullable=false,updatable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt;
 @Column(length=30) private String representativePhone; @Column(length=20) private String storageTemperature;
 @OneToMany(mappedBy="fieldSampling",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("rowOrder") private List<FieldSamplingItem> items=new ArrayList<>();
 protected FieldSampling(){} public FieldSampling(TestRequest r){testRequest=r;createdAt=Instant.now();updatedAt=createdAt;}
 public void update(String n,LocalDate d,String rep,String collector,String phone,Instant start,Instant end,String location,String growing,String packing,String vehicle,String trailer,String container,String condition,String storage,String seal,boolean present,String method,String quantity,String notes){formNumber=n;formDate=d;representative=rep;sampleCollector=collector;collectorPhone=phone;samplingStart=start;samplingEnd=end;samplingLocation=location;growingArea=growing;packingFacility=packing;vehicleNumber=vehicle;trailerNumber=trailer;containerNumber=container;sampleCondition=condition;storageCondition=storage;sampleSealNumber=seal;sealPresent=present;samplingMethod=method;sampleQuantity=quantity;fieldNotes=notes;updatedAt=Instant.now();}
 public void updateExtras(String representativePhone,String storageTemperature){this.representativePhone=representativePhone;this.storageTemperature=storageTemperature;updatedAt=Instant.now();}
 public void replaceItems(List<FieldSamplingItem> values){items.clear();values.forEach(v->{items.add(v);v.attach(this);});updatedAt=Instant.now();}
 public String getRepresentativePhone(){return representativePhone;} public String getStorageTemperature(){return storageTemperature;} public List<FieldSamplingItem> getItems(){return items;}
 public Long getId(){return id;} public TestRequest getTestRequest(){return testRequest;} public String getFormNumber(){return formNumber;} public LocalDate getFormDate(){return formDate;} public String getRepresentative(){return representative;} public String getSampleCollector(){return sampleCollector;} public String getCollectorPhone(){return collectorPhone;} public Instant getSamplingStart(){return samplingStart;} public Instant getSamplingEnd(){return samplingEnd;} public String getSamplingLocation(){return samplingLocation;} public String getGrowingArea(){return growingArea;} public String getPackingFacility(){return packingFacility;} public String getVehicleNumber(){return vehicleNumber;} public String getTrailerNumber(){return trailerNumber;} public String getContainerNumber(){return containerNumber;} public String getSampleCondition(){return sampleCondition;} public String getStorageCondition(){return storageCondition;} public String getSampleSealNumber(){return sampleSealNumber;} public boolean isSealPresent(){return sealPresent;} public String getSamplingMethod(){return samplingMethod;} public String getSampleQuantity(){return sampleQuantity;} public String getFieldNotes(){return fieldNotes;}
}
