-- Danh mục phương pháp thử (CFC-TN-16-2026, CFC-TN-17-2026, ...). Admin thêm/sửa/ngưng dùng;
-- Phiếu chuyển mẫu chọn từ danh mục và vẫn lưu mã dạng chuỗi trong sample_transfer_items.test_method
-- để phiếu cũ không đổi khi danh mục thay đổi.
CREATE TABLE test_methods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_test_methods_code (code),
    KEY idx_test_methods_active_code (active, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO test_methods (code, name, active, created_at, updated_at) VALUES
    ('CFC-TN-16-2026', NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('CFC-TN-17-2026', NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
