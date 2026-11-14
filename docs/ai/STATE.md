# Project State

**Cập nhật:** 13/11/2026

## Đã hoàn thành

- Nền tảng Spring Boot 4.1.2/Java 21, React 19/Vite 6 và MySQL profile.
- Login email, JWT cookie, refresh session, CSRF, logout, `/auth/me`; không có registration.
- Quản lý nhân viên Admin-only: danh sách, tạo, sửa role/mật khẩu, khóa/mở khóa.
- Admin Portal responsive theo tone sáng CFC; navbar/sidebar cố định theo viewport, mobile drawer và footer website CFC.
- Phase 1/Phiếu 3: hồ sơ nguồn có cấu trúc, nhiều mẫu trên một phiếu, actor tiếp nhận, ảnh/file phân loại có checksum và tải/xem lại trên desktop/mobile.
- Phase 3/Phiếu 4: vòng đời `DRAFT → PENDING → ACCEPTED|REJECTED`, sửa nháp, chỉ khóa mẫu khi gửi và ẩn bản nháp khỏi KCS/PTN.
- Vertical slice biên bản thử nghiệm 5: thiết bị, hóa chất, QC, raw data, submit, trả lại và kiểm tra hoàn tất.
- Schema MySQL hiện có 12 bảng, bổ sung `sample_transfers`, `sample_transfer_items` và 4 bảng `testing_*`.

## Đã xác minh

- Frontend production build: pass (63 modules).
- Backend Maven test: 8/8 pass trên Java 21.
- MySQL smoke Phase 1/3: tạo phiếu hai mẫu, upload Form 3, sửa/gửi Phiếu 4 và nhận mẫu: pass.
- Google Chrome CDP: 5 route desktop/mobile, không tràn ngang tại 390px, không có console error.
- CodeGraph được index và ưu tiên cho khám phá source.

## Khoảng trống

- Chưa có phát hành/version PDF, public lookup và audit log tổng quát.
- Chưa có migration tool, object storage/backup và bộ integration test nghiệp vụ.
- Chưa có role reviewer chuyên biệt; bước review biên bản hiện do `ADMIN` thực hiện.
- Chưa có gate mã hóa độc lập (counter nguyên tử, `-V/-R`, người thứ hai xác minh), QR và WebSocket thông báo.

## Nguồn đọc mặc định

Đọc `docs/ai/CONTEXT.md` để lấy context ngắn; dùng file này khi cần snapshot trạng thái chi tiết hơn.
