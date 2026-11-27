-- Lịch sử chỉnh sửa hồ sơ tiếp nhận (Phiếu 3) sau khi lưu. Chỉ thêm dòng, không sửa/xóa.
CREATE TABLE receipt_edit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    edited_by_user_id BIGINT NOT NULL,
    edited_at TIMESTAMP(6) NOT NULL,
    reason VARCHAR(500) NULL,
    after_transfer BOOLEAN NOT NULL DEFAULT FALSE,
    summary TEXT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_receipt_edit_logs_receipt (receipt_id, id),
    CONSTRAINT fk_receipt_edit_logs_receipt FOREIGN KEY (receipt_id) REFERENCES receipts (id),
    CONSTRAINT fk_receipt_edit_logs_user FOREIGN KEY (edited_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
