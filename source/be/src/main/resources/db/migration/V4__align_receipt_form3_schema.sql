-- Phiếu tiếp nhận số 3: bổ sung số phiếu yêu cầu và trạng thái niêm phong 3 mức.
-- Chạy trên MySQL 8; chỉ thêm dữ liệu/cột, không dùng NVARCHAR và không xóa dữ liệu.
SET @has_request_number = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'receipts' AND column_name = 'request_number');
SET @sql = IF(@has_request_number = 0, 'ALTER TABLE receipts ADD COLUMN request_number VARCHAR(80) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_seal_status = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'receipts' AND column_name = 'seal_status');
SET @sql = IF(@has_seal_status = 0, 'ALTER TABLE receipts ADD COLUMN seal_status VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE receipts SET seal_status = CASE WHEN seal_intact = TRUE THEN 'INTACT' ELSE 'OPENED' END WHERE seal_status IS NULL;

SET @has_request_index = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'receipts' AND index_name = 'idx_receipts_request_number');
SET @sql = IF(@has_request_index = 0, 'CREATE INDEX idx_receipts_request_number ON receipts(request_number)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
