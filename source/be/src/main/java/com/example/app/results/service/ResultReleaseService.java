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
    private static final int MAX_PAGE_SIZE = 100;

    private final ResultReleaseRepository releases;
    private final ResultReleaseFileRepository files;
    private final ResultReleaseEventRepository events;
    private final Path storageRoot;
    private final SecureRandom random = new SecureRandom();

    public ResultReleaseService(ResultReleaseRepository releases, ResultReleaseFileRepository files,
                                ResultReleaseEventRepository events,
                                @Value("${app.storage.results-root:storage/results}") String storageRoot) {
        this.releases = releases; this.files = files; this.events = events;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResultReleaseListItem> list(ResultReleaseStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phân trang không hợp lệ");
        }
        var result = releases.search(status, PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        ).map(ResultReleaseListItem::from);
        return PagedResponse.from(result, "updatedAt,desc");
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
                String storageName = UUID.randomUUID().toString().replace("-", "") + "." + item.extension();
                Path destination = storageRoot.resolve(storageName).normalize();
                if (!destination.startsWith(storageRoot)) throw new IOException("Invalid storage path");
                Files.write(destination, item.content(), StandardOpenOption.CREATE_NEW);
                written.add(destination);
                ResultReleaseFile saved = files.save(new ResultReleaseFile(release, item.originalName(),
                        item.contentType(), item.extension(), storageName, item.content().length,
                        sha256(item.content()), actor));
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
        if (files.countByRelease_IdAndRemovedAtIsNull(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần đính kèm ít nhất một file trước khi gửi duyệt");
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
        List<ResultReleaseFile> active = files.findByRelease_IdAndRemovedAtIsNullOrderByIdAsc(id);
        if (active.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ chưa có file");
        }
        Set<Long> activeIds = new HashSet<>();
        active.forEach(file -> activeIds.add(file.getId()));
        if (publicFileIds != null && !activeIds.containsAll(publicFileIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách file công khai có file không thuộc hồ sơ");
        }
        int publicCount = 0;
        for (ResultReleaseFile file : active) {
            // Default: PDF only. Admin may explicitly enable Word/Excel by listing the file ids.
            boolean isPublic = publicFileIds == null ? "pdf".equals(file.getExtension()) : publicFileIds.contains(file.getId());
            file.setPublicDownload(isPublic);
            if (isPublic) publicCount++;
        }
        if (publicCount == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cần ít nhất một file công khai (mặc định là PDF) để phát hành");
        }
        release.publish(actor, newLookupCode(), newResultNumber());
        try {
            releases.saveAndFlush(release);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể phát hành do xung đột dữ liệu", exception);
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
        if (actor.getRole() != Role.TECHNICIAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ kỹ thuật viên được lập phiên bản mới");
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
        if (actor.getRole() != Role.TECHNICIAN || !release.getCreatedBy().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ kỹ thuật viên lập hồ sơ được tải file và gửi duyệt");
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
        return ResultReleaseDetailResponse.from(release,
                files.findByRelease_IdAndRemovedAtIsNullOrderByIdAsc(release.getId()));
    }

    private String newLookupCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomString(LOOKUP_CODE_LENGTH);
            if (!releases.existsByLookupCode(code)) return code;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được mã tra cứu");
    }

    private String newResultNumber() {
        String date = LocalDate.now(BUSINESS_ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 10; attempt++) {
            String number = "KQ-" + date + "-" + randomString(6);
            if (!releases.existsByResultNumber(number)) return number;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được số phiếu kết quả");
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
