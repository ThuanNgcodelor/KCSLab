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
import com.example.app.sampling.entity.FieldSampling;
import com.example.app.sampling.entity.TestRequest;
import com.example.app.sampling.repository.FieldSamplingRepository;
import com.example.app.sampling.repository.TestRequestRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
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
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReceiptService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> RECEIPT_SORT_FIELDS = Map.of(
            "receivedAt", "receivedAt",
            "createdAt", "createdAt",
            "receiptCode", "receiptCode",
            "requestNumber", "requestNumber",
            "customerName", "customer.name",
            "sampleName", "sampleName",
            "status", "status",
            "expectedDate", "expectedDate"
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
    private final SampleCodeSequence sampleCodeSequence;
    private final StaffNotificationService notificationService;
    private final ReceiptForm3Exporter form3Exporter;
    private final TestRequestRepository testRequestRepository;
    private final FieldSamplingRepository fieldSamplingRepository;
    private final com.example.app.dossier.storage.DossierStorageService storage;
    private final com.example.app.notification.customer.service.IntakeCustomerNotificationService customerNotifications;

    public ReceiptService(ReceiptRepository receiptRepository,
                          ReceiptAttachmentRepository attachmentRepository,
                          CustomerRepository customerRepository,
                          SampleRepository sampleRepository,
                          SampleCodeSequence sampleCodeSequence,
                          StaffNotificationService notificationService,
                          ReceiptForm3Exporter form3Exporter,
                          com.example.app.notification.customer.service.IntakeCustomerNotificationService customerNotifications,
                          TestRequestRepository testRequestRepository,
                          FieldSamplingRepository fieldSamplingRepository,
                          com.example.app.dossier.storage.DossierStorageService storage) {
        this.receiptRepository = receiptRepository;
        this.attachmentRepository = attachmentRepository;
        this.customerRepository = customerRepository;
        this.sampleRepository = sampleRepository;
        this.sampleCodeSequence = sampleCodeSequence;
        this.notificationService = notificationService;
        this.form3Exporter = form3Exporter;
        this.customerNotifications = customerNotifications;
        this.testRequestRepository = testRequestRepository;
        this.fieldSamplingRepository = fieldSamplingRepository;
        this.storage = storage;
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
    public byte[] exportForm3(Long id, Long sampleId) {
        Receipt receipt = getReceipt(id);
        Sample sample = sampleId == null ? null : receipt.getSamples().stream()
                .filter(item -> sampleId.equals(item.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mẫu trong phiếu tiếp nhận"));
        return form3Exporter.export(receipt, sample);
    }

    @Transactional
    public ReceiptResponse create(CreateReceiptRequest request, User receivedBy) {
        customerNotifications.validate(request.customerNotification());
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
        // Số/No. dạng CFC luôn lưu dạng chuẩn CFC26.00001 (gõ CFC26.0001 hay cfc26.1 đều được chuyển).
        String typedNumber = request.requestNumber().trim();
        String canonicalNumber = com.example.app.sampling.service.TestRequestNumberSequence.normalize(typedNumber);
        String requestNumber = canonicalNumber != null ? canonicalNumber : typedNumber;
        receiptRepository.findByRequestNumberUnique(requestNumber).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số/No. " + existing.getRequestNumber()
                    + " đã có hồ sơ tiếp nhận; mỗi Số/No. chỉ được dùng cho một hồ sơ");
        });
        receipt.setRequestNumber(requestNumber);
        receipt.setSealStatus(request.sealStatus());
        receipt.setStorageAfterReceipt(blankToNull(request.storageAfterReceipt()));
        receipt.setContainerSealNumber(blankToNull(request.containerSealNumber()));
        if (request.testRequestId() != null) {
            TestRequest testRequest = testRequestRepository.findById(request.testRequestId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Phiếu yêu cầu"));
            if (!testRequest.getRequestNumber().equalsIgnoreCase(requestNumber)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Số Phiếu yêu cầu không khớp hồ sơ đã chọn");
            }
            FieldSampling field = null;
            if (request.fieldSamplingId() != null) {
                field = fieldSamplingRepository.findById(request.fieldSamplingId()).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Phiếu lấy mẫu hiện trường"));
                if (!field.getTestRequest().getId().equals(testRequest.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Phiếu lấy mẫu không thuộc Phiếu yêu cầu đã chọn");
                }
            }
            receipt.linkRequest(testRequest, field);
        }

        LocalDate date = receivedAt.atZone(BUSINESS_ZONE).toLocalDate();
        for (SampleRequest sampleRequest : request.samples()) {
            int sequence = sampleCodeSequence.next(date);
            String source = SampleCodes.normalizeSource(sampleRequest.sourceCode());
            String productType = sampleRequest.productType().trim();
            String code = sampleRequest.sampleCode() == null || sampleRequest.sampleCode().isBlank()
                    ? SampleCodes.format(source, productType, date, sequence)
                    : sampleRequest.sampleCode().trim().toUpperCase(Locale.ROOT);
            if (sampleRepository.existsBySampleCodeIgnoreCase(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã mẫu " + code + " đã tồn tại");
            }
            Sample sample = new Sample(code, source, productType, date, sequence, sampleRequest.name().trim(),
                    sampleRequest.quantity(), sampleRequest.packaging(), sampleRequest.condition(),
                    sampleRequest.requestedParameters(), blankToNull(sampleRequest.weight()));
            if (sampleRequest.sampleCode() != null && !sampleRequest.sampleCode().isBlank()) {
                sample.markManuallyCoded(receivedBy);
            }
            receipt.addSample(sample);
        }
        Receipt saved;
        try {
            saved = receiptRepository.saveAndFlush(receipt);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Số/No. " + requestNumber + " đã có hồ sơ tiếp nhận; mỗi Số/No. chỉ được dùng cho một hồ sơ");
        }
        notificationService.notifyRoles(Set.of(Role.ADMIN), receivedBy,
                StaffNotificationType.INTAKE_RECORDED,
                "Phiếu tiếp nhận mới",
                "%s đã ghi nhận %d mẫu từ %s".formatted(
                        receivedBy.getDisplayName(), saved.getSamples().size(), saved.getCustomer().getName()),
                "/admin/intake/" + saved.getId(), "RECEIPT", saved.getId());
        customerNotifications.record(saved, request, receivedBy);
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
        String storageName = UUID.randomUUID() + "." + storageExtension(original);
        try {
            var stored = storage.write(1, receiptId,
                    category == ReceiptAttachmentCategory.TRANSFER_EVIDENCE ? "handover" : "intake",
                    storageName, file.getInputStream(), file.getSize());
            ReceiptAttachment attachment = new ReceiptAttachment(
                    category == null ? ReceiptAttachmentCategory.OTHER : category,
                    blankToNull(description), original, contentType, storageName, stored.size(),
                    stored.checksum(), capturedAt, uploadedBy);
            attachment.locate(stored.locationId(), stored.relativePath());
            receipt.addAttachment(attachment);
            attachmentRepository.saveAndFlush(attachment);
            return toAttachmentResponse(attachment);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được file tải lên", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReceiptAttachmentContent loadAttachment(Long receiptId, Long attachmentId) {
        ReceiptAttachment attachment = attachmentRepository.findByIdAndReceipt_Id(attachmentId, receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy file hồ sơ"));
        Path path = storage.read(attachment.getStorageLocationId(), attachment.getRelativePath(), attachment.getStorageName());
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

    private static String storageExtension(String original) {
        String extension = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).replaceAll("[^a-zA-Z0-9]", "") : "";
        return extension.isEmpty() || extension.length() > 10 ? "bin" : extension.toLowerCase(Locale.ROOT);
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        return new ReceiptResponse(receipt.getId(), receipt.getReceiptCode(), receipt.getCustomer().getName(),
                receipt.getCustomer().getPhone(), receipt.getReceivedBy().getId(),
                receipt.getReceivedBy().getDisplayName(), receipt.getDeliveredBy(), receipt.getRequestNumber(),
                receipt.getTestRequest() == null ? null : receipt.getTestRequest().getId(),
                receipt.getFieldSampling() == null ? null : receipt.getFieldSampling().getId(), receipt.getSampledBy(),
                receipt.getSampledAt(), receipt.getGrowingArea(), receipt.getPackingFacility(),
                receipt.getVehicleNumber(), receipt.getTrailerNumber(), receipt.getContainerNumber(),
                receipt.getReceivedAt(), receipt.getSampleName(), receipt.getQuantity(), receipt.getPackaging(),
                receipt.getSealNumber(), receipt.getContainerSealNumber(), receipt.isSealIntact(), receipt.getSealStatus(), receipt.getSampleCondition(),
                receipt.getConditionDetail(), receipt.getTransportInfo(), receipt.getReceivedTemperature(),
                receipt.getStorageCondition(), receipt.getStorageAfterReceipt(), receipt.getRequestedParameters(), receipt.getExpectedDate(),
                receipt.getSpecialRequest(), receipt.getAcceptanceDecision(), receipt.getAcceptanceReason(),
                receipt.getStatus(), receipt.getSamples().stream().map(sample -> new SampleResponse(
                        sample.getId(), sample.getSampleCode(), sample.getSourceCode(), sample.getProductType(),
                        sample.getName(), sample.getQuantity(), sample.getPackaging(), sample.getCondition(),
                        sample.getRequestedParameters(), sample.getStatus(), sample.getCodingStatus(),
                        sample.getVerifiedAt(), sample.getWeight())).toList(),
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
