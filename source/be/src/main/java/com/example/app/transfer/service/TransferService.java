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
import com.example.app.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {
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
    public TransferResponse findById(Long id) {
        return toResponse(getTransfer(id));
    }

    @Transactional
    public TransferResponse create(CreateTransferRequest request, User deliveredBy) {
        Receipt receipt = receiptRepository.findById(request.receiptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu tiếp nhận"));
        if (receipt.getStatus() == ReceiptStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu đã từ chối không thể chuyển mẫu");
        }
        if (request.resultsDueDate().isBefore(request.transferDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn trả kết quả phải từ ngày chuyển mẫu trở đi");
        }
        if (request.returnToCustomer() && request.customerReturnDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần nhập ngày lưu/trả mẫu cho khách hàng");
        }

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

        SampleTransfer transfer = new SampleTransfer(nextCode(request.transferDate()), receipt, deliveredBy,
                request.transferDate(), request.resultsDueDate(), request.returnToCustomer(),
                request.customerReturnDate(), blankToNull(request.changesNote()));
        for (CreateTransferItemRequest itemRequest : request.items()) {
            Sample sample = samples.get(itemRequest.sampleId());
            if (!sample.getReceipt().getId().equals(receipt.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mẫu không thuộc phiếu tiếp nhận đã chọn");
            }
            if (sample.getStatus() != ReceiptStatus.RECEIVED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mẫu " + sample.getSampleCode() + " chưa sẵn sàng để chuyển");
            }
            if (itemRepository.existsBySample_IdAndTransfer_StatusNot(sample.getId(), TransferStatus.REJECTED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Mẫu " + sample.getSampleCode() + " đang có phiếu chuyển chưa bị từ chối");
            }
            transfer.addItem(new SampleTransferItem(sample,
                    blankToNull(itemRequest.weight()), defaultValue(itemRequest.quantity(), sample.getQuantity()),
                    defaultValue(itemRequest.sampleCondition(), sample.getCondition()),
                    defaultValue(itemRequest.requestedParameters(), sample.getRequestedParameters()),
                    itemRequest.testMethod().trim()));
            sample.markTransferPending();
        }
        return toResponse(transferRepository.save(transfer));
    }

    @Transactional
    public TransferResponse decide(Long id, TransferDecisionRequest request, User receiver) {
        SampleTransfer transfer = getTransfer(id);
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phiếu chuyển mẫu đã được xử lý");
        }
        if (request.decision() != TransferStatus.ACCEPTED && request.decision() != TransferStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quyết định chỉ có thể là ACCEPTED hoặc REJECTED");
        }
        if (request.decision() == TransferStatus.REJECTED && isBlank(request.receivingNote())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do từ chối nhận mẫu");
        }
        if (request.labRetain() && request.labRetentionUntil() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần nhập ngày lưu mẫu tại phòng phân tích");
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

    private SampleTransfer getTransfer(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu chuyển mẫu"));
    }

    private String nextCode(LocalDate transferDate) {
        return "CM-%s-%s".formatted(transferDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }

    private TransferResponse toResponse(SampleTransfer transfer) {
        return new TransferResponse(transfer.getId(), transfer.getTransferCode(), transfer.getReceipt().getId(),
                transfer.getReceipt().getReceiptCode(), transfer.getReceipt().getCustomer().getName(),
                transfer.getDeliveredBy().getDisplayName(),
                transfer.getReceivedBy() == null ? null : transfer.getReceivedBy().getDisplayName(),
                transfer.getTransferDate(), transfer.getResultsDueDate(), transfer.isReturnToCustomer(),
                transfer.getCustomerReturnDate(), transfer.getChangesNote(), transfer.getStatus(),
                transfer.getReceivingNote(), transfer.getLabOpinion(), transfer.isLabRetain(),
                transfer.getLabRetentionUntil(), transfer.getReceivedAt(), transfer.getCreatedAt(),
                transfer.getItems().stream().map(this::toItemResponse).toList());
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
