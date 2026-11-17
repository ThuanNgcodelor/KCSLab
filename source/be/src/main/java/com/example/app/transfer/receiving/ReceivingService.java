package com.example.app.transfer.receiving;

import com.example.app.intake.dto.ReceiptAttachmentContent;
import com.example.app.intake.service.ReceiptService;
import com.example.app.transfer.dto.TransferDecisionRequest;
import com.example.app.transfer.dto.TransferResponse;
import com.example.app.transfer.entity.SampleTransfer;
import com.example.app.transfer.entity.SampleTransferItem;
import com.example.app.transfer.entity.TransferStatus;
import com.example.app.transfer.repository.SampleTransferRepository;
import com.example.app.transfer.service.TransferService;
import com.example.app.user.entity.User;
import com.example.app.common.dto.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReceivingService {
    private final TransferService transfers;
    private final ReceiptService receipts;
    private final SampleTransferRepository repository;
    private final JdbcTemplate jdbc;
    private final com.example.app.intake.service.SampleCodingService coding;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> SORT_FIELDS = Set.of("submittedAt", "receivedAt", "transferCode", "resultsDueDate");

    public ReceivingService(TransferService transfers, ReceiptService receipts,
                            SampleTransferRepository repository, JdbcTemplate jdbc,
                            com.example.app.intake.service.SampleCodingService coding) {
        this.transfers = transfers;
        this.receipts = receipts;
        this.repository = repository;
        this.jdbc = jdbc;
        this.coding = coding;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReceivingTransferResponse> list(User viewer, int page, int size,
            TransferStatus status, String q, LocalDate from, LocalDate to, String sort) {
        if (page < 0 || size < 1 || size > 50 || status == TransferStatus.DRAFT ||
                (from != null && to != null && from.isAfter(to))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bộ lọc hoặc phân trang không hợp lệ");
        }
        String rawSort = sort == null || sort.isBlank()
                ? (status == TransferStatus.PENDING ? "submittedAt,asc" : "submittedAt,desc") : sort;
        String[] parts = rawSort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0]) ||
                !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sắp xếp không hợp lệ");
        }
        Sort.Direction direction = Sort.Direction.fromString(parts[1]);
        var pageable = PageRequest.of(page, size, Sort.by(direction, parts[0]).and(Sort.by(direction, "id")));
        Specification<SampleTransfer> spec = (root, query, builder) -> {
            var predicate = builder.notEqual(root.get("status"), TransferStatus.DRAFT);
            predicate = builder.and(predicate, builder.isNotNull(root.get("submittedAt")));
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            if (from != null) predicate = builder.and(predicate,
                    builder.greaterThanOrEqualTo(root.get("submittedAt"), from.atStartOfDay(ZONE).toInstant()));
            if (to != null) predicate = builder.and(predicate,
                    builder.lessThan(root.get("submittedAt"), to.plusDays(1).atStartOfDay(ZONE).toInstant()));
            if (q != null && !q.isBlank()) {
                String escaped = q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                var subquery = query.subquery(Long.class);
                var item = subquery.from(SampleTransferItem.class);
                subquery.select(item.get("id")).where(builder.equal(item.get("transfer"), root),
                        builder.like(builder.lower(item.get("sampleCode")), pattern, '\\'));
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("transferCode")), pattern, '\\'),
                        builder.like(builder.lower(root.get("receipt").get("receiptCode")), pattern, '\\'),
                        builder.exists(subquery)));
            }
            return predicate;
        };
        var result = repository.findAll(spec, pageable)
                .map(transfer -> ReceivingTransferResponse.from(transfers.findById(transfer.getId(), viewer)));
        return PagedResponse.from(result, parts[0] + "," + direction.name().toLowerCase(Locale.ROOT));
    }

    public record Trend(LocalDate date, long submitted, long accepted, long rejected) {}
    public record Dashboard(LocalDate from, LocalDate to, long submittedTransfers, long submittedSamples,
                            long pendingNow, long acceptedTransfers, long rejectedTransfers,
                            Double averageProcessingSeconds, List<Trend> trend,
                            List<ReceivingTransferResponse> pendingQueue,
                            List<ReceivingTransferResponse> recentDecisions) {}

    @Transactional(readOnly = true)
    public Dashboard dashboard(User viewer, LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate to = requestedTo == null ? LocalDate.now(ZONE) : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(6) : requestedFrom;
        if (from.isAfter(to) || from.plusDays(365).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng ngày không hợp lệ (tối đa 366 ngày)");
        }
        Instant start = from.atStartOfDay(ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZONE).toInstant();
        var submitted = countRange("submitted_at", null, start, end);
        Long samples = jdbc.queryForObject("select count(*) from sample_transfer_items i join sample_transfers t "
                + "on t.id=i.transfer_id where t.submitted_at >= ? and t.submitted_at < ? and t.status <> 'DRAFT'",
                Long.class, Timestamp.from(start), Timestamp.from(end));
        Long pending = jdbc.queryForObject("select count(*) from sample_transfers where status='PENDING'", Long.class);
        long accepted = countRange("received_at", "ACCEPTED", start, end);
        long rejected = countRange("received_at", "REJECTED", start, end);
        Double average = jdbc.queryForObject("select avg(timestampdiff(MICROSECOND, submitted_at, received_at))/1000000.0 "
                + "from sample_transfers where status in ('ACCEPTED','REJECTED') and received_at >= ? and received_at < ?",
                Double.class, Timestamp.from(start), Timestamp.from(end));
        // Việt Nam does not observe daylight saving time: each business day is
        // exactly 24 hours from its UTC boundary. Group in SQL, not one query per day.
        Map<Integer, long[]> buckets = new HashMap<>();
        jdbc.query("select timestampdiff(DAY, ?, submitted_at) as day_offset, count(*) as total "
                        + "from sample_transfers where submitted_at >= ? and submitted_at < ? and status <> 'DRAFT' "
                        + "group by day_offset", rs -> {
                    buckets.computeIfAbsent(rs.getInt("day_offset"), ignored -> new long[3])[0] = rs.getLong("total");
                }, Timestamp.from(start), Timestamp.from(start), Timestamp.from(end));
        jdbc.query("select timestampdiff(DAY, ?, received_at) as day_offset, status, count(*) as total "
                        + "from sample_transfers where received_at >= ? and received_at < ? "
                        + "and status in ('ACCEPTED','REJECTED') group by day_offset, status", rs -> {
                    long[] bucket = buckets.computeIfAbsent(rs.getInt("day_offset"), ignored -> new long[3]);
                    bucket["ACCEPTED".equals(rs.getString("status")) ? 1 : 2] = rs.getLong("total");
                }, Timestamp.from(start), Timestamp.from(start), Timestamp.from(end));
        List<Trend> trend = new ArrayList<>();
        for (int dayOffset = 0; !from.plusDays(dayOffset).isAfter(to); dayOffset++) {
            long[] counts = buckets.getOrDefault(dayOffset, new long[3]);
            trend.add(new Trend(from.plusDays(dayOffset), counts[0], counts[1], counts[2]));
        }
        var pendingQueue = list(viewer, 0, 5, TransferStatus.PENDING, null, null, null, "submittedAt,asc").content();
        var recentDecisions = list(viewer, 0, 5, null, null, null, null, "receivedAt,desc").content()
                .stream().filter(item -> item.receivedAt() != null).toList();
        return new Dashboard(from, to, submitted, samples == null ? 0 : samples, pending == null ? 0 : pending,
                accepted, rejected, average, trend, pendingQueue, recentDecisions);
    }

    private long countRange(String column, String status, Instant from, Instant to) {
        String sql = "select count(*) from sample_transfers where " + column + " >= ? and " + column + " < ?"
                + (status == null ? " and status <> 'DRAFT'" : " and status = ?");
        Long count = status == null ? jdbc.queryForObject(sql, Long.class, Timestamp.from(from), Timestamp.from(to))
                : jdbc.queryForObject(sql, Long.class, Timestamp.from(from), Timestamp.from(to), status);
        return count == null ? 0 : count;
    }

    @Transactional(readOnly = true)
    public ReceivingTransferResponse detail(Long id, User viewer) {
        return ReceivingTransferResponse.from(visibleTransfer(id, viewer));
    }

    @Transactional(readOnly = true)
    public List<ReceivingEvidenceResponse> evidence(Long id, User viewer) {
        TransferResponse transfer = visibleTransfer(id, viewer);
        return receipts.findById(transfer.receiptId()).attachments().stream()
                .map(attachment -> ReceivingEvidenceResponse.from(id, attachment)).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptAttachmentContent content(Long id, Long attachmentId, User viewer) {
        TransferResponse transfer = visibleTransfer(id, viewer);
        return receipts.loadAttachment(transfer.receiptId(), attachmentId);
    }

    @Transactional
    public ReceivingTransferResponse decide(Long id, TransferDecisionRequest request, User viewer) {
        // Do not read the entity before the locking query: a stale managed entity
        // could otherwise survive while another transaction commits its decision.
        return ReceivingTransferResponse.from(transfers.decide(id, request, viewer));
    }

    @Transactional
    public ReceivingTransferResponse verifyCoding(Long id, Long sampleId, User viewer) {
        SampleTransfer transfer = repository.findByIdForDecision(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu chuyển mẫu"));
        if (transfer.getStatus() != TransferStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần xác nhận nhận bàn giao trước khi đối chiếu mã");
        }
        if (transfer.getItems().stream().noneMatch(item -> item.getSample().getId().equals(sampleId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mẫu không thuộc phiếu chuyển này");
        }
        coding.verifyAtKcs(sampleId, viewer);
        return ReceivingTransferResponse.from(transfers.findById(id, viewer));
    }

    private TransferResponse visibleTransfer(Long id, User viewer) {
        TransferResponse transfer = transfers.findById(id, viewer);
        // KCS workspace never exposes drafts, including when accessed by an administrator.
        if (transfer.submittedAt() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu chuyển mẫu");
        }
        return transfer;
    }
}
