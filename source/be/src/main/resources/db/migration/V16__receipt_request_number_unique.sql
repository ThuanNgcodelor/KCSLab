-- Số/No. là định danh duy nhất của hồ sơ tiếp nhận (Phiếu 3): mỗi Số/No chỉ có một hồ sơ.
-- Dữ liệu cũ đã có số trùng nên không đặt UNIQUE trực tiếp lên request_number: cột request_number_unique
-- chỉ giữ số của hồ sơ đầu tiên (id nhỏ nhất); hồ sơ trùng cũ để NULL. Hồ sơ mới luôn ghi cột này.
ALTER TABLE receipts
    ADD COLUMN request_number_unique VARCHAR(80) NULL AFTER request_number,
    ADD UNIQUE KEY uk_receipts_request_number_unique (request_number_unique);

UPDATE receipts r
JOIN (SELECT MIN(id) AS id FROM receipts WHERE request_number IS NOT NULL GROUP BY request_number) first_receipt
  ON first_receipt.id = r.id
SET r.request_number_unique = r.request_number;
