package com.example.app.notification.customer.service;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.repository.CustomerNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class CustomerNotificationOperations {
    private static final Set<String> STATUSES = Set.of("QUEUED", "SENDING", "RETRY_WAIT", "ACCEPTED", "DELIVERED", "SIMULATED", "UNKNOWN", "BLOCKED", "FAILED_FINAL", "CANCELLED");
    private final JdbcTemplate jdbc;
    private final CustomerNotificationRepository repository;
    private final CustomerNotificationProperties properties;
    private final ZaloTokenService tokens;
    public CustomerNotificationOperations(JdbcTemplate jdbc, CustomerNotificationRepository repository, CustomerNotificationProperties properties, ZaloTokenService tokens) {
        this.jdbc = jdbc; this.repository = repository; this.properties = properties; this.tokens = tokens;
    }

    public boolean paused() { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT paused FROM customer_notification_controls WHERE id=1", Boolean.class)); }

    @Transactional(readOnly = true)
    public Overview overview() {
        var counts = new LinkedHashMap<String, Long>();
        jdbc.query("SELECT status,COUNT(*) FROM customer_notification_outbox GROUP BY status", row -> { counts.put(row.getString(1), row.getLong(2)); });
        List<String> warnings = new ArrayList<>();
        var health = tokens.health();
        if (properties.getMode().name().startsWith("ZALO")) {
            if (!health.state().equals("READY")) warnings.add("TOKEN_" + health.state());
            if (health.accessExpiresAt() != null && health.accessExpiresAt().isBefore(Instant.now().plusSeconds(1800))) warnings.add("TOKEN_EXPIRING");
            if (health.refreshExpiresAt() != null && health.refreshExpiresAt().isBefore(Instant.now().plusSeconds(604800))) warnings.add("REFRESH_TOKEN_EXPIRING");
            if (!properties.isPollingEnabled() && !properties.isWebhookEnabled()) warnings.add("DELIVERY_TRACKING_DISABLED");
        }
        long quotaErrors = jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_outbox WHERE reason_code IN ('-115','-126','-137','-144','-147','TEST_CALL_LIMIT')", Long.class);
        if (quotaErrors > 0) warnings.add("QUOTA_OR_BALANCE_ERRORS");
        long tokenErrors = jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_outbox WHERE reason_code='-124'", Long.class);
        if (tokenErrors > 0) warnings.add("TOKEN_REJECTED_BY_PROVIDER");
        if (counts.getOrDefault("UNKNOWN", 0L) > 0) warnings.add("UNKNOWN_REQUIRES_RECONCILIATION");
        long unmatched = jdbc.queryForObject("SELECT COUNT(*) FROM zalo_delivery_inbox WHERE status IN ('UNMATCHED','REJECTED')", Long.class);
        if (unmatched > 0) warnings.add("WEBHOOK_REQUIRES_REVIEW");
        var budget = jdbc.queryForList("SELECT used_calls FROM customer_notification_test_budgets WHERE session_key=?", Integer.class, properties.getSessionKey());
        return new Overview(properties.getMode().name(), paused(), counts, List.copyOf(warnings), health,
                budget.isEmpty() ? 0 : budget.getFirst(), properties.getMaxCallsPerSession(), unmatched);
    }

    @Transactional(readOnly = true)
    public QueuePage queue(String status, String query, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || status != null && !STATUSES.contains(status) || query != null && query.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bộ lọc không hợp lệ");
        }
        String where = " WHERE (? IS NULL OR status=?) AND (? IS NULL OR LOCATE(?,request_number)>0)";
        var ids = jdbc.queryForList("SELECT id FROM customer_notification_outbox" + where + " ORDER BY id DESC LIMIT ? OFFSET ?", Long.class,
                status, status, query, query, size, (long) page * size);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_outbox" + where, Long.class, status, status, query, query);
        var rows = ids.stream().map(repository::find).map(job -> new QueueItem(job.id(), job.receiptId(), job.requestNumber(),
                RecipientPhone.mask(job.phone()), job.mode(), job.status(), job.reason(), job.attemptCount(), job.createdAt())).toList();
        int pages = (int) ((total + size - 1) / size);
        return new QueuePage(rows, page, size, total, pages, page == 0, page + 1 >= pages, "createdAt,desc");
    }

    @Transactional
    public void pause(boolean paused, String requestId, String reason, long actorId) {
        lockControls();
        if (!audit(requestId, paused ? "PAUSE" : "RESUME", reason, Boolean.toString(paused), actorId)) return;
        jdbc.update("UPDATE customer_notification_controls SET paused=?,updated_at=UTC_TIMESTAMP(6) WHERE id=1", paused);
    }

    @Transactional
    public void provision(String access, String refresh, long expires, String requestId, String reason, long actorId) {
        lockControls();
        String fingerprint = CustomerNotificationRepository.sha256(tokens.scope() + "|" + access + "|" + refresh + "|" + expires);
        if (!audit(requestId, "PROVISION_TOKEN", reason, fingerprint, actorId)) return;
        try { tokens.provision(access, refresh, expires); }
        catch (IllegalStateException exception) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Chưa thể cập nhật token; kiểm tra cấu hình hoặc lượt refresh đang chạy"); }
    }

    private void lockControls() { jdbc.queryForList("SELECT id FROM customer_notification_controls WHERE id=1 FOR UPDATE"); }

    private boolean audit(String requestId, String action, String reason, String payload, long actorId) {
        String hash = CustomerNotificationRepository.sha256(action + "|" + payload + "|" + reason + "|" + actorId);
        var existing = jdbc.queryForList("SELECT request_hash FROM customer_notification_operation_audit WHERE request_id=?", String.class, requestId);
        if (!existing.isEmpty()) {
            if (!existing.getFirst().equals(hash)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã yêu cầu đã được dùng cho thao tác khác");
            return false;
        }
        jdbc.update("INSERT INTO customer_notification_operation_audit(request_id,action,request_hash,actor_user_id,reason,created_at) VALUES (?,?,?,?,?,UTC_TIMESTAMP(6))",
                requestId, action, hash, actorId, reason);
        return true;
    }

    public record Overview(String mode, boolean paused, Map<String, Long> counts, List<String> warnings, ZaloTokenService.Health token,
                           int sessionCalls, int sessionLimit, long unmatchedWebhooks) {}
    public record QueueItem(long id, long receiptId, String requestNumber, String recipientMasked, String mode, String status,
                            String reasonCode, int attempts, Instant createdAt) {}
    public record QueuePage(List<QueueItem> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, String sort) {}
}
