# Current Plan — Tiếp nhận, thông báo, mã hóa và phát hành kết quả

## Hoàn thành bổ sung ngày 14/11/2026

- [x] Thay dropdown Phiếu 4 tải 100 hồ sơ bằng tìm kiếm server-side có debounce và phân trang 20 phiếu/trang.
- [x] Chỉ hiển thị phiếu còn mẫu `RECEIVED` qua bộ lọc backend `transferable=true`; có test loại phiếu đã chuyển khỏi kết quả.
- [x] Thêm index MySQL `samples(status, receipt_id)` cho truy vấn ứng viên khi dữ liệu tăng lớn.
- [x] Ẩn Tổng quan đối với role Tiếp nhận và sửa responsive `/admin/transfers` thành card mobile không tràn ngang.
- [x] Tạo trang `/admin/profile` cho mọi role: xem email/role/trạng thái, tự đổi tên hiển thị, avatar và mật khẩu.
- [x] Avatar được nén hai lớp: frontend cắt vuông 640×640 trước khi gửi; backend xác thực JPG/PNG, giới hạn request 5 MB/40 MP rồi chỉ lưu JPEG 320×320 chất lượng 82%. Không giữ ảnh gốc, mỗi user chỉ có một file hiện hành.
- [x] Avatar có URL theo phiên bản và cache private 30 ngày; xuất hiện ở sidebar và danh sách nhân viên. Đổi mật khẩu thu hồi toàn bộ refresh session.

## Profile P1 — Thông tin cá nhân

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Trang hồ sơ dùng chung ba role | Hoàn thành | Route `/admin/profile`, có loading/error/success và responsive |
| Tên hiển thị | Hoàn thành | Người dùng tự sửa; email/role/trạng thái là chỉ đọc |
| Avatar | Hoàn thành | Upload, thay, xóa; JPG/PNG; client pre-compress + server normalize 320×320 |
| Kiểm soát dung lượng | Hoàn thành MVP | Không lưu ảnh gốc, ghi đè file theo user, cache URL có version |
| Đổi mật khẩu | Hoàn thành | Kiểm tra mật khẩu hiện tại, tối thiểu 10 ký tự, thu hồi refresh session và đăng nhập lại |
| Lưu trữ production | Chưa làm | Hiện lưu local; cần object storage/backup khi triển khai nhiều máy chủ |

**Cập nhật:** 14/11/2026 · **Ưu tiên hiện tại:** Hoàn tất Intake I4–I5; I1–I3 đã có runtime và đã smoke test trên MySQL/Chrome

## Báo cáo sơ bộ hiện hành

| Hạng mục | Trạng thái | Ranh giới hiện tại |
|---|---|---|
| Nền tảng, auth, user/RBAC | Hoàn thành vertical slice | Login email, JWT cookie/refresh/CSRF, Admin tạo/sửa/khóa; từng user có profile/avatar/đổi mật khẩu; không có đăng ký công khai |
| Phiếu 3 — Tiếp nhận | Phase 1 hoàn thành, cần Phase 2 mã hóa | Đã có hồ sơ nguồn, actor, nhiều mẫu, ảnh/file phân loại; thiếu xác minh hai người, `-V/-R`, QC và mã mù |
| Thông báo khách hàng | Đã chốt plan, chưa có runtime | Ưu tiên ZBS Template Message; SMS Brandname/MultiSMS dự phòng; chưa có consent, outbox, provider adapter hoặc webhook |
| Phiếu 4 — Chuyển mẫu | Phase 3 hoàn thành | Nháp/sửa/gửi, KCS nhận hoặc từ chối, ẩn nháp khỏi KCS, lưu/hoàn mẫu và đồng bộ trạng thái |
| Phiếu 5 — Thử nghiệm | Hoàn thành đến kiểm tra nội bộ | Thiết bị, hóa chất, QC/raw data, submit, kiểm tra/trả lại; mẫu đến `TEST_REVIEWED` |
| Duyệt/phát hành kết quả | Đang lập kế hoạch, chưa có runtime | Chưa có `result_releases`, PDF versioned, checksum phát hành, thu hồi/đính chính |
| Tra cứu khách hàng | Chưa triển khai | Chưa có mã + đuôi điện thoại, trạng thái công khai hoặc tải PDF |
| Dashboard quản trị | Đã nối dữ liệu tiếp nhận thật | Aggregate theo ngày/người nhận từ backend; đã bỏ constants mẫu của dashboard |
| Responsive Admin UI | Hoàn thành nền giao diện | Shell fluid tối đa 1680px; drawer từ 1080px; đã kiểm tra 390/1024/1440/2560px |

