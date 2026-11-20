-- Số/No. dạng CFC luôn có 4 chữ số thứ tự (CFC26.0001); số gõ tay kiểu CFC26.00001 được đưa về dạng chuẩn.
-- Chỉ đổi khi dạng chuẩn chưa bị dòng khác dùng (tránh vi phạm unique key).
UPDATE test_requests t
SET t.request_number = CONCAT(LEFT(t.request_number, 6), LPAD(CAST(SUBSTRING(t.request_number, 7) AS UNSIGNED), 4, '0'))
WHERE t.request_number REGEXP '^CFC[0-9]{2}\\.0[0-9]{4,}$'
  AND NOT EXISTS (SELECT 1 FROM (SELECT request_number FROM test_requests) x
                  WHERE x.request_number = CONCAT(LEFT(t.request_number, 6), LPAD(CAST(SUBSTRING(t.request_number, 7) AS UNSIGNED), 4, '0')));

UPDATE receipts r
SET r.request_number = CONCAT(LEFT(r.request_number, 6), LPAD(CAST(SUBSTRING(r.request_number, 7) AS UNSIGNED), 4, '0'))
WHERE r.request_number REGEXP '^CFC[0-9]{2}\\.0[0-9]{4,}$';

UPDATE receipts r
SET r.request_number_unique = r.request_number
WHERE r.request_number_unique IS NOT NULL AND r.request_number_unique <> r.request_number
  AND NOT EXISTS (SELECT 1 FROM (SELECT request_number_unique FROM receipts) x WHERE x.request_number_unique = r.request_number);

UPDATE sampling_plans p JOIN test_requests t ON t.id = p.test_request_id
SET p.form_number = t.request_number
WHERE p.form_number <> t.request_number
  AND NOT EXISTS (SELECT 1 FROM (SELECT form_number FROM sampling_plans) x WHERE x.form_number = t.request_number);

UPDATE field_sampling_forms f JOIN test_requests t ON t.id = f.test_request_id
SET f.form_number = t.request_number
WHERE f.form_number <> t.request_number
  AND NOT EXISTS (SELECT 1 FROM (SELECT form_number FROM field_sampling_forms) x WHERE x.form_number = t.request_number);
