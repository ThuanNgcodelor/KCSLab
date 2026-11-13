# CFC Lab — AI Context

**Cập nhật:** 13/11/2026 · **Nguồn sự thật ngắn cho phiên AI mới**

## Hiện trạng

- Ứng dụng đang chạy theo modular monolith: Spring Boot 4.1.2/Java 21 ở `source/be`, React 19/Vite 6/JSX ở `source/fe`, MySQL 8.4 cho profile `mysql`.
- Đã hoàn thành: đăng nhập bằng email, JWT access cookie + refresh session, CSRF, logout, `/auth/me`; Admin quản lý tài khoản; Admin Portal; tiếp nhận mẫu theo phiếu 3; upload hồ sơ giấy; sinh mã mẫu CFC-TN-14.
- Không có đăng ký công khai. Admin được seed từ biến môi trường; không ghi credentials vào tài liệu.
- Frontend hiện chỉ cho role `ADMIN` vào `/admin/*`. Backend cho `ADMIN` quản lý user và cho `ADMIN|RECEPTIONIST` dùng `/receipts/**`; cần mở route frontend theo role trước khi Receptionist sử dụng.

## Luồng đã chạy

1. Admin đăng nhập tại `/login`.
2. Quản lý nhân viên tại `/admin/users`, tạo ở `/admin/users/new`, sửa ở `/admin/users/:id/edit`.
3. Xem phiếu tại `/admin/intake`, tạo tại `/admin/intake/new`.
4. Backend lưu customer → receipt → sample → attachment và sinh mã dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`.
5. File dev lưu cục bộ ở `source/be/storage/receipts`; metadata và SHA-256 checksum lưu MySQL.

## API và dữ liệu thực tế

- Auth: `GET /api/v1/auth/csrf`, `POST /login`, `POST /refresh`, `POST /logout`, `GET /me`.
- User: `GET|POST /api/v1/users`, `PATCH /api/v1/users/{id}`; Admin-only.
- Intake: `GET|POST /api/v1/receipts`, `GET /api/v1/receipts/{id}`, `POST /api/v1/receipts/{id}/attachments`.
- Bảng hiện có: `users`, `auth_sessions`, `customers`, `receipts`, `samples`, `receipt_attachments`.
- Schema hiện dùng SQL thủ công + Hibernate `validate`; chưa có Flyway/Liquibase.

## Xác minh gần nhất

- Backend `mvn test` trong Docker Maven/JDK 21: pass.
- Frontend `npm run build`: pass.
- Playwright/MySQL: login, tạo user, tạo phiếu, upload DOCX, sinh mã và xem lại danh sách: pass.
- Mobile 390px: `/admin`, `/admin/users`, `/admin/intake`, `/admin/intake/new` không horizontal overflow; console không có lỗi.
- Chưa có test JUnit/integration nghiệp vụ đáng kể; Maven pass hiện chủ yếu xác minh compile/context.

## Việc tiếp theo

1. Triển khai phiếu chuyển mẫu số 4 và custody events.
2. Chốt người giao/nhận, quyền ký và trạng thái chuyển mẫu với LAB.
3. Sau đó triển khai biên bản thử nghiệm số 5, duyệt/phát hành PDF và tra cứu khách bằng mã + đuôi số điện thoại.
4. Trước production: migration tool, object storage/backup, audit log, rate limit và test integration.

## Cách nạp ngữ cảnh tiết kiệm token

- Bắt đầu bằng file này; dùng CodeGraph để lấy symbol/call path trước khi đọc source.
- Chỉ đọc `plans/CURRENT.md` khi lập kế hoạch; `docs/API_CONVENTIONS.md` khi đổi API; `docs/DATABASE.md` khi đổi schema; `docs/RBAC.md` khi đổi quyền.
- Chỉ mở skill phù hợp tác vụ. Không tải toàn bộ references của skill hoặc toàn bộ `docs/`.
- `docs/ai/CHANGELOG.md` là lịch sử, không phải file cần đọc đầu phiên.
