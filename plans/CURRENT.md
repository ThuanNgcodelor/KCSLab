# Current Plan — Phiếu chuyển mẫu số 4

**Cập nhật:** 13/11/2026 · **Giai đoạn hiện tại:** 3 — Chuyển mẫu KCS/PTN

## Mục tiêu

Nối phiếu tiếp nhận đã có với bước bàn giao mẫu lên KCS/PTN, giữ được chuỗi custody và quyền giao/nhận rõ ràng.

## Đã hoàn thành làm nền

- [x] Auth/session/CSRF và RBAC cơ bản.
- [x] Quản lý nhân viên; không có đăng ký công khai.
- [x] Phiếu tiếp nhận số 3, customer, sample, attachment và mã CFC-TN-14.
- [x] Frontend desktop/mobile và Playwright smoke test với MySQL.

## Phạm vi tiếp theo

- [ ] Chốt field từ `4.Phieu chuyen mau.docx` và mapping sang receipt/sample.
- [ ] Chốt actor: người giao, người nhận, KCS/PTN, quyền xác nhận và trường hợp từ chối.
- [ ] Thiết kế `sample_transfers`/`sample_custody_events`, trạng thái và audit tối thiểu.
- [ ] API tạo/xem/xác nhận phiếu chuyển mẫu.
- [ ] Giao diện danh sách, tạo phiếu và xác nhận bàn giao, responsive mobile.
- [ ] Test service/API và Playwright end-to-end.

## Chưa làm trong giai đoạn này

- Biên bản thử nghiệm số 5.
- Duyệt/phát hành PDF kết quả.
- Public lookup cho khách.

## Điều kiện hoàn thành

- Từ một receipt đã tiếp nhận có thể tạo phiếu 4, chọn mẫu, ghi người giao/nhận, xác nhận bàn giao và xem lịch sử.
- Quyền backend/frontend thống nhất cho ADMIN, RECEPTIONIST và TECHNICIAN.
- Build/test và Playwright pass; tài liệu API/database/RBAC được cập nhật.
