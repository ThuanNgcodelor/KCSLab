package com.example.app.intake.service;

import com.example.app.common.dto.PagedResponse;
import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentContent;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.ReceiptSearchCriteria;
import com.example.app.intake.dto.SampleRequest;
import com.example.app.intake.dto.SampleResponse;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptAttachment;
import com.example.app.intake.entity.ReceiptAttachmentCategory;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptAttachmentRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.intake.repository.ReceiptSpecifications;
import com.example.app.intake.repository.SampleRepository;
import com.example.app.notification.entity.StaffNotificationType;
import com.example.app.notification.service.StaffNotificationService;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReceiptService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SAMPLE_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> RECEIPT_SORT_FIELDS = Map.of(
            "receivedAt", "receivedAt",
            "createdAt", "createdAt",
            "receiptCode", "receiptCode",
            "customerName", "customer.name",
            "sampleName", "sampleName",
            "status", "status",
            "expectedDate", "expectedDate"
    );
    private static final Map<String, String> COMMON_PRODUCT_CODES = Map.of(
            "sr", "SR",
            "sau rieng", "SR",
            "mit", "MIT",
            "thanh long", "THANHLONG"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );

    private final ReceiptRepository receiptRepository;
    private final ReceiptAttachmentRepository attachmentRepository;
    private final CustomerRepository customerRepository;
    private final SampleRepository sampleRepository;
    private final StaffNotificationService notificationService;
    private final ReceiptForm3Exporter form3Exporter;
    private final Path storageRoot;

    public ReceiptService(ReceiptRepository receiptRepository,
                          ReceiptAttachmentRepository attachmentRepository,
                          CustomerRepository customerRepository,
                          SampleRepository sampleRepository,
                          StaffNotificationService notificationService,
                          ReceiptForm3Exporter form3Exporter,
                          @Value("${app.storage.root:storage/receipts}") String storageRoot) {
        this.receiptRepository = receiptRepository;
        this.attachmentRepository = attachmentRepository;
        this.customerRepository = customerRepository;
        this.sampleRepository = sampleRepository;
        this.notificationService = notificationService;
        this.form3Exporter = form3Exporter;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReceiptResponse> findAll(int page, int size, String sort) {
        return findAll(ReceiptSearchCriteria.empty(), page, size, sort);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReceiptResponse> findAll(ReceiptSearchCriteria criteria,
                                                   int page, int size, String sort) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số trang phải từ 0 trở lên");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Kích thước trang phải từ 1 đến " + MAX_PAGE_SIZE);
        }
        if (criteria != null && criteria.from() != null && criteria.to() != null
                && criteria.from().isAfter(criteria.to())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được sau ngày kết thúc");
        }

        ReceiptSort receiptSort = parseSort(sort);
        Sort ordering = Sort.by(receiptSort.direction(), receiptSort.property());
        if (!"id".equals(receiptSort.property())) {
            ordering = ordering.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        Page<ReceiptResponse> result = receiptRepository
                .findAll(ReceiptSpecifications.byCriteria(criteria), PageRequest.of(page, size, ordering))
                .map(this::toResponse);
        return PagedResponse.from(result, receiptSort.canonical());
    }

    @Transactional(readOnly = true)
    public ReceiptResponse findById(Long id) {
        return toResponse(getReceipt(id));
    }

    @Transactional(readOnly = true)
    public byte[] exportForm3(Long id) {
        return form3Exporter.export(getReceipt(id));
    }

    @Transactional
    public ReceiptResponse create(CreateReceiptRequest request, User receivedBy) {
        validateBusinessRules(request);
        String phone = request.customerPhone().trim();
        Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> customerRepository.save(
                new Customer(request.customerName().trim(), phone, blankToNull(request.customerEmail()),
                        blankToNull(request.customerAddress()))));
        Instant receivedAt = request.receivedAt() == null ? Instant.now() : request.receivedAt();
        AcceptanceDecision decision = request.acceptanceDecision() == null
                ? AcceptanceDecision.ACCEPTED
                : request.acceptanceDecision();
        Receipt receipt = new Receipt(nextReceiptCode(receivedAt), customer, receivedBy,
                request.deliveredBy().trim(), receivedAt, blankToNull(request.sampledBy()), request.sampledAt(),
                blankToNull(request.growingArea()), blankToNull(request.packingFacility()),
                blankToNull(request.vehicleNumber()), blankToNull(request.trailerNumber()),
                blankToNull(request.containerNumber()), request.sampleName().trim(), request.quantity(),
                request.packaging(), request.sealNumber(), request.sealIntact(), request.sampleCondition(),
                request.conditionDetail(), request.transportInfo(), request.receivedTemperature(),
                request.storageCondition(), request.requestedParameters(), request.expectedDate(),
                request.specialRequest(), decision, request.acceptanceReason());
        receipt.setRequestNumber(request.requestNumber().trim());
        receipt.setSealStatus(request.sealStatus());

        LocalDate date = receivedAt.atZone(BUSINESS_ZONE).toLocalDate();
        int sequence = (int) sampleRepository.countByReceivedDate(date);
        for (SampleRequest sampleRequest : request.samples()) {
            sequence++;
            String source = sampleRequest.sourceCode().trim().toUpperCase(Locale.ROOT);
            String productType = sampleRequest.productType().trim();
            String productCode = productCode(productType);
            String code = "%s-%s-%s-%03d".formatted(source, productCode, SAMPLE_DATE.format(date), sequence);
            receipt.addSample(new Sample(code, source, productType, date, sequence, sampleRequest.name().trim(),
                    sampleRequest.quantity(), sampleRequest.packaging(), sampleRequest.condition(),
                    sampleRequest.requestedParameters()));
        }
        Receipt saved = receiptRepository.save(receipt);
        notificationService.notifyRoles(Set.of(Role.ADMIN, Role.TECHNICIAN), receivedBy,
                StaffNotificationType.INTAKE_RECORDED,
                "Phiếu tiếp nhận mới",
                "%s đã ghi nhận %d mẫu từ %s".formatted(
                        receivedBy.getDisplayName(), saved.getSamples().size(), saved.getCustomer().getName()),
                "/admin/intake/" + saved.getId(), "RECEIPT", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ReceiptAttachmentResponse attach(Long receiptId, MultipartFile file,
                                            ReceiptAttachmentCategory category, String description,
                                            Instant capturedAt, User uploadedBy) {
        Receipt receipt = getReceipt(receiptId);
        validateAttachment(file);
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "receipt-file" : file.getOriginalFilename());
        if (original.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ");
        }

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String storageName = UUID.randomUUID() + "-" + original;
        Path destination = storageRoot.resolve(storageName).normalize();
        try {
            Files.createDirectories(storageRoot);
            if (!destination.startsWith(storageRoot)) throw new IOException("Invalid storage path");
            Files.copy(file.getInputStream(), destination);
            ReceiptAttachment attachment = new ReceiptAttachment(
                    category == null ? ReceiptAttachmentCategory.OTHER : category,
                    blankToNull(description), original, contentType, storageName, file.getSize(),
                    sha256(destination), capturedAt, uploadedBy);
            receipt.addAttachment(attachment);
            attachmentRepository.saveAndFlush(attachment);
            return toAttachmentResponse(attachment);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể lưu file hồ sơ", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReceiptAttachmentContent loadAttachment(Long receiptId, Long attachmentId) {
        ReceiptAttachment attachment = attachmentRepository.findByIdAndReceipt_Id(attachmentId, receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy file hồ sơ"));
        Path path = storageRoot.resolve(attachment.getStorageName()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "File hồ sơ không còn trên vùng lưu trữ");
        }
        return new ReceiptAttachmentContent(new FileSystemResource(path), attachment.getContentType(),
                attachment.getOriginalName());
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File hồ sơ không được để trống");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Mỗi file hồ sơ không được vượt quá 10 MB");
        }
        String contentType = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Chỉ chấp nhận PDF, DOCX, JPG, PNG, WEBP, HEIC hoặc HEIF");
        }
    }

    private Receipt getReceipt(Long id) {
        return receiptRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận"));
    }

    private String nextReceiptCode(Instant receivedAt) {
        return "TN-%s-%s".formatted(receivedAt.atZone(BUSINESS_ZONE).format(RECEIPT_DATE),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String productCode(String productType) {
        String normalized = Normalizer.normalize(productType, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        String knownCode = COMMON_PRODUCT_CODES.get(normalized);
        if (knownCode != null) return knownCode;

        String compact = normalized.replace(" ", "").toUpperCase(Locale.ROOT);
        if (compact.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Loại sản phẩm phải có ít nhất một chữ hoặc số");
        }
        if (compact.length() <= 10) return compact;

        String[] words = normalized.split(" +");
        if (words.length > 1) {
            String initials = java.util.Arrays.stream(words)
                    .filter(word -> !word.isBlank())
                    .map(word -> word.substring(0, 1))
                    .reduce("", String::concat)
                    .toUpperCase(Locale.ROOT);
            if (initials.length() >= 2) return initials.substring(0, Math.min(10, initials.length()));
        }
        return compact.substring(0, 10);
    }

    private ReceiptSort parseSort(String rawSort) {
        String value = rawSort == null || rawSort.isBlank() ? "receivedAt,desc" : rawSort.trim();
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sort phải có dạng field,direction");
        }

        String field = parts[0].trim();
        String property = RECEIPT_SORT_FIELDS.get(field);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trường sort không hợp lệ: " + field);
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chiều sort chỉ nhận asc hoặc desc");
        }
        return new ReceiptSort(property, direction, field + "," + direction.name().toLowerCase(Locale.ROOT));
    }

    private record ReceiptSort(String property, Sort.Direction direction, String canonical) {}

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IOException("Không thể tạo checksum", exception);
        }
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        return new ReceiptResponse(receipt.getId(), receipt.getReceiptCode(), receipt.getCustomer().getName(),
                receipt.getCustomer().getPhone(), receipt.getReceivedBy().getId(),
                receipt.getReceivedBy().getDisplayName(), receipt.getDeliveredBy(), receipt.getRequestNumber(), receipt.getSampledBy(),
                receipt.getSampledAt(), receipt.getGrowingArea(), receipt.getPackingFacility(),
                receipt.getVehicleNumber(), receipt.getTrailerNumber(), receipt.getContainerNumber(),
                receipt.getReceivedAt(), receipt.getSampleName(), receipt.getQuantity(), receipt.getPackaging(),
                receipt.getSealNumber(), receipt.isSealIntact(), receipt.getSealStatus(), receipt.getSampleCondition(),
                receipt.getConditionDetail(), receipt.getTransportInfo(), receipt.getReceivedTemperature(),
                receipt.getStorageCondition(), receipt.getRequestedParameters(), receipt.getExpectedDate(),
                receipt.getSpecialRequest(), receipt.getAcceptanceDecision(), receipt.getAcceptanceReason(),
                receipt.getStatus(), receipt.getSamples().stream().map(sample -> new SampleResponse(
                        sample.getId(), sample.getSampleCode(), sample.getSourceCode(), sample.getProductType(),
                        sample.getName(), sample.getQuantity(), sample.getPackaging(), sample.getCondition(),
                        sample.getRequestedParameters(), sample.getStatus())).toList(),
                receipt.getAttachments().stream().map(this::toAttachmentResponse).toList());
    }

    private void validateBusinessRules(CreateReceiptRequest request) {
        if (request.sealStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải chọn trạng thái niêm phong");
        }
        if (request.acceptanceDecision() != AcceptanceDecision.ACCEPTED
                && (request.acceptanceReason() == null || request.acceptanceReason().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Phải ghi lý do hoặc điều kiện khi không chấp nhận hoàn toàn");
        }
        if (request.samples().stream().anyMatch(sample -> sample.requestedParameters() == null
                || sample.requestedParameters().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi mẫu phải có chỉ tiêu yêu cầu");
        }
    }

    private ReceiptAttachmentResponse toAttachmentResponse(ReceiptAttachment attachment) {
        User uploader = attachment.getUploadedBy();
        return new ReceiptAttachmentResponse(attachment.getId(), attachment.getCategory(),
                attachment.getDescription(), attachment.getOriginalName(), attachment.getContentType(),
                attachment.getSize(), attachment.getChecksum(), attachment.getCapturedAt(),
                attachment.getUploadedAt(), uploader == null ? null : uploader.getId(),
                uploader == null ? null : uploader.getDisplayName(),
                "/api/v1/receipts/%d/attachments/%d/content".formatted(
                        attachment.getReceipt().getId(), attachment.getId()));
    }
}
