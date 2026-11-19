-- Phiếu 07 (Kế hoạch lấy mẫu) và Phiếu 02 (Lấy mẫu hiện trường) dùng chung Số/No. với Phiếu yêu cầu.
-- Đồng bộ dữ liệu cũ; bỏ qua dòng nào sẽ trùng số với một phiếu khác (unique key).
UPDATE sampling_plans p
JOIN test_requests r ON r.id = p.test_request_id
SET p.form_number = r.request_number
WHERE p.form_number <> r.request_number
  AND NOT EXISTS (SELECT 1 FROM (SELECT id, form_number FROM sampling_plans) x
                  WHERE x.form_number = r.request_number AND x.id <> p.id);

UPDATE field_sampling_forms f
JOIN test_requests r ON r.id = f.test_request_id
SET f.form_number = r.request_number
WHERE f.form_number <> r.request_number
  AND NOT EXISTS (SELECT 1 FROM (SELECT id, form_number FROM field_sampling_forms) x
                  WHERE x.form_number = r.request_number AND x.id <> f.id);
