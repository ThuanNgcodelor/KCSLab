-- Kỹ thuật viên được gắn (tag) vào một phiếu chuyển mẫu để thử nghiệm. Nhiều người cho một phiếu.
CREATE TABLE transfer_assignees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transfer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_assignee (transfer_id, user_id),
    KEY idx_transfer_assignees_user (user_id, transfer_id),
    CONSTRAINT fk_transfer_assignees_transfer FOREIGN KEY (transfer_id) REFERENCES sample_transfers (id),
    CONSTRAINT fk_transfer_assignees_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transfer_assignees_by FOREIGN KEY (assigned_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
