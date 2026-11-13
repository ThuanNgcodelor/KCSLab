package com.example.app.testing.service;

import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.testing.dto.EquipmentRequest;
import com.example.app.testing.dto.EquipmentResponse;
import com.example.app.testing.dto.MeasurementRequest;
import com.example.app.testing.dto.MeasurementResponse;
import com.example.app.testing.dto.ReagentRequest;
import com.example.app.testing.dto.ReagentResponse;
import com.example.app.testing.dto.ReviewTestingRecordRequest;
import com.example.app.testing.dto.SaveTestingRecordRequest;
import com.example.app.testing.dto.TestingRecordResponse;
import com.example.app.testing.entity.MeasurementType;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.testing.entity.TestingEquipment;
import com.example.app.testing.entity.TestingMeasurement;
import com.example.app.testing.entity.TestingReagent;
import com.example.app.testing.entity.TestingRecord;
import com.example.app.testing.repository.TestingRecordRepository;
import com.example.app.transfer.entity.SampleTransferItem;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.repository.SampleTransferItemRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TestingRecordService {
    private final TestingRecordRepository recordRepository;
    private final SampleTransferItemRepository transferItemRepository;

    public TestingRecordService(TestingRecordRepository recordRepository,
                                SampleTransferItemRepository transferItemRepository) {
        this.recordRepository = recordRepository;
        this.transferItemRepository = transferItemRepository;
    }

    @Transactional(readOnly = true)
    public List<TestingRecordResponse> findAll() {
        return recordRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TestingRecordResponse findById(Long id) {
        return toResponse(getRecord(id));
    }

    @Transactional
    public TestingRecordResponse create(SaveTestingRecordRequest request, User analyst) {
        SampleTransferItem transferItem = transferItemRepository.findById(request.transferItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mẫu đã bàn giao"));
        if (transferItem.getTransfer().getStatus() != TransferStatus.ACCEPTED
                || transferItem.getSample().getStatus() != ReceiptStatus.TRANSFERRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mẫu phải được phòng phân tích xác nhận nhận trước khi lập biên bản");
        }
        if (recordRepository.existsByTransferItem_Id(transferItem.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mẫu đã có biên bản thử nghiệm");
        }

        TestingRecord record = new TestingRecord(nextCode(), transferItem, analyst);
        applyContent(record, request);
        transferItem.getSample().markInTesting();
        return toResponse(recordRepository.save(record));
    }

    @Transactional
    public TestingRecordResponse update(Long id, SaveTestingRecordRequest request, User actor) {
        TestingRecord record = getRecord(id);
        requireEditable(record, actor);
        if (!record.getTransferItem().getId().equals(request.transferItemId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đổi mẫu của biên bản thử nghiệm");
        }
        applyContent(record, request);
        return toResponse(recordRepository.save(record));
    }

    @Transactional
    public TestingRecordResponse submit(Long id, User actor) {
        TestingRecord record = getRecord(id);
        requireEditable(record, actor);
        if (record.getAnalysisStart() == null || record.getAnalysisEnd() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần nhập thời gian bắt đầu và kết thúc trước khi gửi kiểm tra");
        }
        if (record.getMeasurements().stream().noneMatch(row -> row.getType() == MeasurementType.RAW)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần có ít nhất một dòng số liệu thực nghiệm");
        }
        record.submit();
        record.getTransferItem().getSample().markAwaitingReview();
        return toResponse(recordRepository.save(record));
    }

    @Transactional
    public TestingRecordResponse review(Long id, ReviewTestingRecordRequest request, User checker) {
        TestingRecord record = getRecord(id);
        if (record.getStatus() != TestRecordStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biên bản chưa ở trạng thái chờ kiểm tra");
        }
        if (request.decision() != TestRecordStatus.REVIEWED && request.decision() != TestRecordStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quyết định chỉ có thể là REVIEWED hoặc RETURNED");
        }
        if (request.decision() == TestRecordStatus.RETURNED && isBlank(request.reviewNote())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do trả lại biên bản");
        }
        record.review(request.decision(), checker, blankToNull(request.reviewNote()));
        if (request.decision() == TestRecordStatus.REVIEWED) {
            record.getTransferItem().getSample().markTestReviewed();
        } else {
            record.getTransferItem().getSample().markInTesting();
        }
        return toResponse(recordRepository.save(record));
    }

    private void applyContent(TestingRecord record, SaveTestingRecordRequest request) {
        if (request.analysisStart() != null && request.analysisEnd() != null
                && request.analysisEnd().isBefore(request.analysisStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        for (ReagentRequest reagent : request.reagents()) {
            if (reagent.preparationDate() != null && reagent.expiryDate() != null
                    && reagent.expiryDate().isBefore(reagent.preparationDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hạn sử dụng hóa chất phải từ ngày pha trở đi");
            }
        }
        record.updateContent(request.analysisStart(), request.analysisEnd(), blankToNull(request.calibrationCurve()),
                blankToNull(request.calculationFormula()), request.recoveryPercent(),
                blankToNull(request.qcChartNote()), blankToNull(request.analystNote()));

        List<TestingEquipment> equipment = request.equipment().stream().map(this::toEquipment).toList();
        List<TestingReagent> reagents = request.reagents().stream().map(this::toReagent).toList();
        List<TestingMeasurement> measurements = new ArrayList<>();
        for (int index = 0; index < request.measurements().size(); index++) {
            measurements.add(toMeasurement(request.measurements().get(index), index + 1));
        }
        record.replaceDetails(equipment, reagents, measurements);
    }

    private void requireEditable(TestingRecord record, User actor) {
        if (record.getStatus() != TestRecordStatus.DRAFT && record.getStatus() != TestRecordStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biên bản đã gửi kiểm tra nên không thể sửa");
        }
        if (actor.getRole() != Role.ADMIN && !record.getAnalyst().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ người phân tích hoặc Admin được sửa biên bản");
        }
    }

    private TestingRecord getRecord(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biên bản thử nghiệm"));
    }

    private String nextCode() {
        return "BBTN-%s-%s".formatted(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }

    private TestingEquipment toEquipment(EquipmentRequest item) {
        return new TestingEquipment(item.name().trim(), blankToNull(item.serialNumber()),
                blankToNull(item.manufacturer()), blankToNull(item.identificationCode()), item.calibrationDate());
    }

    private TestingReagent toReagent(ReagentRequest item) {
        return new TestingReagent(item.name().trim(), blankToNull(item.code()), blankToNull(item.origin()),
                item.preparationDate(), item.expiryDate(), blankToNull(item.storageCondition()),
                blankToNull(item.concentration()));
    }

    private TestingMeasurement toMeasurement(MeasurementRequest item, int order) {
        return new TestingMeasurement(item.type(), order, item.replicate().trim(),
                blankToNull(item.sampleAmount()), blankToNull(item.finalVolume()),
                blankToNull(item.dilutionFactor()), blankToNull(item.measuredConcentration()),
                blankToNull(item.resultPerReplicate()), blankToNull(item.result()), blankToNull(item.deviation()));
    }

    private TestingRecordResponse toResponse(TestingRecord record) {
        SampleTransferItem item = record.getTransferItem();
        return new TestingRecordResponse(record.getId(), record.getRecordCode(), item.getId(),
                item.getTransfer().getTransferCode(), item.getTransfer().getReceipt().getReceiptCode(),
                item.getSample().getId(), item.getSampleCode(), item.getSampleName(), item.getQuantity(),
                item.getRequestedParameters(), item.getTestMethod(), item.getTransfer().getTransferDate(),
                item.getTransfer().getResultsDueDate(), record.getAnalyst().getId(), record.getAnalyst().getDisplayName(),
                record.getCheckedBy() == null ? null : record.getCheckedBy().getDisplayName(),
                record.getAnalysisStart(), record.getAnalysisEnd(), record.getCalibrationCurve(),
                record.getCalculationFormula(), record.getRecoveryPercent(), record.getQcChartNote(),
                record.getAnalystNote(), record.getReviewNote(), record.getStatus(), record.getSubmittedAt(),
                record.getReviewedAt(), record.getCreatedAt(),
                record.getEquipment().stream().map(this::toEquipmentResponse).toList(),
                record.getReagents().stream().map(this::toReagentResponse).toList(),
                record.getMeasurements().stream().map(this::toMeasurementResponse).toList());
    }

    private EquipmentResponse toEquipmentResponse(TestingEquipment item) {
        return new EquipmentResponse(item.getId(), item.getName(), item.getSerialNumber(), item.getManufacturer(),
                item.getIdentificationCode(), item.getCalibrationDate());
    }

    private ReagentResponse toReagentResponse(TestingReagent item) {
        return new ReagentResponse(item.getId(), item.getName(), item.getCode(), item.getOrigin(),
                item.getPreparationDate(), item.getExpiryDate(), item.getStorageCondition(), item.getConcentration());
    }

    private MeasurementResponse toMeasurementResponse(TestingMeasurement item) {
        return new MeasurementResponse(item.getId(), item.getType(), item.getRowOrder(), item.getReplicate(),
                item.getSampleAmount(), item.getFinalVolume(), item.getDilutionFactor(),
                item.getMeasuredConcentration(), item.getResultPerReplicate(), item.getResult(), item.getDeviation());
    }

    private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
}
