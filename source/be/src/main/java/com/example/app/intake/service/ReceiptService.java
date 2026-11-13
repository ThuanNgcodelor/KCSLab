package com.example.app.intake.service;

import com.example.app.intake.dto.CreateReceiptRequest;
import com.example.app.intake.dto.ReceiptAttachmentResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.SampleResponse;
import com.example.app.intake.dto.SampleRequest;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Customer;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptAttachment;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.CustomerRepository;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.intake.repository.SampleRepository;
import com.example.app.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptService {
    private static final DateTimeFormatter SAMPLE_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final SampleRepository sampleRepository;
    private final Path storageRoot;

    public ReceiptService(ReceiptRepository receiptRepository, CustomerRepository customerRepository,
                          SampleRepository sampleRepository,
                          @Value("${app.storage.root:storage/receipts}") String storageRoot) {
        this.receiptRepository = receiptRepository; this.customerRepository = customerRepository;
        this.sampleRepository = sampleRepository; this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> findAll() { return receiptRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList(); }

    @Transactional(readOnly = true)
    public ReceiptResponse findById(Long id) { return toResponse(getReceipt(id)); }

    @Transactional
    public ReceiptResponse create(CreateReceiptRequest request, User receivedBy) {
        String phone = request.customerPhone().trim();
        Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> customerRepository.save(
                new Customer(request.customerName().trim(), phone, blankToNull(request.customerEmail()), blankToNull(request.customerAddress()))));
        Instant receivedAt = request.receivedAt() == null ? Instant.now() : request.receivedAt();
        AcceptanceDecision decision = request.acceptanceDecision() == null ? AcceptanceDecision.ACCEPTED : request.acceptanceDecision();
        Receipt receipt = new Receipt(nextReceiptCode(receivedAt), customer, receivedBy, request.deliveredBy().trim(), receivedAt,
                request.sampleName().trim(), request.quantity(), request.packaging(), request.sealNumber(), request.sealIntact(),
                request.sampleCondition(), request.conditionDetail(), request.transportInfo(), request.receivedTemperature(),
                request.storageCondition(), request.requestedParameters(), request.expectedDate(), request.specialRequest(),
                decision, request.acceptanceReason());
        LocalDate date = receivedAt.atZone(ZoneOffset.UTC).toLocalDate();
        int sequence = (int) sampleRepository.countByReceivedDate(date);
        for (SampleRequest sampleRequest : request.samples()) {
            sequence++;
            String source = sampleRequest.sourceCode().trim().toUpperCase();
            String product = sampleRequest.productType().trim().toUpperCase();
            String code = "%s-%s-%s-%03d".formatted(source, product, SAMPLE_DATE.format(date), sequence);
            receipt.addSample(new Sample(code, source, product, date, sequence, sampleRequest.name().trim(),
                    sampleRequest.quantity(), sampleRequest.packaging(), sampleRequest.condition(), sampleRequest.requestedParameters()));
        }
        return toResponse(receiptRepository.save(receipt));
    }

    @Transactional
    public ReceiptAttachmentResponse attach(Long receiptId, MultipartFile file) {
        Receipt receipt = getReceipt(receiptId);
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File phiếu không được để trống");
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "receipt-file" : file.getOriginalFilename());
        if (original.contains("..")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ");
        String storageName = UUID.randomUUID() + "-" + original;
        try {
            Files.createDirectories(storageRoot);
            Path destination = storageRoot.resolve(storageName).normalize();
            if (!destination.startsWith(storageRoot)) throw new IOException("Invalid storage path");
            Files.copy(file.getInputStream(), destination);
            ReceiptAttachment attachment = new ReceiptAttachment(original,
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    storageName, file.getSize(), sha256(destination));
            receipt.addAttachment(attachment);
            receiptRepository.save(receipt);
            return toAttachmentResponse(attachment);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file phiếu", exception);
        }
    }

    private Receipt getReceipt(Long id) { return receiptRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận")); }
    private String nextReceiptCode(Instant receivedAt) { return "TN-%s-%s".formatted(receivedAt.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd")), UUID.randomUUID().toString().substring(0, 6).toUpperCase()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String sha256(Path file) throws IOException { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); } catch (Exception exception) { throw new IOException("Không thể tạo checksum", exception); } }
    private ReceiptResponse toResponse(Receipt receipt) { return new ReceiptResponse(receipt.getId(), receipt.getReceiptCode(), receipt.getCustomer().getName(), receipt.getCustomer().getPhone(), receipt.getDeliveredBy(), receipt.getReceivedAt(), receipt.getSampleName(), receipt.getQuantity(), receipt.getPackaging(), receipt.getSealNumber(), receipt.isSealIntact(), receipt.getSampleCondition(), receipt.getConditionDetail(), receipt.getTransportInfo(), receipt.getReceivedTemperature(), receipt.getStorageCondition(), receipt.getRequestedParameters(), receipt.getExpectedDate(), receipt.getSpecialRequest(), receipt.getAcceptanceDecision(), receipt.getAcceptanceReason(), receipt.getStatus(), receipt.getSamples().stream().map(sample -> new SampleResponse(sample.getSampleCode(), sample.getSourceCode(), sample.getProductType(), sample.getName(), sample.getStatus())).toList(), receipt.getAttachments().stream().map(this::toAttachmentResponse).toList()); }
    private ReceiptAttachmentResponse toAttachmentResponse(ReceiptAttachment attachment) { return new ReceiptAttachmentResponse(attachment.getId(), attachment.getOriginalName(), attachment.getContentType(), attachment.getSize(), attachment.getChecksum(), attachment.getUploadedAt()); }
}
