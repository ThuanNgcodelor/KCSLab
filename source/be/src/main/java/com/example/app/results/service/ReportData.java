package com.example.app.results.service;

import java.time.LocalDate;
import java.util.List;

/** Everything printed on a KQKN-DV report; built from structured data, never from free text concatenation. */
public record ReportData(String resultNumber, String lookupCode, LocalDate reportDate,
        String customerName, String customerAddress, String sampleName, String orchardCode, String packingFacility,
        String truckNumber, String moocNumber, String containerNumber, String sealNumber, String sampleCode,
        String sampleCollector, LocalDate samplingDate, String sampleCondition, String sampleQuantity,
        LocalDate dateReceived, LocalDate testingDate, List<Row> rows, String qualityHeadName, String directorName,
        boolean preview, List<Photo> photos) {
    public record Row(String parameter, String unit, String method, String result, String note) {}
    /** A sample photo taken at intake (Phiếu 3), already re-encoded as JPEG; printed on the "Hình ảnh mẫu" page. */
    public record Photo(String caption, byte[] jpeg, int width, int height) {}

    public ReportData {
        photos = photos == null ? List.of() : List.copyOf(photos);
    }
}
