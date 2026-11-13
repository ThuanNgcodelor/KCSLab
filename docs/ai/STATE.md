# Project State

**Cập nhật:** 13/11/2026

## Đã hoàn thành

- Nền tảng Spring Boot 4.1.2/Java 21, React 19/Vite 6 và MySQL profile.
- Login email, JWT cookie, refresh session, CSRF, logout, `/auth/me`; không có registration.
- Quản lý nhân viên Admin-only: danh sách, tạo, sửa role/mật khẩu, khóa/mở khóa.
- Admin Portal responsive; mobile viewport 390px đã kiểm tra bằng Playwright.
- Vertical slice tiếp nhận phiếu 3: customer, receipt, sample, attachment, mã CFC-TN-14 và lưu file local trong dev.
- Schema MySQL hiện có 6 bảng: users, auth_sessions, customers, receipts, samples, receipt_attachments.

## Đã xác minh

- Frontend build: pass.
- Backend Maven test trong Docker: pass.
- Browser/MySQL smoke test auth, user management và intake/upload: pass.
- CodeGraph được index và ưu tiên cho khám phá source.

## Khoảng trống

- Chưa có phiếu chuyển mẫu 4, custody history, biên bản thử nghiệm 5, review/release PDF, public lookup và audit log.
- Chưa có migration tool, object storage/backup và bộ integration test nghiệp vụ.
- Frontend `/admin/*` hiện chỉ cho ADMIN dù backend receipt đã cho RECEPTIONIST.

## Nguồn đọc mặc định

Đọc `docs/ai/CONTEXT.md` để lấy context ngắn; dùng file này khi cần snapshot trạng thái chi tiết hơn.
