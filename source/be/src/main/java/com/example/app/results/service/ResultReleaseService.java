package com.example.app.results.service;

import com.example.app.common.dto.PagedResponse;
import com.example.app.results.dto.ResultReleaseDetailResponse;
import com.example.app.results.dto.ResultReleaseListItem;
import com.example.app.results.entity.ResultRelease;
import com.example.app.results.entity.ResultReleaseEvent;
import com.example.app.results.entity.ResultReleaseFile;
import com.example.app.results.entity.ResultReleaseStatus;
import com.example.app.results.repository.ResultReleaseEventRepository;
import com.example.app.results.repository.ResultReleaseFileRepository;
import com.example.app.results.repository.ResultReleaseRepository;
import com.example.app.testing.entity.TestRecordStatus;
import com.example.app.intake.service.SampleCodes;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Result dossier workflow: {@code DRAFT/RETURNED -> SUBMITTED -> PUBLISHED -> REVOKED}.
 * Separation of duties assumed until LAB confirms: the technician who created the dossier uploads and
 * submits; only an ADMIN who is neither the creator nor the analyst of the testing record may return,
 * publish or revoke. Publishing additionally requires a reviewed testing record and a sample whose
 * CFC-TN-14 coding is verified. The result number format is provisional (LAB has not fixed it).
 */
