package com.example.app.sampling.entity;

import jakarta.persistence.*;

/** One sample row of Phiếu 02 section III: name, sampling method, quantity and ticked parameters. */
@Entity @Table(name="field_sampling_items")
public class FieldSamplingItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="field_sampling_id") private FieldSampling fieldSampling;
 @Column(nullable=false) private int rowOrder;
 @Column(nullable=false,length=200) private String sampleName;
 @Column(length=255) private String samplingMethod; @Column(length=120) private String quantity;
 @Column(length=80) private String parameters; @Column(length=200) private String parametersOther;
 protected FieldSamplingItem(){}
 public FieldSamplingItem(int rowOrder,String sampleName,String samplingMethod,String quantity,String parameters,String parametersOther){this.rowOrder=rowOrder;this.sampleName=sampleName;this.samplingMethod=samplingMethod;this.quantity=quantity;this.parameters=parameters;this.parametersOther=parametersOther;}
 void attach(FieldSampling value){fieldSampling=value;}
 public Long getId(){return id;} public int getRowOrder(){return rowOrder;} public String getSampleName(){return sampleName;} public String getSamplingMethod(){return samplingMethod;} public String getQuantity(){return quantity;} public String getParameters(){return parameters;} public String getParametersOther(){return parametersOther;}
}
