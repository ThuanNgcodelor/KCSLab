-- P3: result dossier workflow (files, review, publish, revoke, versions) and P4 public lookup audit.
-- Publishing is only possible after the CFC-TN-14 coding gate (V7) is satisfied by the sample.

ALTER TABLE result_releases
    ADD COLUMN lookup_code VARCHAR(32) NULL,
    ADD COLUMN result_number VARCHAR(40) NULL,
    ADD COLUMN submitted_at TIMESTAMP(6) NULL,
    ADD COLUMN submitted_by_user_id BIGINT NULL,
    ADD COLUMN decided_at TIMESTAMP(6) NULL,
    ADD COLUMN decided_by_user_id BIGINT NULL,
    ADD COLUMN decision_note VARCHAR(500) NULL,
    ADD COLUMN published_at TIMESTAMP(6) NULL,
    ADD COLUMN revoked_at TIMESTAMP(6) NULL,
    ADD COLUMN revoked_by_user_id BIGINT NULL,
    ADD COLUMN revoke_reason VARCHAR(500) NULL,
    -- At most one published version per testing record, enforced by the database.
    ADD COLUMN published_marker TINYINT GENERATED ALWAYS AS (IF(status = 'PUBLISHED', 1, NULL)) STORED,
    ADD UNIQUE KEY uk_result_release_lookup_code (lookup_code),
    ADD UNIQUE KEY uk_result_release_number (result_number),
    ADD UNIQUE KEY uk_result_release_one_published (testing_record_id, published_marker),
    ADD CONSTRAINT fk_result_release_submitter FOREIGN KEY (submitted_by_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_result_release_decider FOREIGN KEY (decided_by_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_result_release_revoker FOREIGN KEY (revoked_by_user_id) REFERENCES users(id);

CREATE TABLE result_release_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    release_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    extension VARCHAR(10) NOT NULL,
    storage_name VARCHAR(80) NOT NULL,
    size BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    public_download BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by_user_id BIGINT NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    removed_at TIMESTAMP(6) NULL,
    removed_by_user_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_release_file_storage (storage_name),
    KEY idx_result_release_files_release (release_id, removed_at),
    CONSTRAINT fk_result_release_files_release FOREIGN KEY (release_id) REFERENCES result_releases(id),
    CONSTRAINT fk_result_release_files_uploader FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_result_release_files_remover FOREIGN KEY (removed_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Append-only history: rows are inserted, never updated or deleted.
CREATE TABLE result_release_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    release_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NULL,
    note VARCHAR(500) NULL,
    actor_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_result_release_events_release (release_id, id),
    CONSTRAINT fk_result_release_events_release FOREIGN KEY (release_id) REFERENCES result_releases(id),
    CONSTRAINT fk_result_release_events_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Public lookup audit. Never stores a phone number, a lookup code or a raw IP address.
CREATE TABLE public_lookup_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    release_id BIGINT NULL,
    action VARCHAR(20) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    client_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_public_lookup_audit_created (created_at),
    KEY idx_public_lookup_audit_release (release_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
