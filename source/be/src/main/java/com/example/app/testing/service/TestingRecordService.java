package com.example.app.testing.service;

import com.example.app.common.dto.PagedResponse;
import com.example.app.common.query.ListQuery;
import com.example.app.intake.entity.ReceiptStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.Map;
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

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "createdAt", "createdAt", "recordCode", "recordCode", "status", "status",
            "analysisStart", "analysisStart", "sampleCode", "transferItem.sampleCode",
            "resultsDueDate", "transferItem.transfer.resultsDueDate");

    public record Candidate(Long transferItemId, Long sampleId, String sampleCode, String sampleName, String quantity,
                            String requestedParameters, String testMethod, String transferCode, String receiptCode,
                            String requestNumber, java.time.LocalDate transferDate, java.time.LocalDate resultsDueDate) {}

    /** Paged, sorted, filtered list; everything is executed by the database. */
    @Transactional(readOnly = true)
    public PagedResponse<TestingRecordResponse> search(String q, TestRecordStatus status, LocalDate from, LocalDate to,
                                                       int page, int size, String sort) {
        ListQuery.requireOrderedRange(from, to);
        var parsed = ListQuery.parseSort(sort, SORT_FIELDS, "createdAt,desc");
        Specification<TestingRecord> spec = (root, query, builder) -> builder.conjunction();
        if (status != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("status"), status));
        if (from != null) {
            var start = ListQuery.startOfDay(from);
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (to != null) {
            var end = ListQuery.endExclusive(to);
            spec = spec.and((root, query, builder) -> builder.lessThan(root.get("createdAt"), end));
        }
        if (ListQuery.hasText(q)) {
            String pattern = ListQuery.contains(q);
            spec = spec.and((root, query, builder) -> {
                var item = root.join("transferItem", JoinType.INNER);
                var transfer = item.join("transfer", JoinType.INNER);
                return builder.or(
                        builder.like(builder.lower(root.get("recordCode")), pattern, '\\'),
                        builder.like(builder.lower(item.get("sampleCode")), pattern, '\\'),
                        builder.like(builder.lower(item.get("sampleName")), pattern, '\\'),
                        builder.like(builder.lower(item.get("testMethod")), pattern, '\\'),
                        builder.like(builder.lower(transfer.get("transferCode")), pattern, '\\'));
            });
        }
        return PagedResponse.from(recordRepository.findAll(spec, ListQuery.page(page, size, parsed))
                .map(this::toResponse), parsed.canonical());
    }

    @Transactional(readOnly = true)
    public List<Candidate> candidates() {
        return recordRepository.findCandidates(org.springframework.data.domain.PageRequest.of(0, 200,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")))
                .stream().map(item -> new Candidate(item.getId(), item.getSample().getId(), item.getSampleCode(),
                        item.getSampleName(), item.getQuantity(), item.getRequestedParameters(), item.getTestMethod(),
                        item.getTransfer().getTransferCode(), item.getTransfer().getReceipt().getReceiptCode(),
                        item.getTransfer().getReceipt().getRequestNumber(),
                        item.getTransfer().getTransferDate(), item.getTransfer().getResultsDueDate())).toList();
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
        if (!transferItem.getSample().isCodingVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "KCS cần đối chiếu và xác minh mã mẫu trước khi thử nghiệm");
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
    public TestingRecordResponse createFromResultFile(Long transferItemId, User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin được lập hồ sơ trực tiếp từ file kết quả");
        }
        SampleTransferItem transferItem = transferItemRepository.findById(transferItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mẫu đã bàn giao"));
        if (transferItem.getTransfer().getStatus() != TransferStatus.ACCEPTED
                || transferItem.getSample().getStatus() != ReceiptStatus.TRANSFERRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mẫu phải được KCS xác nhận nhận trước khi tải kết quả");
        }
        if (!transferItem.getSample().isCodingVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mẫu chưa có mã hợp lệ");
        }
        if (recordRepository.existsByTransferItem_Id(transferItemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mẫu đã có hồ sơ kết quả");
        }
        TestingRecord record = new TestingRecord(nextCode(), transferItem, actor);
        record.completeFromResultFile(actor);
        transferItem.getSample().markTestReviewed();
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
                item.getTransfer().getReceipt().getRequestNumber(),
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
