package com.example.app.sampling.dto;

import com.example.app.sampling.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TestRequestDtos {
 private TestRequestDtos(){}
 public record ItemRequest(@NotBlank @Size(max=200) String sampleNameOrId,@NotBlank String parameters,
                           @Size(max=120) String matrix,@Size(max=200) String testMethod){}
 public record SaveRequest(@NotBlank @Size(max=80) String requestNumber,@NotNull LocalDate requestDate,
   @NotBlank @Size(max=200) String customerName,@Size(max=40) String taxCode,@Size(max=160) String contactPerson,
   @Size(max=500) String customerAddress,@NotBlank @Size(max=30) String customerPhone,@Email @Size(max=200) String customerEmail,
   @NotBlank @Size(max=200) String sampleName,@Size(max=100) String lotCode,@Size(max=120) String variety,@Size(max=255) String origin,
   LocalDate harvestDate,LocalDate packingDate,@NotBlank @Pattern(regexp="COMPANY|CUSTOMER") String samplingMode,
   @Size(max=500) String purpose,LocalDate expectedDate,@Size(max=120) String resultDeliveryMethod,String serviceConditions,
   @Size(max=120) String delayNotice,boolean completed,@NotEmpty List<@Valid ItemRequest> items){}
 public record ItemResponse(Long id,String sampleNameOrId,String parameters,String matrix,String testMethod){static ItemResponse from(TestRequestItem i){return new ItemResponse(i.getId(),i.getSampleNameOrId(),i.getParametersText(),i.getSampleMatrix(),i.getTestMethod());}}
 public record Response(Long id,String requestNumber,LocalDate requestDate,String customerName,String taxCode,String contactPerson,
   String customerAddress,String customerPhone,String customerEmail,String sampleName,String lotCode,String variety,String origin,
   LocalDate harvestDate,LocalDate packingDate,String samplingMode,String purpose,LocalDate expectedDate,String resultDeliveryMethod,
   String serviceConditions,String delayNotice,String status,Long createdById,String createdByName,Instant createdAt,Instant updatedAt,
   List<ItemResponse> items,PlanResponse samplingPlan,FieldResponse fieldSampling){
   public static Response from(TestRequest r,SamplingPlan p,FieldSampling f){return new Response(r.getId(),r.getRequestNumber(),r.getRequestDate(),r.getCustomerName(),r.getTaxCode(),r.getContactPerson(),r.getCustomerAddress(),r.getCustomerPhone(),r.getCustomerEmail(),r.getSampleName(),r.getLotCode(),r.getVariety(),r.getOrigin(),r.getHarvestDate(),r.getPackingDate(),r.getSamplingMode(),r.getPurpose(),r.getExpectedDate(),r.getResultDeliveryMethod(),r.getServiceConditions(),r.getDelayNotice(),r.getStatus(),r.getCreatedBy().getId(),r.getCreatedBy().getDisplayName(),r.getCreatedAt(),r.getUpdatedAt(),r.getItems().stream().map(ItemResponse::from).toList(),p==null?null:PlanResponse.from(p),f==null?null:FieldResponse.from(f));}
 }
 public record SavePlan(@NotBlank @Size(max=80) String formNumber,@NotNull LocalDate formDate,@Size(max=160) String representative,
   @NotBlank @Size(max=160) String sampleCollector,@Size(max=30) String collectorPhone,Instant samplingStart,Instant samplingEnd,
   @Size(max=500) String samplingLocation,@Size(max=255) String growingArea,@Size(max=255) String packingFacility,
   @Size(max=80) String vehicleNumber,@Size(max=80) String trailerNumber,@Size(max=80) String containerNumber,String schemeNotes,String transportConditions){}
 public record PlanResponse(Long id,String formNumber,LocalDate formDate,String representative,String sampleCollector,String collectorPhone,Instant samplingStart,Instant samplingEnd,String samplingLocation,String growingArea,String packingFacility,String vehicleNumber,String trailerNumber,String containerNumber,String schemeNotes,String transportConditions){static PlanResponse from(SamplingPlan p){return new PlanResponse(p.getId(),p.getFormNumber(),p.getFormDate(),p.getRepresentative(),p.getSampleCollector(),p.getCollectorPhone(),p.getSamplingStart(),p.getSamplingEnd(),p.getSamplingLocation(),p.getGrowingArea(),p.getPackingFacility(),p.getVehicleNumber(),p.getTrailerNumber(),p.getContainerNumber(),p.getSchemeNotes(),p.getTransportConditions());}}
 public record SaveField(@NotBlank @Size(max=80) String formNumber,@NotNull LocalDate formDate,@Size(max=160) String representative,
   @NotBlank @Size(max=160) String sampleCollector,@Size(max=30) String collectorPhone,Instant samplingStart,Instant samplingEnd,
   @Size(max=500) String samplingLocation,@Size(max=255) String growingArea,@Size(max=255) String packingFacility,
   @Size(max=80) String vehicleNumber,@Size(max=80) String trailerNumber,@Size(max=80) String containerNumber,
   @Size(max=120) String sampleCondition,@Size(max=120) String storageCondition,@Size(max=80) String sampleSealNumber,
   boolean sealPresent,@Size(max=255) String samplingMethod,@Size(max=120) String sampleQuantity,String fieldNotes){}
 public record FieldResponse(Long id,String formNumber,LocalDate formDate,String representative,String sampleCollector,String collectorPhone,Instant samplingStart,Instant samplingEnd,String samplingLocation,String growingArea,String packingFacility,String vehicleNumber,String trailerNumber,String containerNumber,String sampleCondition,String storageCondition,String sampleSealNumber,boolean sealPresent,String samplingMethod,String sampleQuantity,String fieldNotes){static FieldResponse from(FieldSampling f){return new FieldResponse(f.getId(),f.getFormNumber(),f.getFormDate(),f.getRepresentative(),f.getSampleCollector(),f.getCollectorPhone(),f.getSamplingStart(),f.getSamplingEnd(),f.getSamplingLocation(),f.getGrowingArea(),f.getPackingFacility(),f.getVehicleNumber(),f.getTrailerNumber(),f.getContainerNumber(),f.getSampleCondition(),f.getStorageCondition(),f.getSampleSealNumber(),f.isSealPresent(),f.getSamplingMethod(),f.getSampleQuantity(),f.getFieldNotes());}}
}
