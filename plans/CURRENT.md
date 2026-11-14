# Current Plan — Hoàn thiện mã hóa và phát hành kết quả

**Cập nhật:** 13/11/2026 · **Giai đoạn hiện tại:** 5A — Hoàn thiện hồ sơ nguồn và mã hóa

## Báo cáo sơ bộ hiện hành

| Hạng mục | Trạng thái | Ranh giới hiện tại |
|---|---|---|
| Nền tảng, auth, user/RBAC | Hoàn thành vertical slice | Login email, JWT cookie/refresh/CSRF, Admin tạo/sửa/khóa tài khoản; không có đăng ký công khai |
| Phiếu 3 — Tiếp nhận | Phase 1 hoàn thành, cần Phase 2 mã hóa | Đã có hồ sơ nguồn, actor, nhiều mẫu, ảnh/file phân loại; thiếu xác minh hai người, `-V/-R`, QC và mã mù |
| Phiếu 4 — Chuyển mẫu | Phase 3 hoàn thành | Nháp/sửa/gửi, KCS nhận hoặc từ chối, ẩn nháp khỏi KCS, lưu/hoàn mẫu và đồng bộ trạng thái |
| Phiếu 5 — Thử nghiệm | Hoàn thành đến kiểm tra nội bộ | Thiết bị, hóa chất, QC/raw data, submit, kiểm tra/trả lại; mẫu đến `TEST_REVIEWED` |
| Duyệt/phát hành kết quả | Đang lập kế hoạch, chưa có runtime | Chưa có `result_releases`, PDF versioned, checksum phát hành, thu hồi/đính chính |
| Tra cứu khách hàng | Chưa triển khai | Chưa có mã + đuôi điện thoại, trạng thái công khai hoặc tải PDF |
| Dashboard quản trị | Giao diện mẫu | Các số liệu/hoạt động/mức sẵn sàng hiện là dữ liệu hard-code, chưa phải báo cáo thật |
| Responsive Admin UI | Hoàn thành nền giao diện | Shell fluid tối đa 1680px; drawer từ 1080px; đã kiểm tra 390/1024/1440/2560px |

**Ranh giới sản phẩm hiện tại:** hệ thống vận hành nội bộ được từ đăng nhập đến biên bản đã kiểm tra; chưa có kết quả pháp lý/khách hàng được phát hành. Hoàn thành các giai đoạn 0–4 không đồng nghĩa sẵn sàng production.

## Kết luận từ biểu mẫu mới

Đã đối chiếu `1. Phiếu KQKN-DV.docx`, ví dụ `KQKN-SR-CT001-092026.docx` và CFC-TN-14. Phiếu kết quả phụ thuộc vào dữ liệu tiếp nhận và mã hóa chưa được lưu đầy đủ, vì vậy phải hoàn thành gate 5A trước khi thiết kế snapshot/PDF. Phân tích chi tiết nằm tại `docs/SAMPLE_CODING.md`.

