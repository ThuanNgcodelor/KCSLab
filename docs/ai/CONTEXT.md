# CFC Lab — AI Context

**Cập nhật:** 13/11/2026 · **Nguồn sự thật ngắn cho phiên AI mới**

## Hiện trạng

- Ứng dụng đang chạy theo modular monolith: Spring Boot 4.1.2/Java 21 ở `source/be`, React 19/Vite 6/JSX ở `source/fe`, MySQL 8.4 cho profile `mysql`.
- Đã hoàn thành: auth email/session/CSRF; quản lý tài khoản; phiếu tiếp nhận 3; phiếu chuyển mẫu 4; biên bản thử nghiệm 5 đến bước kiểm tra/trả lại.
- Không có đăng ký công khai. Admin được seed từ biến môi trường; không ghi credentials vào tài liệu.
- Frontend mở từng route theo role: `ADMIN` quản lý user/review; `ADMIN|RECEPTIONIST` tiếp nhận; `ADMIN|TECHNICIAN` thử nghiệm. Quyền transfer cho ba role theo cấu hình hiện tại.
- UI dùng tone sáng CFC, logo từ `guideline-assets/cfc-008.png`; sidebar cố định theo viewport, mobile drawer và footer liên kết website CFC.

## Luồng đã chạy

1. Admin đăng nhập tại `/login`.
2. Quản lý nhân viên tại `/admin/users`, tạo ở `/admin/users/new`, sửa ở `/admin/users/:id/edit`.
3. Xem phiếu tại `/admin/intake`, tạo tại `/admin/intake/new`.
4. Backend lưu customer → receipt → sample → attachment và sinh mã dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`.
5. File dev lưu cục bộ ở `source/be/storage/receipts`; metadata và SHA-256 checksum lưu MySQL.
6. Lập/xác nhận phiếu 4 tại `/admin/transfers`; khi nhận thành công mẫu chuyển sang `TRANSFERRED`.
7. Lập biên bản 5 tại `/admin/testing`; `DRAFT/RETURNED → SUBMITTED → REVIEWED`, đồng bộ mẫu `IN_TESTING → AWAITING_REVIEW → TEST_REVIEWED`.

## API và dữ liệu thực tế

- Auth: `GET /api/v1/auth/csrf`, `POST /login`, `POST /refresh`, `POST /logout`, `GET /me`.
- User: `GET|POST /api/v1/users`, `PATCH /api/v1/users/{id}`; Admin-only.
- Intake: `GET|POST /api/v1/receipts`, `GET /api/v1/receipts/{id}`, `POST /api/v1/receipts/{id}/attachments`.
- Transfer: `GET|POST /api/v1/transfers`, `GET /api/v1/transfers/{id}`, `POST /api/v1/transfers/{id}/decision`.
- Testing: `GET|POST /api/v1/testing-records`, `GET|PUT /api/v1/testing-records/{id}`, `POST /submit`, `POST /review`.
- Bảng hiện có: 6 bảng nền/intake, `sample_transfers`, `sample_transfer_items`, `testing_records`, `testing_equipment`, `testing_reagents`, `testing_measurements`.
- Schema hiện dùng SQL thủ công + Hibernate `validate`; chưa có Flyway/Liquibase.

## Xác minh gần nhất

- Backend `mvn test` trong Docker Maven/JDK 21: pass.
- Frontend `npm run build`: pass.
- Playwright/MySQL: login, intake, transfer, tạo/submit/review biên bản 5 và đồng bộ trạng thái mẫu: pass.
- Mobile 390px và desktop 1440px: không horizontal overflow; menu mở/đóng, sidebar đúng viewport, footer/logo hiển thị; console không có lỗi.
- Có unit test service cho transfer và testing lifecycle; chưa có bộ integration/Testcontainers hoàn chỉnh.

## Việc tiếp theo

1. Chốt role/người có quyền duyệt và phát hành kết quả.
2. Triển khai `result_releases`, version/checksum và PDF bất biến.
3. Sau đó triển khai tra cứu khách bằng mã + đuôi số điện thoại, chỉ trả dữ liệu đã phát hành.
4. Trước production: migration tool, object storage/backup, audit log, rate limit và integration tests.

## Cách nạp ngữ cảnh tiết kiệm token

- Bắt đầu bằng file này; dùng CodeGraph để lấy symbol/call path trước khi đọc source.
- Chỉ đọc `plans/CURRENT.md` khi lập kế hoạch; `docs/API_CONVENTIONS.md` khi đổi API; `docs/DATABASE.md` khi đổi schema; `docs/RBAC.md` khi đổi quyền.
- Chỉ mở skill phù hợp tác vụ. Không tải toàn bộ references của skill hoặc toàn bộ `docs/`.
- `docs/ai/CHANGELOG.md` là lịch sử, không phải file cần đọc đầu phiên.
