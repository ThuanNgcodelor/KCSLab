package com.example.app.intake.service;

import com.example.app.intake.dto.IntakeDashboardReceiptResponse;
import com.example.app.intake.dto.IntakeDashboardResponse;
import com.example.app.intake.dto.ReceiptResponse;
import com.example.app.intake.dto.ReceiptSearchCriteria;
import com.example.app.intake.entity.AcceptanceDecision;
import com.example.app.intake.entity.Receipt;
import com.example.app.intake.entity.ReceiptStatus;
import com.example.app.intake.entity.Sample;
import com.example.app.intake.repository.ReceiptRepository;
import com.example.app.intake.repository.ReceiptSpecifications;
import com.example.app.intake.repository.SampleRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class IntakeDashboardService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReceiptRepository receiptRepository;
    private final SampleRepository sampleRepository;
    private final ReceiptService receiptService;

    public IntakeDashboardService(ReceiptRepository receiptRepository,
                                  SampleRepository sampleRepository,
                                  ReceiptService receiptService) {
        this.receiptRepository = receiptRepository;
        this.sampleRepository = sampleRepository;
        this.receiptService = receiptService;
    }

    @Transactional(readOnly = true)
    public IntakeDashboardResponse summarize(LocalDate requestedFrom, LocalDate requestedTo, Long receivedBy) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate from = requestedFrom == null ? today.minusDays(6) : requestedFrom;
        LocalDate to = requestedTo == null ? today : requestedTo;
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được sau ngày kết thúc");
        }

        ReceiptSearchCriteria criteria = new ReceiptSearchCriteria(
                null, from, to, null, null, null, receivedBy, false);
        Specification<Receipt> base = ReceiptSpecifications.byCriteria(criteria);
        long totalReceipts = receiptRepository.count(base);
        long totalSamples = sampleRepository.count(sampleRange(from, to, receivedBy));
        long accepted = receiptRepository.count(base.and(decision(AcceptanceDecision.ACCEPTED)));
        long conditional = receiptRepository.count(base.and(decision(AcceptanceDecision.ACCEPTED_WITH_CONDITION)));
        long rejected = receiptRepository.count(base.and(decision(AcceptanceDecision.REJECTED)));
        long awaitingTransfer = receiptRepository.count(base.and((root, query, builder) ->
                root.get("status").in(ReceiptStatus.RECEIVED, ReceiptStatus.ACCEPTED_WITH_CONDITION)));

        var recentReceipts = receiptService.findAll(criteria, 0, 5, "receivedAt,desc")
                .content().stream().map(this::toDashboardReceipt).toList();
        return new IntakeDashboardResponse(from, to, totalReceipts, totalSamples, accepted,
                conditional, rejected, awaitingTransfer, recentReceipts);
    }

    private Specification<Receipt> decision(AcceptanceDecision decision) {
        return (root, query, builder) -> builder.equal(root.get("acceptanceDecision"), decision);
    }

    private Specification<Sample> sampleRange(LocalDate from, LocalDate to, Long receivedBy) {
        Instant fromInstant = from.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        return (root, query, builder) -> {
            var receipt = root.join("receipt");
            var range = builder.and(
                    builder.greaterThanOrEqualTo(receipt.get("receivedAt"), fromInstant),
                    builder.lessThan(receipt.get("receivedAt"), toExclusive));
            return receivedBy == null ? range : builder.and(range,
                    builder.equal(receipt.get("receivedBy").get("id"), receivedBy));
        };
    }

    private IntakeDashboardReceiptResponse toDashboardReceipt(ReceiptResponse receipt) {
        return new IntakeDashboardReceiptResponse(receipt.id(), receipt.receiptCode(), receipt.customerName(),
                receipt.sampleName(), receipt.samples().size(), receipt.receivedByName(), receipt.receivedAt(),
                receipt.expectedDate(), receipt.status());
    }
}
