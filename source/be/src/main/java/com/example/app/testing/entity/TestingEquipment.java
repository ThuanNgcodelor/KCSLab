package com.example.app.testing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "testing_equipment")
public class TestingEquipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "testing_record_id")
    private TestingRecord testingRecord;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 120) private String serialNumber;
    @Column(length = 160) private String manufacturer;
    @Column(length = 120) private String identificationCode;
    private LocalDate calibrationDate;

    protected TestingEquipment() {}
    public TestingEquipment(String name, String serialNumber, String manufacturer,
                            String identificationCode, LocalDate calibrationDate) {
        this.name = name; this.serialNumber = serialNumber; this.manufacturer = manufacturer;
        this.identificationCode = identificationCode; this.calibrationDate = calibrationDate;
    }
    void attachTo(TestingRecord record) { this.testingRecord = record; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSerialNumber() { return serialNumber; }
    public String getManufacturer() { return manufacturer; }
    public String getIdentificationCode() { return identificationCode; }
    public LocalDate getCalibrationDate() { return calibrationDate; }
}
