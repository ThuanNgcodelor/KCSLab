CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
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
    CONSTRAINT fk_receipts_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_receipts_user FOREIGN KEY (received_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS samples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sample_code VARCHAR(80) NOT NULL,
    source_code VARCHAR(20) NOT NULL,
    product_type VARCHAR(10) NOT NULL,
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
    CONSTRAINT fk_samples_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS receipt_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    storage_name VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_receipt_attachments_receipt (receipt_id),
    CONSTRAINT fk_receipt_attachments_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
