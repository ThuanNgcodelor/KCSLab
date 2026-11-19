-- Phiếu 3 bám mẫu Word: "Bảo quản sau khi nhận" tách khỏi điều kiện nhiệt độ (Thường/Lạnh/Khác),
-- và tình trạng mẫu là checklist nhiều lựa chọn nên cần cột dài hơn.
ALTER TABLE receipts ADD COLUMN storage_after_receipt VARCHAR(255) NULL AFTER storage_condition;
ALTER TABLE receipts MODIFY sample_condition VARCHAR(255) NOT NULL;
ALTER TABLE samples MODIFY sample_condition VARCHAR(255) NULL;
ALTER TABLE sample_transfer_items MODIFY sample_condition VARCHAR(255) NULL;
