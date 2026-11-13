# Current Plan — Duyệt và phát hành kết quả

**Cập nhật:** 13/11/2026 · **Giai đoạn hiện tại:** 5 — Duyệt/phát hành

## Báo cáo sơ bộ hiện hành

| Hạng mục | Trạng thái | Ranh giới hiện tại |
|---|---|---|
| Nền tảng, auth, user/RBAC | Hoàn thành vertical slice | Login email, JWT cookie/refresh/CSRF, Admin tạo/sửa/khóa tài khoản; không có đăng ký công khai |
| Phiếu 3 — Tiếp nhận | Hoàn thành vertical slice | Customer, receipt, sample, mã CFC-TN-14, attachment/checksum và giao diện vận hành |
| Phiếu 4 — Chuyển mẫu | Hoàn thành vertical slice | Tạo phiếu, giao/nhận hoặc từ chối, lưu/hoàn mẫu và đồng bộ trạng thái |
| Phiếu 5 — Thử nghiệm | Hoàn thành đến kiểm tra nội bộ | Thiết bị, hóa chất, QC/raw data, submit, kiểm tra/trả lại; mẫu đến `TEST_REVIEWED` |
| Duyệt/phát hành kết quả | Đang lập kế hoạch, chưa có runtime | Chưa có `result_releases`, PDF versioned, checksum phát hành, thu hồi/đính chính |
| Tra cứu khách hàng | Chưa triển khai | Chưa có mã + đuôi điện thoại, trạng thái công khai hoặc tải PDF |
| Dashboard quản trị | Giao diện mẫu | Các số liệu/hoạt động/mức sẵn sàng hiện là dữ liệu hard-code, chưa phải báo cáo thật |
| Responsive Admin UI | Hoàn thành nền giao diện | Shell fluid tối đa 1680px; drawer từ 1080px; đã kiểm tra 390/1024/1440/2560px |

**Ranh giới sản phẩm hiện tại:** hệ thống vận hành nội bộ được từ đăng nhập đến biên bản đã kiểm tra; chưa có kết quả pháp lý/khách hàng được phát hành. Hoàn thành các giai đoạn 0–4 không đồng nghĩa sẵn sàng production.

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

- [ ] Chốt quyền phát hành. Mặc định MVP đề xuất dùng `ADMIN` làm người kiểm tra/phát hành nhưng bắt buộc khác tài khoản người phân tích; nếu LAB cần tách chuyên môn sẽ thêm `REVIEWER`/`LAB_MANAGER`.
- [ ] Chặn người phân tích tự kiểm tra hoặc tự phát hành biên bản của chính mình; bổ sung test quyền tương ứng.
- [ ] Thiết kế `result_releases`: version, trạng thái, người duyệt, thời điểm, checksum và quan hệ với biên bản.
- [ ] Tạo PDF kết quả từ dữ liệu đã kiểm tra; file phát hành không bị ghi đè.
- [ ] API xem trước, phát hành, thu hồi/đính chính và tải PDF có kiểm soát.
- [ ] Giao diện hàng đợi duyệt/phát hành và lịch sử phiên bản.
- [ ] Test quyền, tính bất biến, checksum, PDF và Playwright end-to-end.

## Thứ tự triển khai đề xuất

1. Sửa quy tắc tách người phân tích–người kiểm tra và khóa contract quyền bằng unit test.
2. Bổ sung schema/entity `result_releases` và trạng thái phát hành; chưa tạo public API.
3. Tạo bản xem trước PDF từ snapshot dữ liệu `REVIEWED`.
4. Phát hành version 1 với SHA-256; không cho ghi đè file/snapshot.
5. Thêm thu hồi/đính chính thành version mới và lịch sử trên Admin UI.
6. Chạy backend test, frontend build và Playwright từ biên bản đến tải PDF.

## Chưa làm trong giai đoạn này

- Public lookup bằng mã + đuôi số điện thoại.
- QR/OTP, rate limit chống dò mã và lịch sử vùng trồng.
- Audit log tổng quát, object storage/backup và migration versioned.

## Điều kiện hoàn thành

- Chỉ biên bản `REVIEWED` mới được phát hành; dữ liệu nháp/QC/raw data không xuất hiện trong tài liệu khách.
- Người phân tích không được tự kiểm tra hoặc tự phát hành kết quả của chính mình.
- Mỗi lần phát hành tạo version và checksum riêng, có thể thu hồi/đính chính nhưng không ghi đè lịch sử.
- Backend/frontend thống nhất quyền; build, test và Playwright pass.

## Rủi ro/việc kỹ thuật chưa đóng

- `TestingRecordService.review` hiện chưa kiểm tra `checker != analyst`.
- Dashboard đang hiển thị dữ liệu mẫu nên không dùng làm số liệu vận hành.
- Schema vẫn quản lý bằng SQL thủ công + Hibernate `validate`; chưa có Flyway/Liquibase.
- File phiếu đang lưu local; chưa có object storage, retention và backup production.
- Chưa có audit log tổng quát, rate limiting và bộ integration/Testcontainers đầy đủ.
