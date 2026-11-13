# Project State

**Cập nhật:** 13/11/2026

## Đã hoàn thành

- Nền tảng Spring Boot 4.1.2/Java 21, React 19/Vite 6 và MySQL profile.
- Login email, JWT cookie, refresh session, CSRF, logout, `/auth/me`; không có registration.
- Quản lý nhân viên Admin-only: danh sách, tạo, sửa role/mật khẩu, khóa/mở khóa.
- Admin Portal responsive theo tone sáng CFC; navbar/sidebar cố định theo viewport, mobile drawer và footer website CFC.
- Vertical slice tiếp nhận phiếu 3: customer, receipt, sample, attachment, mã CFC-TN-14 và lưu file local trong dev.
- Vertical slice phiếu chuyển mẫu 4: tạo phiếu, các mẫu bàn giao, chấp nhận/từ chối và dữ liệu lưu/hoàn mẫu.
- Vertical slice biên bản thử nghiệm 5: thiết bị, hóa chất, QC, raw data, submit, trả lại và kiểm tra hoàn tất.
- Schema MySQL hiện có 12 bảng, bổ sung `sample_transfers`, `sample_transfer_items` và 4 bảng `testing_*`.

## Đã xác minh

- Frontend build: pass.
- Backend Maven test trong Docker: pass.
- Browser/MySQL E2E auth, intake, transfer và vòng đời testing đến `TEST_REVIEWED`: pass.
- CodeGraph được index và ưu tiên cho khám phá source.

## Khoảng trống

- Chưa có phát hành/version PDF, public lookup và audit log tổng quát.
- Chưa có migration tool, object storage/backup và bộ integration test nghiệp vụ.
- Chưa có role reviewer chuyên biệt; bước review biên bản hiện do `ADMIN` thực hiện.

## Nguồn đọc mặc định

Đọc `docs/ai/CONTEXT.md` để lấy context ngắn; dùng file này khi cần snapshot trạng thái chi tiết hơn.
