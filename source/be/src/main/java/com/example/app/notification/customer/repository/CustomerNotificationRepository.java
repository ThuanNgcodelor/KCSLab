package com.example.app.notification.customer.repository;

import com.example.app.notification.customer.provider.CustomerMessageProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** JDBC participates in the existing JPA transaction; network IO is always outside these transactions. */
@Repository
public class CustomerNotificationRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private static final String SELECT_JOB = """
            SELECT o.*, c.recipient_phone FROM customer_notification_outbox o
            JOIN receipt_notification_consents c ON c.receipt_id = o.receipt_id
            """;
    private static final RowMapper<Job> JOB = (row, index) -> new Job(row.getLong("id"), row.getLong("receipt_id"),
            row.getString("provider_mode"), row.getString("app_id"), row.getString("oa_id"), row.getString("template_id"),
            row.getString("template_data"), row.getString("recipient_phone"), row.getString("request_number"),
            row.getString("status"), row.getString("reason_code"), row.getInt("attempt_count"),
            row.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC), row.getString("session_key"));

    public CustomerNotificationRepository(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
    }

    public void record(long receiptId, String phone, String consentVersion, long actorId, String mode, String sessionKey, String appId,
                       String oaId, String templateId, String templateVersion, String requestNumber, String payload,
                       String status, String reason) {
        jdbc.update("""
                INSERT INTO receipt_notification_consents
                (receipt_id, recipient_phone, consent_accepted, consent_version, actor_user_id, recorded_at)
                VALUES (?, ?, true, ?, ?, UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE id = id
                """, receiptId, phone, consentVersion, actorId);
        jdbc.update("""
                INSERT INTO customer_notification_outbox
                (receipt_id, event_key, event_type, provider_mode, session_key, app_id, oa_id, template_id, template_version,
                 request_number, template_data, status, reason_code, next_attempt_at, created_at, updated_at)
                VALUES (?, ?, 'INTAKE_RECORDED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE id = id
                """, receiptId, "INTAKE_RECORDED:" + receiptId, mode, sessionKey, appId, oaId, templateId, templateVersion,
                requestNumber, payload, status, reason);
    }

    public List<Job> jobs(long receiptId, int page, int size) {
        return jdbc.query(SELECT_JOB + " WHERE o.receipt_id = ? ORDER BY o.id DESC LIMIT ? OFFSET ?", JOB, receiptId, size, (long) page * size);
    }

    public Job find(long id) {
        var rows = jdbc.query(SELECT_JOB + " WHERE o.id = ?", JOB, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public long count(long receiptId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM customer_notification_outbox WHERE receipt_id = ?", Long.class, receiptId);
    }

    public boolean hasDeliveredAttempt(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM notification_delivery_attempts WHERE outbox_id=? AND status='DELIVERED')", Boolean.class, id));
    }

    public List<Attempt> attempts(long outboxId) {
        return jdbc.query("""
                SELECT id, attempt_number, tracking_id, provider_mode, status, provider_code, provider_message_id,
                       created_at, completed_at, delivered_at, delivery_source, status_check_count, last_status_check_at, last_status_code
                FROM notification_delivery_attempts WHERE outbox_id = ? ORDER BY id
                """, (row, i) -> new Attempt(row.getLong("id"), row.getInt("attempt_number"), row.getString("tracking_id"),
                row.getString("provider_mode"), row.getString("status"), row.getString("provider_code"), row.getString("provider_message_id"),
                row.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC), row.getObject("completed_at", LocalDateTime.class) == null
                        ? null : row.getObject("completed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                row.getObject("delivered_at", LocalDateTime.class) == null ? null : row.getObject("delivered_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                row.getString("delivery_source"), row.getInt("status_check_count"),
                row.getObject("last_status_check_at", LocalDateTime.class) == null ? null : row.getObject("last_status_check_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                row.getString("last_status_code")), outboxId);
    }

    public List<Long> due(String mode) {
        return jdbc.queryForList("""
                SELECT id FROM customer_notification_outbox
                WHERE provider_mode = ? AND status IN ('QUEUED', 'RETRY_WAIT') AND next_attempt_at <= UTC_TIMESTAMP(6)
                ORDER BY id LIMIT 10
                """, Long.class, mode);
    }

    public Claimed claim(long id, String mode, String sessionKey, int maxCalls) {
        return tx.execute(ignored -> {
            var rows = jdbc.query(SELECT_JOB + " WHERE o.id = ? AND o.next_attempt_at <= UTC_TIMESTAMP(6) FOR UPDATE", JOB, id);
            if (rows.isEmpty()) return null;
            Job job = rows.getFirst();
            if (!job.mode().equals(mode) || !List.of("QUEUED", "RETRY_WAIT").contains(job.status())) return null;
            if (job.attemptCount() >= 5) {
                block(id, "ATTEMPT_LIMIT");
                return null;
            }
            if (mode.equals("ZALO_DEVELOPMENT") || mode.equals("ZALO_LIVE")) {
                jdbc.update("INSERT INTO customer_notification_test_budgets(session_key) VALUES (?) ON DUPLICATE KEY UPDATE session_key = session_key", sessionKey);
                if (jdbc.update("UPDATE customer_notification_test_budgets SET used_calls = used_calls + 1 WHERE session_key = ? AND used_calls < ?",
                        sessionKey, maxCalls) == 0) {
                    block(id, "TEST_CALL_LIMIT");
                    return null;
                }
            }
            String owner = UUID.randomUUID().toString();
            String tracking = (mode.equals("ZALO_LIVE") ? "CFCLIVE" : "CFCDEV") + UUID.randomUUID().toString().replace("-", "");
            String hash = sha256(job.templateId() + "|" + job.phone() + "|" + job.payload());
            jdbc.update("""
                    UPDATE customer_notification_outbox SET status = 'SENDING', reason_code = NULL,
                    attempt_count = attempt_count + 1, lease_owner = ?, lease_until = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 60 SECOND),
                    updated_at = UTC_TIMESTAMP(6) WHERE id = ?
                    """, owner, id);
            jdbc.update("""
                    INSERT INTO notification_delivery_attempts
                    (outbox_id, attempt_number, tracking_id, session_key, provider_mode, status, request_hash,
                     retry_request_id, actor_user_id, reason, created_at)
                    SELECT id, attempt_count, ?, ?, provider_mode, 'SENDING', ?, retry_request_id, retry_actor_id, retry_reason,
                    UTC_TIMESTAMP(6) FROM customer_notification_outbox WHERE id = ?
                    """, tracking, sessionKey, hash, id);
            return new Claimed(job, owner, tracking);
        });
    }

    public void finish(Claimed claim, CustomerMessageProvider.Outcome outcome) {
        tx.executeWithoutResult(ignored -> {
            int changed = jdbc.update("""
                    UPDATE customer_notification_outbox SET status = ?, reason_code = ?, lease_owner = NULL,
                    lease_until = NULL, next_attempt_at = DATE_ADD(UTC_TIMESTAMP(6), INTERVAL ? SECOND),
                    updated_at = UTC_TIMESTAMP(6) WHERE id = ? AND status = 'SENDING' AND lease_owner = ?
                    """, outcome.status(), outcome.code(), retryDelay(claim, outcome), claim.job().id(), claim.owner());
            if (changed == 1) jdbc.update("""
                    UPDATE notification_delivery_attempts SET status = ?, provider_code = ?, provider_message_id = ?,
                    completed_at = UTC_TIMESTAMP(6) WHERE tracking_id = ? AND status = 'SENDING'
                    """, outcome.status(), outcome.code(), outcome.messageId(), claim.trackingId());
        });
    }

    private long retryDelay(Claimed claim, CustomerMessageProvider.Outcome outcome) {
        if (claim.job().mode().equals("ZALO_LIVE") && outcome.status().equals("RETRY_WAIT") && !List.of("TOKEN_NOT_READY", "SENDING_PAUSED").contains(outcome.code())) {
            // Quota exhaustion is an explicit rejection; wait rather than repeatedly calling a depleted account.
            return switch (outcome.code()) {
                case "-144", "-147" -> 86400;
                case "-115" -> 3600;
                default -> 3600;
            };
        }
        return Math.min(3600, 30L << Math.min(claim.job().attemptCount(), 6));
    }

    public void recoverExpired() {
        tx.executeWithoutResult(ignored -> {
            jdbc.update("""
                    UPDATE notification_delivery_attempts a JOIN customer_notification_outbox o ON o.id = a.outbox_id
                    SET a.status = 'UNKNOWN', a.provider_code = 'LEASE_EXPIRED', a.completed_at = UTC_TIMESTAMP(6)
                    WHERE o.status = 'SENDING' AND o.lease_until < UTC_TIMESTAMP(6) AND a.status = 'SENDING'
                    """);
            jdbc.update("""
                    UPDATE customer_notification_outbox SET status = 'UNKNOWN', reason_code = 'LEASE_EXPIRED', lease_owner = NULL,
                    lease_until = NULL, updated_at = UTC_TIMESTAMP(6) WHERE status = 'SENDING' AND lease_until < UTC_TIMESTAMP(6)
                    """);
        });
    }

    public void block(long id, String reason) {
        jdbc.update("UPDATE customer_notification_outbox SET status = 'BLOCKED', reason_code = ?, updated_at = UTC_TIMESTAMP(6) WHERE id = ? AND status IN ('QUEUED', 'RETRY_WAIT')", reason, id);
    }

    public Job lock(long receiptId, long id) {
        var rows = jdbc.query(SELECT_JOB + " WHERE o.id = ? AND o.receipt_id = ? FOR UPDATE", JOB, id, receiptId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public boolean hasRetry(long id, String requestId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM notification_delivery_attempts WHERE outbox_id = ? AND retry_request_id = ?)
                    OR EXISTS(SELECT 1 FROM customer_notification_outbox WHERE id = ? AND retry_request_id = ?)
                """, Boolean.class, id, requestId, id, requestId));
    }

    public void queueRetry(long id, String requestId, String reason, long actorId) {
        jdbc.update("""
                UPDATE customer_notification_outbox SET status = 'QUEUED', reason_code = NULL, retry_request_id = ?,
                retry_reason = ?, retry_actor_id = ?, next_attempt_at = UTC_TIMESTAMP(6), updated_at = UTC_TIMESTAMP(6) WHERE id = ?
                """, requestId, reason, actorId, id);
    }

    public static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public record Job(long id, long receiptId, String mode, String appId, String oaId, String templateId, String payload,
                      String phone, String requestNumber, String status, String reason, int attemptCount, Instant createdAt, String sessionKey) {
        @Override public String toString() { return "CustomerNotificationJob[id=" + id + ", status=" + status + "]"; }
    }
    public record Claimed(Job job, String owner, String trackingId) {}
    public record Attempt(long id, int number, String trackingId, String mode, String status, String providerCode,
                          String providerMessageId, Instant createdAt, Instant completedAt, Instant deliveredAt, String deliverySource,
                          int statusCheckCount, Instant lastStatusCheckAt, String lastStatusCode) {}
}
