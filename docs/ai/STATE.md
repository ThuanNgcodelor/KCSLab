# Project State

## Cập nhật mới nhất — duy trì đăng nhập bằng Redis

- Trang đăng nhập có tùy chọn **Ghi nhớ đăng nhập**; hệ thống không lưu mật khẩu. Chọn ghi nhớ tạo cookie HttpOnly tối đa 30 ngày, không chọn dùng cookie phiên trình duyệt.
- Access token mặc định 15 phút. Frontend tự gọi `/api/v1/auth/refresh` khi API trả 401, gom các request đồng thời vào một lần refresh rồi thử lại request ban đầu.
- Refresh token ngẫu nhiên 48 byte, Redis chỉ lưu SHA-256 hash với TTL; token được xoay sau mỗi lần refresh và token cũ không thể dùng lại. Đổi mật khẩu thu hồi mọi phiên của người dùng.
- Redis Laragon 5.x được hỗ trợ bằng Lua `GET + DEL` nguyên tử thay cho lệnh `GETDEL` chỉ có từ Redis 6.2.
- Cookie resolver bỏ qua access token cũ tại `csrf/login/refresh`, nên access token hết hạn không chặn chính endpoint làm mới phiên.
- Trang thông tin cá nhân đã khóa overflow theo content area; dưới 1280 px chuyển layout hồ sơ thành một cột và các ô mật khẩu giữ cùng chiều cao.
- Xác minh: backend 16/16 test pass; frontend production build 71 modules; Redis runtime smoke đạt remembered/session cookie, 401 → refresh → 200 và chống tái sử dụng token; Chrome 1424/1024/390 px không tràn ngang.

## Cập nhật mới nhất — bộ chọn Phiếu 4 và responsive

- Màn lập Phiếu chuyển mẫu tìm phiếu tiếp nhận phía server theo mã phiếu, mã mẫu, khách hàng hoặc số điện thoại; debounce 300 ms, 20 kết quả/trang và có điều hướng trang.
- API `GET /api/v1/receipts` hỗ trợ `transferable=true`, chỉ trả phiếu không bị từ chối và còn ít nhất một mẫu `RECEIVED`; không còn tải 100 phiếu rồi lọc ở trình duyệt.
- MySQL có composite index `idx_samples_status_receipt(status, receipt_id)` phục vụ truy vấn ứng viên chuyển mẫu; migration idempotent đã được áp dụng trên CSDL local.
- Navbar của `RECEPTIONIST` không còn mục Tổng quan. Trang `/admin/transfers` chuyển bảng thành card có nhãn ở màn hình nhỏ và không tràn ngang.
- Đã xác minh bằng test `ReceiptServiceTest`, frontend production build và Google Chrome CDP ở breakpoint mobile; API MySQL trả đúng 3/3 ứng viên hiện có, tối đa 20/trang.

## Cập nhật mới nhất — thông tin cá nhân

- Cả `ADMIN`, `RECEPTIONIST`, `TECHNICIAN` có trang `/admin/profile`; tự đổi tên hiển thị, avatar và mật khẩu, còn email/role/trạng thái là chỉ đọc.
- Avatar JPG/PNG được frontend co về 640×640 để giảm lưu lượng upload; backend kiểm tra tối đa 5 MB/40 MP, cắt vuông và chỉ lưu JPEG 320×320 chất lượng 82%. Ảnh gốc không được lưu và file mới ghi đè file cũ của cùng user.
- Avatar dùng URL versioned và cache private 30 ngày, hiển thị tại sidebar và danh sách nhân viên. Đổi mật khẩu thu hồi toàn bộ refresh session.
- MySQL đã có cột nullable `users.avatar_updated_at` bằng migration idempotent; file hiện lưu local tại `storage/avatars`.
- Xác minh: backend 12/12 test pass, frontend build 71 modules, Chrome profile desktop 1424 px/mobile 390 px không overflow; ảnh smoke thực tế trả đúng 320×320 rồi được xóa.

**Cập nhật:** 14/11/2026

## Đã hoàn thành

- Nền tảng Spring Boot 4.1.2/Java 21, React 19/Vite 6 và MySQL profile.
- Login email, JWT cookie, refresh session, CSRF, logout, `/auth/me`; không có registration.
- Quản lý nhân viên Admin-only: danh sách, tạo, sửa role/mật khẩu, khóa/mở khóa.
- Admin Portal responsive theo tone sáng CFC; navbar/sidebar cố định theo viewport, mobile drawer và footer website CFC.
- Phase 1/Phiếu 3: hồ sơ nguồn có cấu trúc, nhiều mẫu trên một phiếu, actor tiếp nhận, ảnh/file phân loại có checksum và tải/xem lại trên desktop/mobile.
- Phase 3/Phiếu 4: vòng đời `DRAFT → PENDING → ACCEPTED|REJECTED`, sửa nháp, chỉ khóa mẫu khi gửi và ẩn bản nháp khỏi KCS/PTN.
- Danh sách Phiếu 3 phân trang phía server: mặc định 20/trang, tối đa 100, 9 lựa chọn sort trên UI và whitelist sort ở backend.
- Đã chốt kế hoạch Phase 1B thông báo khách hàng: `INTAKE_RECORDED`, ZBS Template Message ưu tiên, SMS Brandname/MultiSMS dự phòng và outbox/idempotency/webhook; phần gửi khách chưa có runtime.
- Workspace Tiếp nhận đã có dashboard/query server-side, notification center lưu bền, trang thông báo và WebSocket nội bộ; form Phiếu 3 vẫn còn draft/completion/catalog duyệt.
- Vertical slice biên bản thử nghiệm 5: thiết bị, hóa chất, QC, raw data, submit, trả lại và kiểm tra hoàn tất.
- Schema MySQL hiện có 12 bảng, bổ sung `sample_transfers`, `sample_transfer_items` và 4 bảng `testing_*`.

## Đã xác minh

- Frontend production build: pass (63 modules).
- Backend Maven test: 9/9 pass trên Java 21.
- MySQL smoke Phase 1/3: tạo phiếu hai mẫu, upload Form 3, sửa/gửi Phiếu 4 và nhận mẫu: pass.
- Google Chrome CDP: 7 route desktop/mobile, phân trang/sort hiển thị đúng, không tràn ngang tại 390px, không có console error.
- CodeGraph được index và ưu tiên cho khám phá source.

## Khoảng trống

- Chưa có phát hành/version PDF, public lookup và audit log tổng quát.
- Chưa có migration tool, object storage/backup và bộ integration test nghiệp vụ.
- Chưa có role reviewer chuyên biệt; bước review biên bản hiện do `ADMIN` thực hiện.
- Chưa có gate mã hóa độc lập (counter nguyên tử, `-V/-R`, người thứ hai xác minh), QR và WebSocket thông báo.
- Chưa có consent/template được duyệt, outbox/delivery, worker, provider adapter hoặc callback ZBS/SMS; chi tiết tại `docs/INTAKE_NOTIFICATIONS.md`.
- Avatar hiện lưu filesystem local; chưa có object storage/CDN, backup và xử lý nhiều instance production.

## Nguồn đọc mặc định

Đọc `docs/ai/CONTEXT.md` để lấy context ngắn; dùng file này khi cần snapshot trạng thái chi tiết hơn.
