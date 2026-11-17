package com.example.app.publicapi;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/** Writes audit rows in their own transaction so failed lookups are recorded too. No phone numbers or codes. */
@Component
class PublicLookupAudit {
    private final JdbcTemplate jdbc;

    PublicLookupAudit(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(Long releaseId, String action, String outcome, String clientHash) {
        jdbc.update("insert into public_lookup_audit (release_id, action, outcome, client_hash, created_at) values (?,?,?,?,?)",
                releaseId, action, outcome, clientHash, Timestamp.from(Instant.now()));
    }
}