@Service
public class ResultReleaseService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LOOKUP_CODE_LENGTH = 16;

    private final ResultReleaseRepository releases;
    private final ResultReleaseFileRepository files;
    private final ResultReleaseEventRepository events;
    private final com.example.app.results.repository.ResultReleaseItemRepository items;
    private final ReportPdfService pdf;
    private final ResultNumberSequence numbers;
    private final String defaultQualityHead;
    private final String defaultDirector;
    private final Path storageRoot;
    private final SecureRandom random = new SecureRandom();

    public ResultReleaseService(ResultReleaseRepository releases, ResultReleaseFileRepository files,
                                ResultReleaseEventRepository events,
                                com.example.app.results.repository.ResultReleaseItemRepository items,
                                ReportPdfService pdf, ResultNumberSequence numbers,
                                @Value("${app.report.quality-head-name:}") String defaultQualityHead,
                                @Value("${app.report.director-name:}") String defaultDirector,
                                @Value("${app.storage.results-root:storage/results}") String storageRoot) {
        this.releases = releases; this.files = files; this.events = events;
        this.items = items; this.pdf = pdf; this.numbers = numbers;
        this.defaultQualityHead = defaultQualityHead; this.defaultDirector = defaultDirector;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    private static final java.util.Map<String, String> SORT_FIELDS = java.util.Map.of(
            "updatedAt", "updatedAt", "createdAt", "createdAt", "status", "status", "version", "version",
            "resultNumber", "resultNumber", "recordCode", "testingRecord.recordCode",
            "publishedAt", "publishedAt");

    public record ReadyRecord(Long testingRecordId, String recordCode, String sampleCode, String sampleName,
                              String analystName, java.time.Instant reviewedAt) {}

    @Transactional(readOnly = true)
    public PagedResponse<ResultReleaseListItem> list(ResultReleaseStatus status, String q, java.time.LocalDate from,
                                                     java.time.LocalDate to, int page, int size, String sort) {
        com.example.app.common.query.ListQuery.requireOrderedRange(from, to);
        var parsed = com.example.app.common.query.ListQuery.parseSort(sort, SORT_FIELDS, "updatedAt,desc");
        org.springframework.data.jpa.domain.Specification<ResultRelease> spec = (root, query, builder) -> builder.conjunction();
        if (status != null) spec = spec.and((root, query, builder) -> builder.equal(root.get("status"), status));
        if (from != null) {
            var start = com.example.app.common.query.ListQuery.startOfDay(from);
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("updatedAt"), start));
        }
        if (to != null) {
            var end = com.example.app.common.query.ListQuery.endExclusive(to);
            spec = spec.and((root, query, builder) -> builder.lessThan(root.get("updatedAt"), end));
        }
        if (com.example.app.common.query.ListQuery.hasText(q)) {
            String pattern = com.example.app.common.query.ListQuery.contains(q);
            spec = spec.and((root, query, builder) -> {
                var record = root.join("testingRecord", jakarta.persistence.criteria.JoinType.INNER);
                var item = record.join("transferItem", jakarta.persistence.criteria.JoinType.INNER);
                return builder.or(
                        builder.like(builder.lower(record.get("recordCode")), pattern, '\\'),
                        builder.like(builder.lower(item.get("sampleCode")), pattern, '\\'),
                        builder.like(builder.lower(item.get("sampleName")), pattern, '\\'),
                        builder.like(builder.lower(builder.coalesce(root.<String>get("resultNumber"), "")), pattern, '\\'));
            });
        }
        var result = releases.findAll(spec, com.example.app.common.query.ListQuery.page(page, size, parsed))
                .map(ResultReleaseListItem::from);
        return PagedResponse.from(result, parsed.canonical());
    }

    /** Reviewed testing records waiting for a dossier, so the results page can start one without hunting for it. */
    @Transactional(readOnly = true)
    public List<ReadyRecord> readyRecords() {
        return releases.findReadyRecords(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id")))
                .stream().map(record -> new ReadyRecord(record.getId(), record.getRecordCode(),
                        record.getTransferItem().getSampleCode(), record.getTransferItem().getSampleName(),
                        record.getAnalyst().getDisplayName(), record.getReviewedAt())).toList();
    }

    @Transactional(readOnly = true)
    public ResultReleaseDetailResponse detail(Long id) {
        ResultRelease release = get(id);
        return toDetail(release);
    }

    @Transactional(readOnly = true)
    public List<ResultReleaseDetailResponse.EventResponse> history(Long id) {
        get(id);
        return events.findByRelease_IdOrderByIdAsc(id).stream().map(ResultReleaseDetailResponse.EventResponse::from).toList();
    }

    @Transactional
    public ResultReleaseDetailResponse addFiles(Long id, List<MultipartFile> uploads, User actor) {
        ResultRelease release = lock(id);
        requireCreator(release, actor);
        requireEditable(release);
        if (uploads == null || uploads.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa chọn file kết quả");
        }
        long current = files.countByRelease_IdAndRemovedAtIsNull(id);
        if (current + uploads.size() > ResultFilePolicy.MAX_FILES_PER_RELEASE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mỗi hồ sơ kết quả tối đa " + ResultFilePolicy.MAX_FILES_PER_RELEASE + " file");
        }
        // Validate everything first so a bad file never leaves a partial upload behind.
        List<ResultFilePolicy.Validated> validated = uploads.stream().map(ResultFilePolicy::validate).toList();
        List<Path> written = new ArrayList<>();
        registerCleanupOnRollback(written);
        try {
            Files.createDirectories(storageRoot);
            for (ResultFilePolicy.Validated item : validated) {
                ResultReleaseFile saved = store(release, item.originalName(), item.contentType(), item.extension(),
                        item.content(), actor, written);
                events.save(new ResultReleaseEvent(release, "FILE_ADDED", release.getStatus(), release.getStatus(),
                        saved.getOriginalName(), actor));
            }
            files.flush();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file kết quả", exception);
        }
        release.touch();
        return toDetail(release);
    }

    /** Writes bytes under a random name and registers the file row; the file is removed again if the transaction fails. */
    private ResultReleaseFile store(ResultRelease release, String originalName, String contentType, String extension,
                                    byte[] content, User actor, List<Path> written) throws IOException {
        Files.createDirectories(storageRoot);
        String storageName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path destination = storageRoot.resolve(storageName).normalize();
        if (!destination.startsWith(storageRoot)) throw new IOException("Invalid storage path");
        Files.write(destination, content, StandardOpenOption.CREATE_NEW);
        written.add(destination);
        return files.save(new ResultReleaseFile(release, originalName, contentType, extension, storageName,
                content.length, sha256(content), actor));
    }

    public record ReportRowInput(String parameter, String unit, String method, String result, String note) {}

    /** Replaces the result rows and signatory names of the KQKN report while the dossier is editable. */
    @Transactional
    public ResultReleaseDetailResponse updateReport(Long id, List<ReportRowInput> rows, String qualityHead,
                                                    String director, User actor) {
        ResultRelease release = lock(id);
        requireCreator(release, actor);
        requireEditable(release);
        if (rows == null || rows.isEmpty() || rows.size() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiếu kết quả cần từ 1 đến 40 chỉ tiêu");
        }
        items.deleteByReleaseId(id);
        items.flush();
        int order = 1;
        for (ReportRowInput row : rows) {
            String parameter = trim(row.parameter()), result = trim(row.result());
            if (parameter == null || result == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi dòng cần có chỉ tiêu và kết quả");
            }
            if (parameter.length() > 200 || result.length() > 120 || length(row.unit()) > 60
                    || length(row.method()) > 200 || length(row.note()) > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung dòng " + order + " quá dài");
            }
            items.save(new com.example.app.results.entity.ResultReleaseItem(release, order++, parameter,
                    trim(row.unit()), trim(row.method()), result, trim(row.note())));
        }
        if (length(qualityHead) > 120 || length(director) > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên người ký quá dài");
        }
        release.setSignatories(trim(qualityHead), trim(director));
        events.save(new ResultReleaseEvent(release, "REPORT_UPDATED", release.getStatus(), release.getStatus(),
                rows.size() + " chỉ tiêu", actor));
        return toDetail(release);
    }

    /** Draft rendering of the report (watermarked "preview"). Published reports are served from the stored PDF. */
    @Transactional(readOnly = true)
    public byte[] previewPdf(Long id) {
        ResultRelease release = get(id);
        if (items.countByRelease_Id(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chưa nhập chỉ tiêu cho phiếu kết quả");
        }
        return pdf.render(buildReport(release, release.getLookupCode(), release.getResultNumber(),
                LocalDate.now(BUSINESS_ZONE), true));
    }

    ReportData buildReport(ResultRelease release, String lookupCode, String resultNumber, LocalDate reportDate, boolean preview) {
        var record = release.getTestingRecord();
        var item = record.getTransferItem();
        var receipt = item.getTransfer().getReceipt();
        var customer = receipt.getCustomer();
        String condition = java.util.stream.Stream.of(receipt.getSampleCondition(), receipt.getConditionDetail())
                .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining(". "));
        List<ReportData.Row> rows = items.findByRelease_IdOrderByRowOrderAsc(release.getId()).stream()
                .map(row -> new ReportData.Row(row.getParameterName(), row.getUnit(), row.getMethod(),
                        row.getResultValue(), row.getNote())).toList();
        return new ReportData(resultNumber, lookupCode, reportDate, customer.getName(), customer.getAddress(),
                receipt.getSampleName(), receipt.getGrowingArea(), receipt.getPackingFacility(),
                receipt.getVehicleNumber(), receipt.getTrailerNumber(), receipt.getContainerNumber(),
                receipt.getSealNumber(), item.getSampleCode(), receipt.getSampledBy(), toDate(receipt.getSampledAt()),
                condition, item.getQuantity() != null ? item.getQuantity() : receipt.getQuantity(),
                toDate(receipt.getReceivedAt()), toDate(record.getAnalysisStart()), rows,
                effective(release.getQualityHeadName(), defaultQualityHead),
                effective(release.getDirectorName(), defaultDirector), preview);
    }

    /** Phiếu 5 data shown beside the KQKN editor so results are copied from the testing record, not retyped from memory. */
    private ResultReleaseDetailResponse.Reference reference(ResultRelease release) {
        var record = release.getTestingRecord();
        var item = record.getTransferItem();
        return new ResultReleaseDetailResponse.Reference(record.getId(), record.getRecordCode(), record.getAnalysisStart(),
                record.getAnalysisEnd(), record.getRecoveryPercent(), item.getRequestedParameters(), item.getTestMethod(),
                record.getMeasurements().stream().map(m -> new ResultReleaseDetailResponse.Measurement(m.getType().name(),
                        m.getReplicate(), m.getResultPerReplicate(), m.getResult(), m.getDeviation())).toList());
    }

    /** First draft of the result rows: one per requested parameter on Phiếu 4, with its test method. Results stay empty. */
    private List<ResultReleaseDetailResponse.ItemResponse> suggestions(ResultRelease release) {
        var item = release.getTestingRecord().getTransferItem();
        String requested = item.getRequestedParameters();
        if (requested == null || requested.isBlank()) return List.of();
        List<ResultReleaseDetailResponse.ItemResponse> rows = new ArrayList<>();
        int order = 1;
        for (String name : requested.split("[,;\\n]")) {
            String parameter = name.trim();
            if (parameter.isEmpty() || rows.size() >= 40) continue;
            rows.add(new ResultReleaseDetailResponse.ItemResponse(null, order++, parameter, null, item.getTestMethod(), "", null));
        }
        return rows;
    }

    private static LocalDate toDate(java.time.Instant instant) { return instant == null ? null : instant.atZone(BUSINESS_ZONE).toLocalDate(); }
    private static String effective(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static int length(String value) { return value == null ? 0 : value.trim().length(); }

    @Transactional
    public ResultReleaseDetailResponse removeFile(Long id, Long fileId, User actor) {
        ResultRelease release = lock(id);
        requireCreator(release, actor);
        requireEditable(release);
        ResultReleaseFile file = files.findByIdAndRelease_Id(fileId, id)
                .filter(candidate -> !candidate.isRemoved())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file kết quả"));
        file.remove(actor);
        events.save(new ResultReleaseEvent(release, "FILE_REMOVED", release.getStatus(), release.getStatus(),
                file.getOriginalName(), actor));
        release.touch();
        return toDetail(release);
    }

    /** Loads a file of a dossier for authenticated staff. Removed files are not served. */
    @Transactional(readOnly = true)
    public StoredFile loadFile(Long id, Long fileId) {
        ResultReleaseFile file = files.findByIdAndRelease_Id(fileId, id)
                .filter(candidate -> !candidate.isRemoved())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file kết quả"));
        return open(file);
    }

    public StoredFile open(ResultReleaseFile file) {
        Path path = storageRoot.resolve(file.getStorageName()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File không còn trên vùng lưu trữ");
        }
        return new StoredFile(new FileSystemResource(path), file.getContentType(), file.getOriginalName(), file.getSize());
    }

    public record StoredFile(Resource resource, String contentType, String originalName, long size) {}

    @Transactional
    public ResultReleaseDetailResponse submit(Long id, User actor) {
        ResultRelease release = lock(id);
        requireCreator(release, actor);
        requireEditable(release);
        if (release.getTestingRecord().getStatus() != TestRecordStatus.REVIEWED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biên bản thử nghiệm chưa ở trạng thái đã kiểm tra");
        }
        if (items.countByRelease_Id(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần nhập ít nhất một chỉ tiêu cho phiếu kết quả trước khi gửi duyệt");
        }
        if (effective(release.getQualityHeadName(), defaultQualityHead) == null
                || effective(release.getDirectorName(), defaultDirector) == null
                || effective(release.getQualityHeadName(), defaultQualityHead).isBlank()
                || effective(release.getDirectorName(), defaultDirector).isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần nhập tên người ký (Trưởng phòng QLCLSP, Tổng Giám đốc)");
        }
        ResultReleaseStatus from = release.getStatus();
        release.submit(actor);
        events.save(new ResultReleaseEvent(release, "SUBMITTED", from, release.getStatus(), null, actor));
        return toDetail(release);
    }

    @Transactional
    public ResultReleaseDetailResponse giveBack(Long id, String reason, User actor) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do trả lại hồ sơ");
        }
        ResultRelease release = lock(id);
        requireApprover(release, actor);
        ResultReleaseStatus from = release.getStatus();
        release.giveBack(actor, reason.trim());
        events.save(new ResultReleaseEvent(release, "RETURNED", from, release.getStatus(), reason.trim(), actor));
        return toDetail(release);
    }

    @Transactional
    public ResultReleaseDetailResponse publish(Long id, Set<Long> publicFileIds, User actor) {
        ResultRelease release = lock(id);
        requireApprover(release, actor);
        if (release.getStatus() != ResultReleaseStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ chưa ở trạng thái chờ duyệt");
        }
        var record = release.getTestingRecord();
        if (record.getStatus() != TestRecordStatus.REVIEWED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Biên bản thử nghiệm chưa ở trạng thái đã kiểm tra");
        }
        var sample = record.getTransferItem().getSample();
        if (!sample.isCodingVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mẫu " + sample.getSampleCode() + " chưa được xác minh mã hóa nên chưa thể phát hành");
        }
        if (releases.existsByTestingRecord_IdAndStatus(record.getId(), ResultReleaseStatus.PUBLISHED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Biên bản này đang có một phiên bản đã phát hành; hãy thu hồi trước");
        }
        if (items.countByRelease_Id(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu kết quả chưa có chỉ tiêu");
        }
        List<ResultReleaseFile> active = files.findByRelease_IdAndRemovedAtIsNullOrderByIdAsc(id);
        Set<Long> activeIds = new HashSet<>();
        active.forEach(file -> activeIds.add(file.getId()));
        if (publicFileIds != null && !activeIds.containsAll(publicFileIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách file công khai có file không thuộc hồ sơ");
        }
        // The generated KQKN PDF is always public. Uploaded attachments stay internal unless the Admin lists them.
        for (ResultReleaseFile file : active) file.setPublicDownload(publicFileIds != null && publicFileIds.contains(file.getId()));

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        String prefix = SampleCodes.productCode(sample.getProductType())
                + today.format(DateTimeFormatter.ofPattern("MMyyyy"));
        String resultNumber = numbers.next(prefix);
        String lookupCode = newLookupCode();
        release.publish(actor, lookupCode, resultNumber);
        try {
            releases.saveAndFlush(release);
            List<Path> written = new ArrayList<>();
            registerCleanupOnRollback(written);
            byte[] report = pdf.render(buildReport(release, lookupCode, resultNumber, today, false));
            ResultReleaseFile generated = store(release, "KQKN-" + resultNumber + ".pdf", "application/pdf", "pdf",
                    report, actor, written).asGeneratedReport();
            files.saveAndFlush(generated);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể phát hành do xung đột dữ liệu", exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu PDF phiếu kết quả", exception);
        }
        events.save(new ResultReleaseEvent(release, "PUBLISHED", ResultReleaseStatus.SUBMITTED,
                release.getStatus(), release.getResultNumber(), actor));
        return toDetail(release);
    }

    @Transactional
    public ResultReleaseDetailResponse revoke(Long id, String reason, User actor) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do thu hồi kết quả");
        }
        ResultRelease release = lock(id);
        requireApprover(release, actor);
        ResultReleaseStatus from = release.getStatus();
        release.revoke(actor, reason.trim());
        events.save(new ResultReleaseEvent(release, "REVOKED", from, release.getStatus(), reason.trim(), actor));
        return toDetail(release);
    }

    /** Opens the next version of a record whose latest version was revoked. */
    @Transactional
    public ResultReleaseDetailResponse revise(Long id, User actor) {
        ResultRelease current = lock(id);
        if (actor.getRole() != Role.TECHNICIAN && actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ kỹ thuật viên hoặc Admin được lập phiên bản mới");
        }
        Long recordId = current.getTestingRecord().getId();
        ResultRelease latest = releases.findLatestForRecord(recordId).orElse(current);
        if (!latest.getId().equals(current.getId()) || current.getStatus() != ResultReleaseStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ có thể lập phiên bản mới từ phiên bản mới nhất đã bị thu hồi");
        }
        try {
            ResultRelease next = releases.saveAndFlush(new ResultRelease(current.getTestingRecord(), actor,
                    releases.maxVersion(recordId) + 1));
            events.save(new ResultReleaseEvent(next, "REVISION_CREATED", null, next.getStatus(),
                    "Từ phiên bản " + current.getVersion(), actor));
            return toDetail(next);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiên bản mới đã được tạo", exception);
        }
    }

    // --- helpers -----------------------------------------------------------------------------

    private ResultRelease get(Long id) {
        return releases.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ kết quả"));
    }

    private ResultRelease lock(Long id) {
        return releases.findByIdForUpdate(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ kết quả"));
    }

    private void requireEditable(ResultRelease release) {
        if (!release.isEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ đã gửi duyệt hoặc phát hành nên không thể sửa");
        }
    }

    private void requireCreator(ResultRelease release, User actor) {
        // TECHNICIAN or ADMIN may prepare a dossier (both may write Phiếu 5), but only the person who opened it edits it.
        // Approval stays independent: the approver can never be the creator (see requireApprover).
        if ((actor.getRole() != Role.TECHNICIAN && actor.getRole() != Role.ADMIN)
                || !release.getCreatedBy().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ người lập hồ sơ được sửa nội dung, tải file và gửi duyệt");
        }
    }

    private void requireApprover(ResultRelease release, User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin được duyệt, phát hành hoặc thu hồi");
        }
        if (release.getCreatedBy().getId().equals(actor.getId())
                || release.getTestingRecord().getAnalyst().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Người duyệt phải độc lập với người lập hồ sơ và người phân tích");
        }
    }

    private ResultReleaseDetailResponse toDetail(ResultRelease release) {
        var rows = items.findByRelease_IdOrderByRowOrderAsc(release.getId());
        return ResultReleaseDetailResponse.from(release,
                files.findByRelease_IdAndRemovedAtIsNullOrderByIdAsc(release.getId()),
                rows, defaultQualityHead, defaultDirector, reference(release), rows.isEmpty() ? suggestions(release) : List.of());
    }

    private String newLookupCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomString(LOOKUP_CODE_LENGTH);
            if (!releases.existsByLookupCode(code)) return code;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được mã tra cứu");
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        return builder.toString();
    }

    private void registerCleanupOnRollback(List<Path> written) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) return;
                for (Path path : written) {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                }
            }
        });
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
