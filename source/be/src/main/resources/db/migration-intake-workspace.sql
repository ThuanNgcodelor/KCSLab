-- One-time idempotent upgrade for the intake workspace and internal notifications.
-- String policy: VARCHAR/TEXT with utf8mb4; never NVARCHAR/NCHAR.

ALTER TABLE samples MODIFY COLUMN product_type VARCHAR(80) NOT NULL;

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

DROP PROCEDURE IF EXISTS cfc_add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE cfc_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @index_sql = CONCAT(
            'CREATE INDEX `', p_index_name, '` ON `', p_table_name, '` (', p_columns, ')'
        );
        PREPARE index_statement FROM @index_sql;
        EXECUTE index_statement;
        DEALLOCATE PREPARE index_statement;
    END IF;
END$$
DELIMITER ;

CALL cfc_add_index_if_missing('receipts', 'idx_receipts_received_at', '`received_at`');
CALL cfc_add_index_if_missing('receipts', 'idx_receipts_status_received', '`status`, `received_at`');
CALL cfc_add_index_if_missing('receipts', 'idx_receipts_received_by_received', '`received_by_user_id`, `received_at`');

DROP PROCEDURE cfc_add_index_if_missing;
