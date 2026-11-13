# Current Plan — Duyệt và phát hành kết quả

**Cập nhật:** 13/11/2026 · **Giai đoạn hiện tại:** 5 — Duyệt/phát hành

## Mục tiêu

Chuyển biên bản thử nghiệm đã kiểm tra thành kết quả có phiên bản, phát hành PDF bất biến và chuẩn bị dữ liệu an toàn cho tra cứu khách hàng.

## Đã hoàn thành làm nền

- [x] Auth/session/CSRF, quản lý nhân viên và RBAC cơ bản; không có đăng ký công khai.
- [x] Phiếu tiếp nhận số 3, customer, sample, attachment và mã CFC-TN-14.
- [x] Phiếu chuyển mẫu số 4: tạo phiếu, giao/nhận hoặc từ chối, thông tin lưu/hoàn mẫu.
- [x] Biên bản thử nghiệm số 5: thiết bị, hóa chất, đường chuẩn, công thức, QC, raw data, submit và kiểm tra/trả lại.
- [x] Giao diện desktop/mobile, navbar cố định theo viewport, footer CFC và bộ nhận diện sáng.
- [x] Maven test, frontend build và Playwright E2E với MySQL.

## Phạm vi tiếp theo

- [ ] Chốt người có quyền duyệt/phát hành và có cần role `REVIEWER`/`LAB_MANAGER` hay không.
- [ ] Thiết kế `result_releases`: version, trạng thái, người duyệt, thời điểm, checksum và quan hệ với biên bản.
- [ ] Tạo PDF kết quả từ dữ liệu đã kiểm tra; file phát hành không bị ghi đè.
- [ ] API xem trước, phát hành, thu hồi/đính chính và tải PDF có kiểm soát.
- [ ] Giao diện hàng đợi duyệt/phát hành và lịch sử phiên bản.
- [ ] Test quyền, tính bất biến, checksum, PDF và Playwright end-to-end.

## Chưa làm trong giai đoạn này

- Public lookup bằng mã + đuôi số điện thoại.
- QR/OTP, rate limit chống dò mã và lịch sử vùng trồng.
- Audit log tổng quát, object storage/backup và migration versioned.

## Điều kiện hoàn thành

- Chỉ biên bản `REVIEWED` mới được phát hành; dữ liệu nháp/QC/raw data không xuất hiện trong tài liệu khách.
- Mỗi lần phát hành tạo version và checksum riêng, có thể thu hồi/đính chính nhưng không ghi đè lịch sử.
- Backend/frontend thống nhất quyền; build, test và Playwright pass.