**Ranh giới sản phẩm hiện tại:** hệ thống vận hành nội bộ được từ đăng nhập đến biên bản đã kiểm tra; chưa gửi Zalo/SMS cho khách và chưa có kết quả pháp lý/khách hàng được phát hành. Hoàn thành các giai đoạn 0–4 không đồng nghĩa sẵn sàng production.

## Plan riêng cho module Tiếp nhận

Nguồn thực thi mới:

- `plans/intake/OVERVIEW.md`: page/route, header, dashboard, API, WebSocket, form Phiếu 3 và thứ tự I1–I6.
- `plans/intake/RECEPTIONIST.md`: dashboard cá nhân và quy trình của người nhận mẫu.
- `plans/intake/ADMIN.md`: dashboard tổng hợp, ngoại lệ, catalog và notification operation.
- `plans/intake/TECHNICIAN_KCS.md`: KCS một phòng, nhận Phiếu 4 và notification realtime.

Kết luận đã chốt:

- [x] Không dùng dashboard hard-code; chỉ xóa constants mẫu khi API thật đã được nối để không tạo màn hình hỏng.
- [x] Search/filter/sort/pagination chạy ở backend/database; frontend chỉ giữ query state và render response.
- [x] Header hiển thị ba notification mới nhất; trang notification riêng dùng pagination 20/trang.
- [x] WebSocket là realtime transport, REST/database là nguồn sự thật; notification nội bộ tách khỏi ZBS/SMS gửi khách.
- [x] KCS là đích cố định, không tạo UI chọn nhiều phòng.
- [x] Phiếu 3 không có danh sách đóng loại sản phẩm; đổi sang combobox có search + nhập mới, còn product code do catalog/Admin quản lý.
- [x] I1: criteria/specification + aggregate API + index/test; dashboard/list dùng dữ liệu thật và query ở backend.
- [x] I2–I3: notification persistence/API, header ba tin + trang riêng 20/trang và WebSocket cookie-auth có reconnect.
- [ ] I4–I5: form Phiếu 3 đã hỗ trợ nhập tự do loại sản phẩm, nhiều mẫu và ảnh mobile; còn draft/completion, catalog duyệt và unsaved warning.
- [ ] I6: MySQL load test hợp lý và E2E ba role trên desktop/mobile.

## Ưu tiên mới — Phase 1B thông báo sau tiếp nhận

Chi tiết contract, schema, retry, bảo mật và acceptance criteria nằm tại `docs/INTAKE_NOTIFICATIONS.md`.

- [x] Chốt điểm kích hoạt MVP: `INTAKE_RECORDED` chỉ phát sau khi Phiếu 3 và mẫu được commit thành công; nội dung chỉ xác nhận “đã ghi nhận”, không khẳng định mẫu đạt hoặc đã mã hóa.
- [x] Chốt chiến lược kênh: ZBS Template Message là kênh ưu tiên; SMS Brandname/MultiSMS là adapter dự phòng; WebSocket chỉ dành cho nhân viên nội bộ.
- [x] Chốt kiến trúc database outbox + worker nền + provider adapter + webhook/callback; không gọi provider trong transaction tiếp nhận.
- [ ] Chốt nội dung consent, template, sender/brandname, ngân sách và nhà cung cấp SMS thực tế.
- [ ] Tạo OA/ZBS Account/AppID, liên kết quyền, gửi duyệt template và chuẩn bị sandbox/provider credentials ngoài source code.
- [ ] Triển khai preferences/consent/template/outbox/delivery bằng migration versioned; mọi chuỗi dùng `VARCHAR/TEXT` + `utf8mb4`.
- [ ] Triển khai worker, idempotency, retry/backoff, fake provider và test rollback/concurrency/duplicate.
- [ ] Bổ sung form chọn kênh/consent, timeline delivery, hàng đợi lỗi phân trang 20/trang và quyền resend cho Admin.
- [ ] Tích hợp ZBS API + delivery webhook; sau đó tích hợp đúng SMS provider và fallback chống gửi kép.
- [ ] Thêm WebSocket nội bộ sau khi event notification ổn định; REST/database vẫn là nguồn sự thật.

### Thứ tự ưu tiên tích hợp với plan cũ