- Tách riêng mã hồ sơ tiếp nhận, mã mẫu và số phiếu kết quả.
- CFC-TN-14 đã được xác nhận là nguồn chuẩn: dùng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`; phiếu ví dụ `PRODUCT-SOURCE-...` được ghi nhận là sai lệch cần rà soát, không đổi generator.
- Không dùng `countByReceivedDate + 1`; thay bằng bộ cấp số nguyên tử theo ngày Việt Nam.
- Bổ sung phần mẫu `-V/-R`, QC, mã mù, đối chiếu độc lập và lịch sử hiệu chỉnh.
- Bổ sung người/ngày lấy mẫu, vùng trồng, cơ sở đóng gói, xe/moóc/container/seal dưới dạng dữ liệu có cấu trúc.

## Mục tiêu

Hoàn thiện chuỗi truy xuất từ hồ sơ nguồn → mã mẫu đã xác minh → biên bản đã kiểm tra → kết quả có phiên bản/PDF bất biến, rồi mới chuẩn bị dữ liệu an toàn cho tra cứu khách hàng.

## Đã hoàn thành làm nền

- [x] Auth/session/CSRF, quản lý nhân viên và RBAC cơ bản; không có đăng ký công khai.
- [x] Phiếu tiếp nhận số 3, customer, sample, attachment và mã CFC-TN-14.
- [x] Phase 1: hồ sơ nguồn có cấu trúc, nhiều mẫu, actor tiếp nhận và bằng chứng ảnh/file từ điện thoại.
- [x] Phase 3/Phiếu chuyển mẫu số 4: tạo/sửa nháp, gửi bàn giao, giao/nhận hoặc từ chối, thông tin lưu/hoàn mẫu.
- [x] Biên bản thử nghiệm số 5: thiết bị, hóa chất, đường chuẩn, công thức, QC, raw data, submit và kiểm tra/trả lại.
- [x] Giao diện desktop/mobile, navbar cố định theo viewport, footer CFC và bộ nhận diện sáng.
- [x] Maven test, frontend build và Playwright E2E với MySQL.

## 5A — Hồ sơ nguồn và mã hóa mẫu

- [x] Chốt thứ tự mã mẫu theo CFC-TN-14: `CT001-SR-DDMMYYYY-SEQ`; không áp dụng thứ tự `SR-CT001-...` từ phiếu ví dụ.
- [ ] Chốt quy tắc số Phiếu kết quả; chưa mặc định generator `SRMMYYYY-SEQ` chỉ từ một ví dụ.
- [x] Bổ sung trường người/ngày lấy mẫu, vùng trồng, cơ sở đóng gói, xe/moóc/container/seal.
- [ ] Tách bước tạo hồ sơ, chấp nhận mẫu, gán mã và xác minh mã; không sinh mã khi hồ sơ chưa đạt.
- [ ] Thay cấp số bằng counter/locking nguyên tử theo ngày `Asia/Ho_Chi_Minh`; thêm test đồng thời và ranh giới nửa đêm.
- [ ] Triển khai sample part `-V/-R`, nhãn mẫu và chặn chuyển mẫu trước khi mã được người thứ hai xác minh.
- [ ] Thiết kế mã QC `BL/SP/D`, mã mù và bảng đối chiếu bảo mật.
- [ ] Lưu actor/thời điểm/lý do cho gán mã, xác minh và hiệu chỉnh; không tái sử dụng mã.
- [ ] Số hóa Sổ theo dõi mẫu; quản lý mapping mã–thông tin gốc và hồ sơ mã mù bằng quyền truy cập riêng.
- [ ] Giới hạn API projection để kỹ thuật viên chỉ thấy dữ liệu kỹ thuật cần thiết, không mặc định thấy danh tính khách.

## 5B — Quyền kiểm tra và phát hành

- [ ] Chốt quyền phát hành. Mặc định MVP đề xuất dùng `ADMIN` làm người kiểm tra/phát hành nhưng bắt buộc khác tài khoản người phân tích; nếu LAB cần tách chuyên môn sẽ thêm `REVIEWER`/`LAB_MANAGER`.
- [ ] Chặn người phân tích tự kiểm tra hoặc tự phát hành biên bản của chính mình; bổ sung test quyền tương ứng.

## 5C — Phiếu kết quả và PDF

- [ ] Thiết kế `result_releases`: version, trạng thái, người duyệt, thời điểm, checksum và quan hệ với biên bản.
- [ ] Tạo số phiếu kết quả độc lập với mã mẫu; chốt signatory và quy tắc QR.
- [ ] Snapshot các trường “Thông tin mẫu” và bảng kết quả từ dữ liệu có cấu trúc tại thời điểm phát hành.
- [ ] Tạo PDF kết quả từ dữ liệu đã kiểm tra; file phát hành không bị ghi đè.
- [ ] API xem trước, phát hành, thu hồi/đính chính và tải PDF có kiểm soát.
- [ ] Giao diện hàng đợi duyệt/phát hành và lịch sử phiên bản.
- [ ] Test quyền, tính bất biến, checksum, PDF và Playwright end-to-end.

## Thứ tự triển khai đề xuất

1. Khóa contract mã mẫu theo CFC-TN-14, chốt riêng quy tắc số Phiếu kết quả và hoàn thiện schema hồ sơ nguồn.
2. Làm bộ cấp số nguyên tử + timezone Việt Nam + test concurrency.
3. Làm `-V/-R`, xác minh hai người, lịch sử hiệu chỉnh và chặn chuyển mẫu chưa xác minh.
4. Thiết kế QC/mã mù và data projection cho kỹ thuật viên.
5. Sửa quy tắc tách người phân tích–người kiểm tra và khóa contract quyền bằng unit test.
6. Bổ sung `result_releases`, snapshot, version/checksum và PDF bất biến.
7. Thêm thu hồi/đính chính cùng lịch sử trên Admin UI; chạy E2E toàn luồng.

## Chưa làm trong giai đoạn này

- Public lookup bằng mã + đuôi số điện thoại.
- QR/OTP, rate limit chống dò mã và lịch sử vùng trồng.
- Audit log tổng quát, object storage/backup và migration versioned.

## Điều kiện hoàn thành

- Chỉ biên bản `REVIEWED` mới được phát hành; dữ liệu nháp/QC/raw data không xuất hiện trong tài liệu khách.
- Chỉ mẫu có mã đã được người thứ hai xác minh mới được chuyển sang phòng thử nghiệm.
- Mã mẫu được cấp nguyên tử theo ngày Việt Nam, hỗ trợ phần nền/QC/mã mù và không tái sử dụng.
- Người phân tích không được tự kiểm tra hoặc tự phát hành kết quả của chính mình.
- Mỗi lần phát hành tạo version và checksum riêng, có thể thu hồi/đính chính nhưng không ghi đè lịch sử.
- Backend/frontend thống nhất quyền; build, test và Playwright pass.

## Rủi ro/việc kỹ thuật chưa đóng

- `TestingRecordService.review` hiện chưa kiểm tra `checker != analyst`.
- `ReceiptService.create` vẫn dùng `countByReceivedDate + 1`; timezone nghiệp vụ đã chuyển sang `Asia/Ho_Chi_Minh` nhưng cấp số vẫn có race condition.
- Model hiện chưa có `-V/-R`, QC, mã mù, người xác minh và lịch sử hiệu chỉnh mã.
- WebSocket thông báo chuyển mẫu và QR chưa triển khai; phải chốt event/điểm sinh QR sau gate xác minh mã.
- Dashboard đang hiển thị dữ liệu mẫu nên không dùng làm số liệu vận hành.
- Schema vẫn quản lý bằng SQL thủ công + Hibernate `validate`; chưa có Flyway/Liquibase.
- File phiếu đang lưu local; chưa có object storage, retention và backup production.
- Chưa có audit log tổng quát, rate limiting và bộ integration/Testcontainers đầy đủ.
