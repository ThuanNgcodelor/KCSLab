-- One-time idempotent upgrade for databases created before Phase 1/Phase 3 hardening.
-- MySQL 8.0+, string columns remain VARCHAR/TEXT with utf8mb4.

DROP PROCEDURE IF EXISTS cfc_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE cfc_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @column_sql = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_definition
        );
        PREPARE column_statement FROM @column_sql;
        EXECUTE column_statement;
        DEALLOCATE PREPARE column_statement;
    END IF;
END$$
DELIMITER ;

CALL cfc_add_column_if_missing('receipts', 'sampled_by', 'VARCHAR(160) NULL AFTER received_at');
CALL cfc_add_column_if_missing('receipts', 'sampled_at', 'TIMESTAMP(6) NULL AFTER sampled_by');
CALL cfc_add_column_if_missing('receipts', 'growing_area', 'VARCHAR(255) NULL AFTER sampled_at');
CALL cfc_add_column_if_missing('receipts', 'packing_facility', 'VARCHAR(255) NULL AFTER growing_area');
CALL cfc_add_column_if_missing('receipts', 'vehicle_number', 'VARCHAR(80) NULL AFTER packing_facility');
CALL cfc_add_column_if_missing('receipts', 'trailer_number', 'VARCHAR(80) NULL AFTER vehicle_number');
CALL cfc_add_column_if_missing('receipts', 'container_number', 'VARCHAR(80) NULL AFTER trailer_number');

CALL cfc_add_column_if_missing('receipt_attachments', 'category', 'VARCHAR(30) NOT NULL DEFAULT ''OTHER'' AFTER receipt_id');
CALL cfc_add_column_if_missing('receipt_attachments', 'description', 'VARCHAR(255) NULL AFTER category');
CALL cfc_add_column_if_missing('receipt_attachments', 'captured_at', 'TIMESTAMP(6) NULL AFTER checksum');
CALL cfc_add_column_if_missing('receipt_attachments', 'uploaded_by_user_id', 'BIGINT NULL AFTER uploaded_at');

CALL cfc_add_column_if_missing('sample_transfers', 'receiving_department', 'VARCHAR(120) NOT NULL DEFAULT ''KCS/PTN'' AFTER received_by_user_id');
CALL cfc_add_column_if_missing('sample_transfers', 'submitted_at', 'TIMESTAMP(6) NULL AFTER lab_retention_until');

DROP PROCEDURE cfc_add_column_if_missing;

SET @attachment_category_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'receipt_attachments'
      AND index_name = 'idx_receipt_attachments_category'
);
SET @attachment_category_sql = IF(
    @attachment_category_index = 0,
    'CREATE INDEX idx_receipt_attachments_category ON receipt_attachments (receipt_id, category)',
    'SELECT 1'
);
PREPARE attachment_category_statement FROM @attachment_category_sql;
EXECUTE attachment_category_statement;
DEALLOCATE PREPARE attachment_category_statement;

SET @attachment_uploader_fk = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'receipt_attachments'
      AND constraint_name = 'fk_receipt_attachments_uploaded_by'
);
SET @attachment_uploader_sql = IF(
    @attachment_uploader_fk = 0,
    'ALTER TABLE receipt_attachments ADD CONSTRAINT fk_receipt_attachments_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)',
    'SELECT 1'
);
PREPARE attachment_uploader_statement FROM @attachment_uploader_sql;
EXECUTE attachment_uploader_statement;
DEALLOCATE PREPARE attachment_uploader_statement;
