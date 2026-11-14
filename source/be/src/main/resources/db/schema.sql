-- MySQL string policy: use VARCHAR/TEXT with utf8mb4; never use NVARCHAR/NCHAR.
-- Tables default to utf8mb4_unicode_ci. Technical case-sensitive values may
-- override to a binary collation while remaining VARCHAR/CHAR.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    avatar_updated_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_sessions_token_hash (token_hash),
    KEY idx_auth_sessions_user_id (user_id),
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(160) NULL,
    address VARCHAR(255) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_customers_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS receipts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_code VARCHAR(40) NOT NULL,
    customer_id BIGINT NOT NULL,
    received_by_user_id BIGINT NOT NULL,
    delivered_by VARCHAR(160) NOT NULL,
    received_at TIMESTAMP(6) NOT NULL,
    sampled_by VARCHAR(160) NULL,
    sampled_at TIMESTAMP(6) NULL,
    growing_area VARCHAR(255) NULL,
    packing_facility VARCHAR(255) NULL,
    vehicle_number VARCHAR(80) NULL,
    trailer_number VARCHAR(80) NULL,
    container_number VARCHAR(80) NULL,
    sample_name VARCHAR(160) NOT NULL,
    quantity VARCHAR(80) NULL,
    packaging VARCHAR(80) NULL,
    seal_number VARCHAR(80) NULL,
    seal_intact BOOLEAN NOT NULL,
    sample_condition VARCHAR(80) NOT NULL,
    condition_detail TEXT NULL,
    transport_info TEXT NULL,
    received_temperature DOUBLE NULL,
    storage_condition VARCHAR(80) NULL,
    requested_parameters TEXT NULL,
    expected_date DATE NULL,
    special_request TEXT NULL,
    acceptance_decision VARCHAR(30) NOT NULL,
    acceptance_reason TEXT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_receipts_code (receipt_code),
    KEY idx_receipts_customer (customer_id),
    KEY idx_receipts_created (created_at),
    KEY idx_receipts_received_at (received_at),
    KEY idx_receipts_status_received (status, received_at),
    KEY idx_receipts_received_by_received (received_by_user_id, received_at),
    CONSTRAINT fk_receipts_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_receipts_user FOREIGN KEY (received_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS samples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sample_code VARCHAR(80) NOT NULL,
    source_code VARCHAR(20) NOT NULL,
    product_type VARCHAR(80) NOT NULL,
    received_date DATE NOT NULL,
    daily_sequence INT NOT NULL,
    name VARCHAR(160) NOT NULL,
    quantity VARCHAR(80) NULL,
    packaging VARCHAR(80) NULL,
    sample_condition VARCHAR(80) NULL,
    requested_parameters TEXT NULL,
    status VARCHAR(30) NOT NULL,
    receipt_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_samples_code (sample_code),
    KEY idx_samples_receipt (receipt_id),
    KEY idx_samples_date (received_date),
    KEY idx_samples_status_receipt (status, receipt_id),
    CONSTRAINT fk_samples_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS receipt_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(255) NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    storage_name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    captured_at TIMESTAMP(6) NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    uploaded_by_user_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_receipt_attachments_receipt (receipt_id),
    KEY idx_receipt_attachments_category (receipt_id, category),
    CONSTRAINT fk_receipt_attachments_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id),
    CONSTRAINT fk_receipt_attachments_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sample_transfers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transfer_code VARCHAR(40) NOT NULL,
    receipt_id BIGINT NOT NULL,
    delivered_by_user_id BIGINT NOT NULL,
    received_by_user_id BIGINT NULL,
    receiving_department VARCHAR(120) NOT NULL,
    transfer_date DATE NOT NULL,
    results_due_date DATE NOT NULL,
    return_to_customer BOOLEAN NOT NULL DEFAULT FALSE,
    customer_return_date DATE NULL,
    changes_note TEXT NULL,
    status VARCHAR(30) NOT NULL,
    receiving_note TEXT NULL,
    lab_opinion TEXT NULL,
    lab_retain BOOLEAN NOT NULL DEFAULT FALSE,
    lab_retention_until DATE NULL,
    submitted_at TIMESTAMP(6) NULL,
    received_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sample_transfers_code (transfer_code),
    KEY idx_sample_transfers_receipt (receipt_id),
    KEY idx_sample_transfers_status (status),
    CONSTRAINT fk_sample_transfers_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id),
    CONSTRAINT fk_sample_transfers_delivered_by FOREIGN KEY (delivered_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_sample_transfers_received_by FOREIGN KEY (received_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sample_transfer_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transfer_id BIGINT NOT NULL,
    sample_id BIGINT NOT NULL,
    sample_name VARCHAR(160) NOT NULL,
    sample_code VARCHAR(80) NOT NULL,
    weight VARCHAR(80) NULL,
    quantity VARCHAR(80) NULL,
    sample_condition VARCHAR(120) NULL,
    requested_parameters TEXT NULL,
    test_method VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sample_transfer_items_transfer (transfer_id),
    KEY idx_sample_transfer_items_sample (sample_id),
    CONSTRAINT fk_sample_transfer_items_transfer FOREIGN KEY (transfer_id) REFERENCES sample_transfers(id),
    CONSTRAINT fk_sample_transfer_items_sample FOREIGN KEY (sample_id) REFERENCES samples(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS testing_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    record_code VARCHAR(40) NOT NULL,
    transfer_item_id BIGINT NOT NULL,
    analyst_user_id BIGINT NOT NULL,
    checked_by_user_id BIGINT NULL,
    analysis_start TIMESTAMP(6) NULL,
    analysis_end TIMESTAMP(6) NULL,
    calibration_curve TEXT NULL,
    calculation_formula TEXT NULL,
    recovery_percent DECIMAL(12,4) NULL,
    qc_chart_note TEXT NULL,
    analyst_note TEXT NULL,
    review_note TEXT NULL,
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMP(6) NULL,
    reviewed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_testing_records_code (record_code),
    UNIQUE KEY uk_testing_records_transfer_item (transfer_item_id),
    KEY idx_testing_records_status (status),
    KEY idx_testing_records_analyst (analyst_user_id),
    CONSTRAINT fk_testing_records_transfer_item FOREIGN KEY (transfer_item_id) REFERENCES sample_transfer_items(id),
    CONSTRAINT fk_testing_records_analyst FOREIGN KEY (analyst_user_id) REFERENCES users(id),
    CONSTRAINT fk_testing_records_checker FOREIGN KEY (checked_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS testing_equipment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    testing_record_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    serial_number VARCHAR(120) NULL,
    manufacturer VARCHAR(160) NULL,
    identification_code VARCHAR(120) NULL,
    calibration_date DATE NULL,
    PRIMARY KEY (id),
    KEY idx_testing_equipment_record (testing_record_id),
    CONSTRAINT fk_testing_equipment_record FOREIGN KEY (testing_record_id) REFERENCES testing_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS testing_reagents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    testing_record_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    code VARCHAR(120) NULL,
    origin VARCHAR(120) NULL,
    preparation_date DATE NULL,
    expiry_date DATE NULL,
    storage_condition VARCHAR(160) NULL,
    concentration VARCHAR(120) NULL,
    PRIMARY KEY (id),
    KEY idx_testing_reagents_record (testing_record_id),
    CONSTRAINT fk_testing_reagents_record FOREIGN KEY (testing_record_id) REFERENCES testing_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS staff_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_url VARCHAR(255) NULL,
    entity_type VARCHAR(60) NULL,
    entity_id BIGINT NULL,
    dedupe_key VARCHAR(180) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_notifications_dedupe (dedupe_key),
    KEY idx_staff_notifications_recipient_created (recipient_user_id, created_at),
    KEY idx_staff_notifications_recipient_read (recipient_user_id, read_at),
    CONSTRAINT fk_staff_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS testing_measurements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    testing_record_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    row_order INT NOT NULL,
    replicate VARCHAR(40) NOT NULL,
    sample_amount VARCHAR(80) NULL,
    final_volume VARCHAR(80) NULL,
    dilution_factor VARCHAR(80) NULL,
    measured_concentration VARCHAR(120) NULL,
    result_per_replicate VARCHAR(120) NULL,
    result VARCHAR(120) NULL,
    deviation VARCHAR(120) NULL,
    PRIMARY KEY (id),
    KEY idx_testing_measurements_record_type (testing_record_id, type),
    CONSTRAINT fk_testing_measurements_record FOREIGN KEY (testing_record_id) REFERENCES testing_records(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
