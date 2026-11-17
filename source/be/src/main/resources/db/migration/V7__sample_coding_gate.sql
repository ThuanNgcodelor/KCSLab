-- CFC-TN-14 coding gate (P2).
-- Atomic per-day sequence, independent verification state and append-only coding events.
-- Not implemented yet (waiting for LAB confirmation): -V/-R parts, QC and blind codes.

CREATE TABLE sample_code_counters (
    counter_date DATE NOT NULL,
    last_sequence INT NOT NULL,
    PRIMARY KEY (counter_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed from existing samples so already issued sequences are never handed out again.
INSERT INTO sample_code_counters (counter_date, last_sequence)
SELECT received_date, MAX(daily_sequence) FROM samples GROUP BY received_date;

ALTER TABLE samples
    ADD COLUMN coding_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CODING',
    ADD COLUMN coding_legacy BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN coded_by_user_id BIGINT NULL,
    ADD COLUMN coded_at TIMESTAMP(6) NULL,
    ADD COLUMN verified_by_user_id BIGINT NULL,
    ADD COLUMN verified_at TIMESTAMP(6) NULL,
    ADD KEY idx_samples_coding_status (coding_status, receipt_id),
    ADD CONSTRAINT fk_samples_coded_by FOREIGN KEY (coded_by_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_samples_verified_by FOREIGN KEY (verified_by_user_id) REFERENCES users(id);

-- Samples that already exist were coded before the gate existed. They are flagged as legacy
-- so they remain usable; LAB must confirm how to treat them (needs confirmation).
UPDATE samples SET coding_status = 'VERIFIED', coding_legacy = TRUE;

CREATE TABLE sample_code_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sample_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    old_code VARCHAR(80) NULL,
    new_code VARCHAR(80) NULL,
    reason VARCHAR(500) NULL,
    actor_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sample_code_events_sample (sample_id, id),
    CONSTRAINT fk_sample_code_events_sample FOREIGN KEY (sample_id) REFERENCES samples(id),
    CONSTRAINT fk_sample_code_events_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
