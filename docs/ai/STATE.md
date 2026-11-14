# Project State

**Cập nhật:** 14/11/2026

## Đã hoàn thành

- Nền tảng Spring Boot 4.1.2/Java 21, React 19/Vite 6 và MySQL profile.
- Login email, JWT cookie, refresh session, CSRF, logout, `/auth/me`; không có registration.
- Quản lý nhân viên Admin-only: danh sách, tạo, sửa role/mật khẩu, khóa/mở khóa.
- Admin Portal responsive theo tone sáng CFC; navbar/sidebar cố định theo viewport, mobile drawer và footer website CFC.
- Phase 1/Phiếu 3: hồ sơ nguồn có cấu trúc, nhiều mẫu trên một phiếu, actor tiếp nhận, ảnh/file phân loại có checksum và tải/xem lại trên desktop/mobile.
- Phase 3/Phiếu 4: vòng đời `DRAFT → PENDING → ACCEPTED|REJECTED`, sửa nháp, chỉ khóa mẫu khi gửi và ẩn bản nháp khỏi KCS/PTN.
- Danh sách Phiếu 3 phân trang phía server: mặc định 20/trang, tối đa 100, 9 lựa chọn sort trên UI và whitelist sort ở backend.
- Đã chốt kế hoạch Phase 1B thông báo khách hàng: `INTAKE_RECORDED`, ZBS Template Message ưu tiên, SMS Brandname/MultiSMS dự phòng, outbox/idempotency/webhook và WebSocket nội bộ tách biệt; chưa có runtime.
- Đã tách plan workspace Tiếp nhận và ba role tại `plans/intake/`: dashboard thật, query server-side, form Phiếu 3, notification center và WebSocket; chưa triển khai runtime mới.
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
- Dashboard còn hard-code; receipt API chưa có search/filter theo ngày/mẫu/trạng thái/người nhận; app chưa có header, notification persistence/page hoặc WebSocket dependency.

## Nguồn đọc mặc định

Đọc `docs/ai/CONTEXT.md` để lấy context ngắn; dùng file này khi cần snapshot trạng thái chi tiết hơn.
