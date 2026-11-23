ALTER TABLE notification_delivery_attempts
    ADD COLUMN delivered_at DATETIME(6),
    ADD COLUMN delivery_source VARCHAR(20),
    ADD COLUMN status_check_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_status_check_at DATETIME(6),
    ADD COLUMN last_status_check_at DATETIME(6),
    ADD COLUMN last_status_code VARCHAR(60),
    ADD KEY ix_customer_status_checks(status, next_status_check_at);

CREATE TABLE zalo_delivery_inbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_key VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    app_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    oa_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    message_id VARCHAR(160) COLLATE utf8mb4_bin NOT NULL,
    tracking_id VARCHAR(48) COLLATE utf8mb4_bin NOT NULL,
    recipient_hash VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    delivered_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason_code VARCHAR(60),
    next_match_at DATETIME(6) NOT NULL,
    match_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_zalo_inbox_event(event_key),
    KEY ix_zalo_inbox_due(status, next_match_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE zalo_managed_tokens (
    scope_key VARCHAR(64) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    app_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    oa_id VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    provider_mode VARCHAR(30) NOT NULL,
    encrypted_pair TEXT NOT NULL,
    access_expires_at DATETIME(6) NOT NULL,
    refresh_expires_at DATETIME(6) NOT NULL,
    state VARCHAR(30) NOT NULL,
    lease_owner VARCHAR(40) COLLATE utf8mb4_bin,
    lease_until DATETIME(6),
    updated_at DATETIME(6) NOT NULL,
    reason_code VARCHAR(60)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE customer_notification_controls (
    id INT NOT NULL PRIMARY KEY,
    paused BOOLEAN NOT NULL DEFAULT false,
    updated_at DATETIME(6) NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
INSERT INTO customer_notification_controls(id, paused, updated_at) VALUES (1, false, UTC_TIMESTAMP(6));

CREATE TABLE customer_notification_operation_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    action VARCHAR(40) NOT NULL,
    request_hash VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    actor_user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_customer_operation_request(request_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