1. Làm N0–N2 của notification bằng fake provider, không chờ tài khoản Zalo/SMS.
2. Tích hợp ZBS/SMS khi LAB có tài khoản, template đã duyệt và sandbox.
3. Tiếp tục Phase 2/5A: sequence nguyên tử, chấp nhận mẫu, `-V/-R`, xác minh hai người, QC/mã mù và gate trước Phiếu 4.
4. Hardening Phase 3 bằng WebSocket nội bộ cho sự kiện gửi/nhận/từ chối Phiếu 4.
5. Sau đó mới phát hành PDF, public lookup và QR kết quả.

## Kết luận từ biểu mẫu mới

Đã đối chiếu `1. Phiếu KQKN-DV.docx`, ví dụ `KQKN-SR-CT001-092026.docx` và CFC-TN-14. Phiếu kết quả phụ thuộc vào dữ liệu tiếp nhận và mã hóa chưa được lưu đầy đủ, vì vậy phải hoàn thành gate 5A trước khi thiết kế snapshot/PDF. Phân tích chi tiết nằm tại `docs/SAMPLE_CODING.md`.

- Tách riêng mã hồ sơ tiếp nhận, mã mẫu và số phiếu kết quả.
- CFC-TN-14 đã được xác nhận là nguồn chuẩn: dùng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`; phiếu ví dụ `PRODUCT-SOURCE-...` được ghi nhận là sai lệch cần rà soát, không đổi generator.
- Không dùng `countByReceivedDate + 1`; thay bằng bộ cấp số nguyên tử theo ngày Việt Nam.
- Bổ sung phần mẫu `-V/-R`, QC, mã mù, đối chiếu độc lập và lịch sử hiệu chỉnh.
- Bổ sung người/ngày lấy mẫu, vùng trồng, cơ sở đóng gói, xe/moóc/container/seal dưới dạng dữ liệu có cấu trúc.

## Mục tiêu

Hoàn thiện chuỗi truy xuất từ hồ sơ nguồn → xác nhận tiếp nhận cho khách → mã mẫu đã xác minh → biên bản đã kiểm tra → kết quả có phiên bản/PDF bất biến, rồi mới chuẩn bị dữ liệu an toàn cho tra cứu khách hàng.

## Đã hoàn thành làm nền

- [x] Auth/session/CSRF, quản lý nhân viên và RBAC cơ bản; không có đăng ký công khai.
- [x] Phiếu tiếp nhận số 3, customer, sample, attachment và mã CFC-TN-14.
- [x] Phase 1: hồ sơ nguồn có cấu trúc, nhiều mẫu, actor tiếp nhận và bằng chứng ảnh/file từ điện thoại.
- [x] Danh sách tiếp nhận phân trang server-side 20 phiếu/trang và sort theo whitelist.
- [x] Thiết kế Phase 1B notification với ZBS/SMS, outbox, idempotency, delivery audit và fallback; chưa có code runtime.
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

1. Intake I1: API dashboard/search/filter/sort/pagination thật và bỏ dashboard hard-code sau khi frontend nối API.
2. Intake I2–I3: notification nội bộ lưu bền, header ba tin, page riêng và WebSocket user destination.
3. Intake I4–I5: dashboard/list theo role và form Phiếu 3 bám sáu mục, catalog loại sản phẩm nhập tay có kiểm soát.
4. Customer notification N0–N2 bằng outbox/fake provider; tích hợp ZBS/SMS khi có tài khoản/template/sandbox.
5. Khóa Phase 2/5A: sequence nguyên tử, `-V/-R`, xác minh hai người, QC/mã mù và gate Phiếu 4.
6. Sửa separation of duties cho thử nghiệm, rồi làm `result_releases`, PDF/QR và public lookup.

## Chưa làm trong giai đoạn này

- Public lookup bằng mã + đuôi số điện thoại.
- QR/OTP, rate limit chống dò mã và lịch sử vùng trồng.
- Gửi nội dung quảng cáo/broadcast hoặc kết quả thử nghiệm qua Zalo/SMS.
- Audit log tổng quát, object storage/backup và migration versioned ngoài các migration bắt buộc của notification.

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
- ZBS/SMS chưa có code, consent, template đã duyệt, credential, outbox, callback hoặc delivery audit; plan nằm tại `docs/INTAKE_NOTIFICATIONS.md`.
- Dashboard đang hiển thị dữ liệu mẫu nên không dùng làm số liệu vận hành.
- Schema vẫn quản lý bằng SQL thủ công + Hibernate `validate`; chưa có Flyway/Liquibase.
- File phiếu đang lưu local; chưa có object storage, retention và backup production.
- Chưa có audit log tổng quát, rate limiting và bộ integration/Testcontainers đầy đủ.
