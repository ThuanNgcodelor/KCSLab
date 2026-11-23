package com.example.app.notification.customer.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.List;

@Repository
public class ZaloDeliveryRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public ZaloDeliveryRepository(JdbcTemplate jdbc, PlatformTransactionManager manager) { this.jdbc = jdbc; tx = new TransactionTemplate(manager); }

    public void receive(String appId, String oaId, String messageId, String trackingId, String recipientHash, Instant deliveredAt) {
        String key = CustomerNotificationRepository.sha256(appId + "|" + oaId + "|" + messageId + "|" + trackingId + "|" + recipientHash + "|" + deliveredAt);
        jdbc.update("""
                INSERT INTO zalo_delivery_inbox(event_key,app_id,oa_id,message_id,tracking_id,recipient_hash,delivered_at,received_at,next_match_at)
                VALUES (?,?,?,?,?,?,?,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE id=id
                """, key, appId, oaId, messageId, trackingId, recipientHash, LocalDateTime.ofInstant(deliveredAt, ZoneOffset.UTC));
    }

    public void applyPending() {
        var ids = jdbc.queryForList("SELECT id FROM zalo_delivery_inbox WHERE status='PENDING' AND next_match_at<=UTC_TIMESTAMP(6) ORDER BY id LIMIT 30", Long.class);
        for (Long id : ids) tx.executeWithoutResult(transaction -> {
            var events = jdbc.query("SELECT * FROM zalo_delivery_inbox WHERE id=? AND status='PENDING' FOR UPDATE", (r, i) ->
                    new Event(r.getLong("id"), r.getString("app_id"), r.getString("oa_id"), r.getString("message_id"), r.getString("tracking_id"),
                            r.getString("recipient_hash"), r.getObject("delivered_at", LocalDateTime.class).toInstant(ZoneOffset.UTC), r.getInt("match_count")), id);
            if (events.isEmpty()) return;
            Event event = events.getFirst();
            var idsForAttempt = jdbc.queryForList("SELECT outbox_id FROM notification_delivery_attempts WHERE tracking_id=?", Long.class, event.trackingId());
            if (idsForAttempt.isEmpty()) {
                jdbc.update("""
                        UPDATE zalo_delivery_inbox SET match_count=match_count+1,next_match_at=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 60 SECOND),
                        status=IF(match_count>=60,'UNMATCHED','PENDING'),reason_code='AWAITING_ATTEMPT' WHERE id=?
                        """, id);
                return;
            }
            long jobId = idsForAttempt.getFirst();
            var jobs = jdbc.queryForList("SELECT * FROM customer_notification_outbox WHERE id=? FOR UPDATE", jobId);
            var job = jobs.getFirst();
            var attempts = jdbc.queryForList("SELECT * FROM notification_delivery_attempts WHERE tracking_id=? FOR UPDATE", event.trackingId());
            var attempt = attempts.getFirst();
            String phone = jdbc.queryForObject("SELECT recipient_phone FROM receipt_notification_consents WHERE receipt_id=?", String.class, job.get("receipt_id"));
            String expectedMessageId = (String) attempt.get("provider_message_id");
            boolean valid = event.appId().equals(job.get("app_id")) && event.oaId().equals(job.get("oa_id"))
                    && (expectedMessageId == null || expectedMessageId.equals(event.messageId()))
                    && event.recipientHash().equals(CustomerNotificationRepository.sha256(phone))
                    && List.of("ZALO_DEVELOPMENT", "ZALO_LIVE").contains(job.get("provider_mode"))
                    && event.deliveredAt().isAfter(createdAt(((Number) attempt.get("id")).longValue()).minusSeconds(300));
            if (!valid) {
                jdbc.update("UPDATE zalo_delivery_inbox SET status='REJECTED',reason_code='ATTEMPT_MISMATCH' WHERE id=?", id);
                return;
            }
            delivered(jobId, ((Number) attempt.get("id")).longValue(), ((Number) attempt.get("attempt_number")).intValue(),
                    event.messageId(), event.deliveredAt(), "WEBHOOK");
            jdbc.update("UPDATE zalo_delivery_inbox SET status='APPLIED',reason_code=NULL WHERE id=?", id);
        });
    }

    private Instant createdAt(long id) {
        return jdbc.queryForObject("SELECT created_at FROM notification_delivery_attempts WHERE id=?",
                (r, i) -> r.getObject(1, LocalDateTime.class).toInstant(ZoneOffset.UTC), id);
    }

    /** Caller holds the outbox lock; a late event updates its own attempt only. */
    private void delivered(long jobId, long attemptId, int number, String messageId, Instant when, String source) {
        jdbc.update("""
                UPDATE notification_delivery_attempts SET status='DELIVERED',provider_message_id=?,delivered_at=COALESCE(delivered_at,?),
                delivery_source=COALESCE(delivery_source,?),completed_at=COALESCE(completed_at,UTC_TIMESTAMP(6)) WHERE id=?
                """, messageId, LocalDateTime.ofInstant(when, ZoneOffset.UTC), source, attemptId);
        jdbc.update("""
                UPDATE customer_notification_outbox SET status='DELIVERED',reason_code=NULL,lease_owner=NULL,lease_until=NULL,updated_at=UTC_TIMESTAMP(6)
                WHERE id=? AND attempt_count=? AND status<>'DELIVERED'
                """, jobId, number);
    }

    public List<Check> dueChecks(String appId, String oaId, String mode, int maxChecks) {
        return jdbc.query("""
                SELECT a.id,a.outbox_id,a.attempt_number,a.provider_message_id,a.status_check_count,c.recipient_phone
                FROM notification_delivery_attempts a JOIN customer_notification_outbox o ON o.id=a.outbox_id
                JOIN receipt_notification_consents c ON c.receipt_id=o.receipt_id
                WHERE o.app_id=? AND o.oa_id=? AND o.provider_mode=? AND a.attempt_number=o.attempt_count
                AND a.status IN ('ACCEPTED','UNKNOWN') AND a.provider_message_id IS NOT NULL
                AND a.status_check_count<? AND (a.next_status_check_at IS NULL OR a.next_status_check_at<=UTC_TIMESTAMP(6))
                AND a.created_at>DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 DAY) ORDER BY a.id LIMIT 10
                """, (r, i) -> new Check(r.getLong(1), r.getLong(2), r.getInt(3), r.getString(4), r.getInt(5), r.getString(6)), appId, oaId, mode, maxChecks);
    }

    public boolean claimCheck(Check check) {
        return jdbc.update("""
                UPDATE notification_delivery_attempts SET status_check_count=status_check_count+1,
                next_status_check_at=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 60 SECOND)
                WHERE id=? AND status_check_count=? AND status IN ('ACCEPTED','UNKNOWN')
                AND (next_status_check_at IS NULL OR next_status_check_at<=UTC_TIMESTAMP(6))
                """, check.id(), check.count()) == 1;
    }

    public void checked(Check check, int status, Instant deliveredAt, String code) {
        tx.executeWithoutResult(transaction -> {
            jdbc.queryForList("SELECT id FROM customer_notification_outbox WHERE id=? FOR UPDATE", check.jobId());
            jdbc.update("UPDATE notification_delivery_attempts SET last_status_check_at=UTC_TIMESTAMP(6),last_status_code=? WHERE id=?", code, check.id());
            if (status == 1 && deliveredAt != null && deliveredAt.isAfter(createdAt(check.id()).minusSeconds(300))) {
                delivered(check.jobId(), check.id(), check.number(), check.messageId(), deliveredAt, "POLL");
            }
            // status 0/-1 or transient query failures never authorize another POST/send or downgrade DELIVERED.
        });
    }

    public record Check(long id, long jobId, int number, String messageId, int count, String phone) {
        @Override public String toString() { return "ZaloStatusCheck[id=" + id + "]"; }
    }
    private record Event(long id, String appId, String oaId, String messageId, String trackingId, String recipientHash, Instant deliveredAt, int matches) {}
}
