-- Only draft creation is enabled until the CFC-TN-14 verification gate is implemented.
CREATE TABLE result_releases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    testing_record_id BIGINT NOT NULL,
    version INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_release_record_version (testing_record_id, version),
    KEY idx_result_release_status_created (status, created_at),
    CONSTRAINT fk_result_release_testing_record FOREIGN KEY (testing_record_id) REFERENCES testing_records(id),
    CONSTRAINT fk_result_release_creator FOREIGN KEY (created_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
