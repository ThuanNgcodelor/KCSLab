package com.example.app.intake.dto;

import com.example.app.intake.entity.SealStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Fields of a saved receipt that may be corrected. Codes, Số/No., customer and attachments are not editable here. */
public record UpdateReceiptRequest(
        @Size(max = 500) String reason,
        @NotBlank @Size(max = 160) String deliveredBy,
        @Size(max = 160) String sampledBy,
        Instant sampledAt,
        @Size(max = 255) String growingArea,
        @Size(max = 255) String packingFacility,
        @Size(max = 80) String vehicleNumber,
        @Size(max = 80) String trailerNumber,
        @Size(max = 80) String containerNumber,
        @Size(max = 80) String sealNumber,
        @Size(max = 80) String containerSealNumber,
        SealStatus sealStatus,
        String conditionDetail,
        String transportInfo,
        Double receivedTemperature,
        @Size(max = 80) String storageCondition,
        @Size(max = 255) String storageAfterReceipt,
        LocalDate expectedDate,
        String specialRequest,
        @NotEmpty List<@Valid SampleEdit> samples
) {
    public record SampleEdit(
            Long id,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80) String quantity,
            @Size(max = 80) String weight,
            @Size(max = 80) String packaging,
            @NotBlank @Size(max = 255) String condition,
            @NotBlank String requestedParameters
    ) {}
}
