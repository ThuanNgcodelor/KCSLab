-- KQKN-DV report content: structured result rows, signatories, generated PDF and result number counter.

ALTER TABLE result_releases
    ADD COLUMN quality_head_name VARCHAR(120) NULL,
    ADD COLUMN director_name VARCHAR(120) NULL;

CREATE TABLE result_release_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    release_id BIGINT NOT NULL,
    row_order INT NOT NULL,
    parameter_name VARCHAR(200) NOT NULL,
    unit VARCHAR(60) NULL,
    method VARCHAR(200) NULL,
    result_value VARCHAR(120) NOT NULL,
    note VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_result_release_items_release (release_id, row_order),
    CONSTRAINT fk_result_release_items_release FOREIGN KEY (release_id) REFERENCES result_releases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- UPLOAD = attachment from a technician; GENERATED_REPORT = the KQKN PDF produced by the system at publication.
ALTER TABLE result_release_files ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'UPLOAD';

-- Result number counters, one per prefix (product code + MMYYYY), e.g. SR092026 -> SR092026-001.
CREATE TABLE result_number_counters (
    prefix VARCHAR(30) NOT NULL,
    last_sequence INT NOT NULL,
    PRIMARY KEY (prefix)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
