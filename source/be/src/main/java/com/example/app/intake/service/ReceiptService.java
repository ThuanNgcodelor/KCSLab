package com.example.app.intake.service;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentContent;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
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
import com.example.app.intake.repository.SampleRepository;
import com.example.app.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ReceiptService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SAMPLE_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif"
    );

    private final ReceiptRepository receiptRepository;
    private final ReceiptAttachmentRepository attachmentRepository;
    private final CustomerRepository customerRepository;
    private final SampleRepository sampleRepository;
    private final Path storageRoot;

    public ReceiptService(ReceiptRepository receiptRepository,
                          ReceiptAttachmentRepository attachmentRepository,
                          CustomerRepository customerRepository,
                          SampleRepository sampleRepository,
                          @Value("${app.storage.root:storage/receipts}") String storageRoot) {
        this.receiptRepository = receiptRepository;
        this.attachmentRepository = attachmentRepository;
        this.customerRepository = customerRepository;
        this.sampleRepository = sampleRepository;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> findAll() {
        return receiptRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptResponse findById(Long id) {
        return toResponse(getReceipt(id));
    }

    @Transactional
    public ReceiptResponse create(CreateReceiptRequest request, User receivedBy) {
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

        LocalDate date = receivedAt.atZone(BUSINESS_ZONE).toLocalDate();
        int sequence = (int) sampleRepository.countByReceivedDate(date);
        for (SampleRequest sampleRequest : request.samples()) {
            sequence++;
            String source = sampleRequest.sourceCode().trim().toUpperCase(Locale.ROOT);
            String product = sampleRequest.productType().trim().toUpperCase(Locale.ROOT);
            String code = "%s-%s-%s-%03d".formatted(source, product, SAMPLE_DATE.format(date), sequence);
            receipt.addSample(new Sample(code, source, product, date, sequence, sampleRequest.name().trim(),
                    sampleRequest.quantity(), sampleRequest.packaging(), sampleRequest.condition(),
                    sampleRequest.requestedParameters()));
        }
        return toResponse(receiptRepository.save(receipt));
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
                receipt.getReceivedBy().getDisplayName(), receipt.getDeliveredBy(), receipt.getSampledBy(),
                receipt.getSampledAt(), receipt.getGrowingArea(), receipt.getPackingFacility(),
                receipt.getVehicleNumber(), receipt.getTrailerNumber(), receipt.getContainerNumber(),
                receipt.getReceivedAt(), receipt.getSampleName(), receipt.getQuantity(), receipt.getPackaging(),
                receipt.getSealNumber(), receipt.isSealIntact(), receipt.getSampleCondition(),
                receipt.getConditionDetail(), receipt.getTransportInfo(), receipt.getReceivedTemperature(),
                receipt.getStorageCondition(), receipt.getRequestedParameters(), receipt.getExpectedDate(),
                receipt.getSpecialRequest(), receipt.getAcceptanceDecision(), receipt.getAcceptanceReason(),
                receipt.getStatus(), receipt.getSamples().stream().map(sample -> new SampleResponse(
                        sample.getId(), sample.getSampleCode(), sample.getSourceCode(), sample.getProductType(),
                        sample.getName(), sample.getQuantity(), sample.getPackaging(), sample.getCondition(),
                        sample.getRequestedParameters(), sample.getStatus())).toList(),
                receipt.getAttachments().stream().map(this::toAttachmentResponse).toList());
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
