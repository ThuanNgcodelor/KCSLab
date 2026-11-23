CREATE TABLE receipt_notification_consents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receipt_id BIGINT NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    consent_accepted BOOLEAN NOT NULL,
    consent_version VARCHAR(40) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_receipt_notification_consent (receipt_id),
    CONSTRAINT fk_customer_consent_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id),
    CONSTRAINT fk_customer_consent_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE customer_notification_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receipt_id BIGINT NOT NULL,
    event_key VARCHAR(120) COLLATE utf8mb4_bin NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    provider_mode VARCHAR(30) NOT NULL,
    session_key VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    app_id VARCHAR(80) NOT NULL,
    oa_id VARCHAR(80) NOT NULL,
    template_id VARCHAR(100) NOT NULL,
    template_version VARCHAR(40) NOT NULL,
    request_number VARCHAR(80) NOT NULL,
    template_data TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reason_code VARCHAR(80),
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    lease_owner VARCHAR(40) COLLATE utf8mb4_bin,
    lease_until DATETIME(6),
    retry_request_id VARCHAR(64) COLLATE utf8mb4_bin,
    retry_reason VARCHAR(255),
    retry_actor_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_customer_notification_event (event_key),
    UNIQUE KEY uk_customer_notification_receipt_event (receipt_id, event_type),
    KEY ix_customer_notification_due (status, next_attempt_at),
    CONSTRAINT fk_customer_outbox_receipt FOREIGN KEY (receipt_id) REFERENCES receipts(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE notification_delivery_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    outbox_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    tracking_id VARCHAR(48) COLLATE utf8mb4_bin NOT NULL,
    session_key VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    provider_mode VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_code VARCHAR(40),
    provider_message_id VARCHAR(160) COLLATE utf8mb4_bin,
    request_hash VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    retry_request_id VARCHAR(64) COLLATE utf8mb4_bin,
    actor_user_id BIGINT,
    reason VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    UNIQUE KEY uk_customer_delivery_tracking (tracking_id),
    UNIQUE KEY uk_customer_delivery_attempt (outbox_id, attempt_number),
    UNIQUE KEY uk_customer_delivery_retry (outbox_id, retry_request_id),
    CONSTRAINT fk_customer_attempt_outbox FOREIGN KEY (outbox_id) REFERENCES customer_notification_outbox(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- A durable, atomic cap shared by workers/restarts for one explicitly named test session.
CREATE TABLE customer_notification_test_budgets (
    session_key VARCHAR(80) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY,
    used_calls INT NOT NULL DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
