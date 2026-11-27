-- Số/No. dạng CFC{YY}.{NNNNN}: thứ tự có tối thiểu 5 chữ số (CFC26.0005 -> CFC26.00005).
-- Chỉ đổi số đúng 4 chữ số, có thể kèm hậu tố (/R1, -2); số đã đủ 5 chữ số không bị đổi lần nữa.
-- Số 4 chữ số được thêm một số 0 sau dấu chấm nên không thể trùng số 5 chữ số có sẵn; phép đổi giữ nguyên thứ tự và tính duy nhất.
UPDATE test_requests
SET request_number = CONCAT(LEFT(request_number, 6), '0', SUBSTRING(request_number, 7))
WHERE request_number REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';

UPDATE receipts
SET request_number = CONCAT(LEFT(request_number, 6), '0', SUBSTRING(request_number, 7))
WHERE request_number REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';

UPDATE receipts
SET request_number_unique = CONCAT(LEFT(request_number_unique, 6), '0', SUBSTRING(request_number_unique, 7))
WHERE request_number_unique REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';

UPDATE sampling_plans
SET form_number = CONCAT(LEFT(form_number, 6), '0', SUBSTRING(form_number, 7))
WHERE form_number REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';

UPDATE field_sampling_forms
SET form_number = CONCAT(LEFT(form_number, 6), '0', SUBSTRING(form_number, 7))
WHERE form_number REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';

UPDATE result_releases
SET result_number = CONCAT(LEFT(result_number, 6), '0', SUBSTRING(result_number, 7))
WHERE result_number REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';

UPDATE result_releases
SET document_number = CONCAT(LEFT(document_number, 6), '0', SUBSTRING(document_number, 7))
WHERE document_number REGEXP '^CFC[0-9]{2}[.][0-9]{4}([^0-9].*)?$';
-- customer_notification_outbox giữ nguyên: đó là lịch sử tin đã gửi cho khách với số cũ.
