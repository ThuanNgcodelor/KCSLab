package com.example.app.sampling.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name="sampling_plans")
public class SamplingPlan {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="test_request_id",unique=true) private TestRequest testRequest;
 @Column(nullable=false,unique=true,length=80) private String formNumber; @Column(nullable=false) private LocalDate formDate;
 private String representative; private String sampleCollector; private String collectorPhone; private Instant samplingStart; private Instant samplingEnd;
 @Column(length=500) private String samplingLocation; private String growingArea; private String packingFacility; private String vehicleNumber; private String trailerNumber; private String containerNumber;
 @Column(columnDefinition="TEXT") private String schemeNotes; @Column(columnDefinition="TEXT") private String transportConditions;
 @Column(nullable=false,updatable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt;
 protected SamplingPlan(){} public SamplingPlan(TestRequest r){testRequest=r;createdAt=Instant.now();updatedAt=createdAt;}
 public void update(String n,LocalDate d,String rep,String collector,String phone,Instant start,Instant end,String location,String growing,String packing,String vehicle,String trailer,String container,String scheme,String transport){formNumber=n;formDate=d;representative=rep;sampleCollector=collector;collectorPhone=phone;samplingStart=start;samplingEnd=end;samplingLocation=location;growingArea=growing;packingFacility=packing;vehicleNumber=vehicle;trailerNumber=trailer;containerNumber=container;schemeNotes=scheme;transportConditions=transport;updatedAt=Instant.now();}
 public Long getId(){return id;} public String getFormNumber(){return formNumber;} public LocalDate getFormDate(){return formDate;} public String getRepresentative(){return representative;} public String getSampleCollector(){return sampleCollector;} public String getCollectorPhone(){return collectorPhone;} public Instant getSamplingStart(){return samplingStart;} public Instant getSamplingEnd(){return samplingEnd;} public String getSamplingLocation(){return samplingLocation;} public String getGrowingArea(){return growingArea;} public String getPackingFacility(){return packingFacility;} public String getVehicleNumber(){return vehicleNumber;} public String getTrailerNumber(){return trailerNumber;} public String getContainerNumber(){return containerNumber;} public String getSchemeNotes(){return schemeNotes;} public String getTransportConditions(){return transportConditions;}
}
