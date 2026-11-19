package com.example.app.sampling.entity;

import jakarta.persistence.*;

@Entity @Table(name="test_request_items")
public class TestRequestItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="test_request_id") private TestRequest testRequest;
    @Column(nullable=false) private int rowOrder;
    @Column(nullable=false,length=200) private String sampleNameOrId;
    @Column(nullable=false,columnDefinition="TEXT") private String parametersText;
    @Column(length=120) private String sampleMatrix;
    @Column(length=200) private String testMethod;
    protected TestRequestItem() {}
    public TestRequestItem(int rowOrder,String sampleNameOrId,String parametersText,String sampleMatrix,String testMethod){this.rowOrder=rowOrder;this.sampleNameOrId=sampleNameOrId;this.parametersText=parametersText;this.sampleMatrix=sampleMatrix;this.testMethod=testMethod;}
    void attach(TestRequest value){testRequest=value;} public Long getId(){return id;} public int getRowOrder(){return rowOrder;} public String getSampleNameOrId(){return sampleNameOrId;} public String getParametersText(){return parametersText;} public String getSampleMatrix(){return sampleMatrix;} public String getTestMethod(){return testMethod;}
}
