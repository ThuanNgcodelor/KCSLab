package com.example.app.transfer.service;

import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.intake.repository.SampleRepository;
import com.example.app.transfer.dto.CreateTransferItemRequest;
import com.example.app.transfer.dto.CreateTransferRequest;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.dto.TransferItemResponse;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.entity.SampleTransfer;
import com.example.app.transfer.entity.SampleTransferItem;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.repository.SampleTransferItemRepository;
import com.example.app.transfer.repository.SampleTransferRepository;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {
    private static final List<TransferStatus> ACTIVE_TRANSFER_STATUSES =
            List.of(TransferStatus.PENDING, TransferStatus.ACCEPTED);

    private final SampleTransferRepository transferRepository;
    private final SampleTransferItemRepository itemRepository;
    private final ReceiptRepository receiptRepository;
    private final SampleRepository sampleRepository;

    public TransferService(SampleTransferRepository transferRepository,
                           SampleTransferItemRepository itemRepository,
                           ReceiptRepository receiptRepository,
                           SampleRepository sampleRepository) {
        this.transferRepository = transferRepository;
        this.itemRepository = itemRepository;
        this.receiptRepository = receiptRepository;
        this.sampleRepository = sampleRepository;
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> findAll() {
        return transferRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> findAll(User viewer) {
        return transferRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .filter(transfer -> canView(transfer, viewer))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse findById(Long id) {
        return toResponse(getTransfer(id));
    }

    @Transactional(readOnly = true)
    public TransferResponse findById(Long id, User viewer) {
        SampleTransfer transfer = getTransfer(id);
        if (!canView(transfer, viewer)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu chuyển mẫu");
        }
        return toResponse(transfer);
    }

    @Transactional
    public TransferResponse create(CreateTransferRequest request, User deliveredBy) {
        Receipt receipt = getReceipt(request.receiptId());
        validateRequest(request, receipt);
        Map<Long, Sample> samples = loadSamples(request, receipt);

        SampleTransfer transfer = new SampleTransfer(nextCode(request.transferDate()), receipt, deliveredBy,
                request.receivingDepartment().trim(), request.transferDate(), request.resultsDueDate(),
                request.returnToCustomer(), request.customerReturnDate(), blankToNull(request.changesNote()));
        buildItems(request, samples).forEach(transfer::addItem);
        return toResponse(transferRepository.save(transfer));
    }

    @Transactional
    public TransferResponse update(Long id, CreateTransferRequest request) {
        SampleTransfer transfer = getTransfer(id);
        requireDraft(transfer);
        if (!transfer.getReceipt().getId().equals(request.receiptId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể đổi phiếu tiếp nhận của bản nháp chuyển mẫu");
        }
        validateRequest(request, transfer.getReceipt());
        Map<Long, Sample> samples = loadSamples(request, transfer.getReceipt());
        transfer.updateDraft(request.receivingDepartment().trim(), request.transferDate(),
                request.resultsDueDate(), request.returnToCustomer(), request.customerReturnDate(),
                blankToNull(request.changesNote()));
        transfer.replaceItems(buildItems(request, samples));
        return toResponse(transferRepository.save(transfer));
    }

    @Transactional
    public TransferResponse submit(Long id) {
        SampleTransfer transfer = getTransfer(id);
        requireDraft(transfer);
        if (transfer.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phiếu chuyển mẫu phải có ít nhất một mẫu trước khi gửi");
        }

        for (SampleTransferItem item : transfer.getItems()) {
            Sample sample = item.getSample();
            if (sample.getStatus() != ReceiptStatus.RECEIVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mẫu " + sample.getSampleCode() + " không còn sẵn sàng để chuyển");
            }
            if (itemRepository.existsBySample_IdAndTransfer_StatusIn(sample.getId(), ACTIVE_TRANSFER_STATUSES)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mẫu " + sample.getSampleCode() + " đang có phiếu chuyển hiệu lực");
            }
        }

        transfer.getItems().forEach(item -> item.getSample().markTransferPending());
        transfer.submit();
        return toResponse(transferRepository.save(transfer));
    }

    @Transactional
    public TransferResponse decide(Long id, TransferDecisionRequest request, User receiver) {
        SampleTransfer transfer = getTransfer(id);
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu chuyển mẫu không ở trạng thái chờ nhận");
        }
        if (request.decision() != TransferStatus.ACCEPTED && request.decision() != TransferStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quyết định chỉ có thể là ACCEPTED hoặc REJECTED");
        }
        if (request.decision() == TransferStatus.REJECTED && isBlank(request.receivingNote())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do từ chối nhận mẫu");
        }
        if (request.labRetain() && request.labRetentionUntil() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần nhập ngày lưu mẫu tại phòng phân tích");
        }

        transfer.decide(request.decision(), receiver, blankToNull(request.receivingNote()),
                blankToNull(request.labOpinion()), request.labRetain(), request.labRetentionUntil());
        if (request.decision() == TransferStatus.ACCEPTED) {
            transfer.getItems().forEach(item -> item.getSample().markTransferred());
            if (transfer.getReceipt().getSamples().stream()
                    .allMatch(sample -> sample.getStatus() == ReceiptStatus.TRANSFERRED)) {
                transfer.getReceipt().markTransferred();
            }
        } else {
            transfer.getItems().forEach(item -> item.getSample().markTransferRejected());
        }
        return toResponse(transferRepository.save(transfer));
    }

    private Receipt getReceipt(Long id) {
        Receipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu tiếp nhận"));
        if (receipt.getStatus() == ReceiptStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phiếu đã từ chối không thể chuyển mẫu");
        }
        return receipt;
    }

    private void validateRequest(CreateTransferRequest request, Receipt receipt) {
        if (receipt.getStatus() == ReceiptStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phiếu đã từ chối không thể chuyển mẫu");
        }
        if (request.resultsDueDate().isBefore(request.transferDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hạn trả kết quả phải từ ngày chuyển mẫu trở đi");
        }
        if (request.returnToCustomer() && request.customerReturnDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cần nhập ngày lưu/trả mẫu cho khách hàng");
        }
        if (request.returnToCustomer() && request.customerReturnDate().isBefore(request.transferDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày trả mẫu không được trước ngày chuyển mẫu");
        }
    }

    private Map<Long, Sample> loadSamples(CreateTransferRequest request, Receipt receipt) {
        Set<Long> requestedIds = request.items().stream().map(CreateTransferItemRequest::sampleId)
                .collect(Collectors.toCollection(HashSet::new));
        if (requestedIds.size() != request.items().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách mẫu bị trùng");
        }
        Map<Long, Sample> samples = sampleRepository.findAllById(requestedIds).stream()
                .collect(Collectors.toMap(Sample::getId, Function.identity()));
        if (samples.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Có mẫu không tồn tại");
        }
        for (Sample sample : samples.values()) {
            if (!sample.getReceipt().getId().equals(receipt.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mẫu không thuộc phiếu tiếp nhận đã chọn");
            }
            if (sample.getStatus() != ReceiptStatus.RECEIVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mẫu " + sample.getSampleCode() + " chưa sẵn sàng để lập phiếu chuyển");
            }
        }
        return samples;
    }

    private List<SampleTransferItem> buildItems(CreateTransferRequest request, Map<Long, Sample> samples) {
        List<SampleTransferItem> items = new ArrayList<>();
        for (CreateTransferItemRequest itemRequest : request.items()) {
            Sample sample = samples.get(itemRequest.sampleId());
            items.add(new SampleTransferItem(sample, blankToNull(itemRequest.weight()),
                    defaultValue(itemRequest.quantity(), sample.getQuantity()),
                    defaultValue(itemRequest.sampleCondition(), sample.getCondition()),
                    defaultValue(itemRequest.requestedParameters(), sample.getRequestedParameters()),
                    itemRequest.testMethod().trim()));
        }
        return items;
    }

    private void requireDraft(SampleTransfer transfer) {
        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ được sửa hoặc gửi phiếu chuyển mẫu đang ở bản nháp");
        }
    }

    private boolean canView(SampleTransfer transfer, User viewer) {
        return viewer.getRole() != Role.TECHNICIAN || transfer.getStatus() != TransferStatus.DRAFT;
    }

    private SampleTransfer getTransfer(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu chuyển mẫu"));
    }

    private String nextCode(LocalDate transferDate) {
        return "CM-%s-%s".formatted(transferDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
    }

    private TransferResponse toResponse(SampleTransfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getTransferCode(), transfer.getReceipt().getId(),
                transfer.getReceipt().getReceiptCode(), transfer.getReceipt().getCustomer().getName(),
                transfer.getReceivingDepartment(), transfer.getDeliveredBy().getDisplayName(),
                transfer.getReceivedBy() == null ? null : transfer.getReceivedBy().getDisplayName(),
                transfer.getTransferDate(), transfer.getResultsDueDate(), transfer.isReturnToCustomer(),
                transfer.getCustomerReturnDate(), transfer.getChangesNote(), transfer.getStatus(),
                transfer.getReceivingNote(), transfer.getLabOpinion(), transfer.isLabRetain(),
                transfer.getLabRetentionUntil(), transfer.getSubmittedAt(), transfer.getReceivedAt(),
                transfer.getCreatedAt(), transfer.getItems().stream().map(this::toItemResponse).toList());
    }

    private TransferItemResponse toItemResponse(SampleTransferItem item) {
        return new TransferItemResponse(item.getId(), item.getSample().getId(), item.getSampleName(),
                item.getSampleCode(), item.getWeight(), item.getQuantity(), item.getSampleCondition(),
                item.getRequestedParameters(), item.getTestMethod());
    }

    private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String defaultValue(String value, String fallback) { return isBlank(value) ? fallback : value.trim(); }
}
