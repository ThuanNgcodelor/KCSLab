package com.example.app.intake.dto;

import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.notification.customer.dto.CustomerNotificationRequest;
import com.example.app.intake.entity.SealStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateReceiptRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        @NotBlank @Size(max = 80) String requestNumber,
        Long testRequestId,
        Long fieldSamplingId,
        @Email String customerEmail,
        String customerAddress,
        @Size(max = 160) String sampledBy,
        Instant sampledAt,
        @Size(max = 255) String growingArea,
        @Size(max = 255) String packingFacility,
        @Size(max = 80) String vehicleNumber,
        @Size(max = 80) String trailerNumber,
        @Size(max = 80) String containerNumber,
        @NotBlank String deliveredBy,
        Instant receivedAt,
        @NotBlank String sampleName,
        String quantity,
        String packaging,
        String sealNumber,
        @Size(max = 80) String containerSealNumber,
        SealStatus sealStatus,
        @NotBlank String sampleCondition,
        String conditionDetail,
        String transportInfo,
        Double receivedTemperature,
        String storageCondition,
        @Size(max = 255) String storageAfterReceipt,
        String requestedParameters,
        LocalDate expectedDate,
        String specialRequest,
        AcceptanceDecision acceptanceDecision,
        String acceptanceReason,
        @NotEmpty List<@Valid SampleRequest> samples,
        @Valid CustomerNotificationRequest customerNotification
) {

    /** Source compatibility for existing callers without customer notifications. */
    public CreateReceiptRequest(
            String customerName,
            String customerPhone,
            String requestNumber,
            Long testRequestId,
            Long fieldSamplingId,
            String customerEmail,
            String customerAddress,
            String sampledBy,
            Instant sampledAt,
            String growingArea,
            String packingFacility,
            String vehicleNumber,
            String trailerNumber,
            String containerNumber,
            String deliveredBy,
            Instant receivedAt,
            String sampleName,
            String quantity,
            String packaging,
            String sealNumber,
            String containerSealNumber,
            SealStatus sealStatus,
            String sampleCondition,
            String conditionDetail,
            String transportInfo,
            Double receivedTemperature,
            String storageCondition,
            String storageAfterReceipt,
            String requestedParameters,
            LocalDate expectedDate,
            String specialRequest,
            AcceptanceDecision acceptanceDecision,
            String acceptanceReason,
            List<SampleRequest> samples
    ) {
        this(customerName, customerPhone, requestNumber, testRequestId, fieldSamplingId, customerEmail, customerAddress, sampledBy, sampledAt, growingArea, packingFacility, vehicleNumber, trailerNumber, containerNumber, deliveredBy, receivedAt, sampleName, quantity, packaging, sealNumber, containerSealNumber, sealStatus, sampleCondition, conditionDetail, transportInfo, receivedTemperature, storageCondition, storageAfterReceipt, requestedParameters, expectedDate, specialRequest, acceptanceDecision, acceptanceReason, samples, null);
    }
    /** Keeps source compatibility for callers created before Phiếu 1/Phiếu 2 linkage was added. */
    public CreateReceiptRequest(
            String customerName, String customerPhone, String requestNumber, String customerEmail,
            String customerAddress, String sampledBy, Instant sampledAt, String growingArea,
            String packingFacility, String vehicleNumber, String trailerNumber, String containerNumber,
            String deliveredBy, Instant receivedAt, String sampleName, String quantity, String packaging,
            String sealNumber, SealStatus sealStatus, String sampleCondition, String conditionDetail,
            String transportInfo, Double receivedTemperature, String storageCondition,
            String requestedParameters, LocalDate expectedDate, String specialRequest,
            AcceptanceDecision acceptanceDecision, String acceptanceReason, List<SampleRequest> samples
    ) {
        this(customerName, customerPhone, requestNumber, null, null, customerEmail, customerAddress,
                sampledBy, sampledAt, growingArea, packingFacility, vehicleNumber, trailerNumber,
                containerNumber, deliveredBy, receivedAt, sampleName, quantity, packaging, sealNumber,
                null, sealStatus, sampleCondition, conditionDetail, transportInfo, receivedTemperature,
                storageCondition, null, requestedParameters, expectedDate, specialRequest, acceptanceDecision,
                acceptanceReason, samples);
    }

    public boolean sealIntact() {
        return sealStatus == SealStatus.INTACT;
    }
}
