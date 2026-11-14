# CFC Lab — AI Context

**Cập nhật:** 14/11/2026 · **Nguồn sự thật ngắn cho phiên AI mới**

## Hiện trạng

- Ứng dụng đang chạy theo modular monolith: Spring Boot 4.1.2/Java 21 ở `source/be`, React 19/Vite 6/JSX ở `source/fe`, MySQL 8.4 cho profile `mysql`.
- Đã hoàn thành vertical slice: auth email/session/CSRF, user, Phase 1/Phiếu 3 có hồ sơ nguồn + nhiều mẫu + bằng chứng ảnh/file, Phase 3/Phiếu 4 có nháp/gửi/nhận và biên bản 5 đến kiểm tra/trả lại. CFC-TN-14 đã được xác nhận là quy trình chuẩn; runtime hiện mới có mã gốc, chưa đủ `-V/-R`, QC/mã mù và xác minh độc lập.
- Ranh giới runtime hiện tại là `TEST_REVIEWED`; chưa có `result_releases`, PDF phát hành, thu hồi/đính chính hoặc tra cứu khách.
- Chưa có gửi tin khách hàng. Phase 1B đã chốt plan dùng ZBS Template Message ưu tiên, SMS Brandname/MultiSMS dự phòng qua database outbox; WebSocket được tách riêng cho nhân viên nội bộ.
- Dashboard Admin đang dùng số liệu/hoạt động mẫu hard-code; không xem đây là báo cáo vận hành thật.
- Module Tiếp nhận hiện chưa có workspace/dashboard theo role, header notification hoặc WebSocket. Plan I1–I6 đã tách tại `plans/intake/`: mọi search/filter/sort/page chạy backend, header chỉ hiện ba tin mới nhất và KCS là một phòng cố định.
- Không có đăng ký công khai. Admin được seed từ biến môi trường; không ghi credentials vào tài liệu.
- Frontend mở từng route theo role: `ADMIN` quản lý user/review; `ADMIN|RECEPTIONIST` tiếp nhận và lập/sửa/gửi Phiếu 4; `ADMIN|TECHNICIAN` thử nghiệm và quyết định nhận mẫu. KCS/PTN không nhìn thấy Phiếu 4 còn `DRAFT`.
- UI dùng tone sáng CFC, logo từ `guideline-assets/cfc-008.png`; admin shell fluid tối đa 1680px, sidebar cố định ở desktop và chuyển drawer từ 1080px trở xuống, typography tăng trên màn 2K; footer liên kết website CFC.

## Luồng đã chạy

1. Admin đăng nhập tại `/login`.
2. Quản lý nhân viên tại `/admin/users`, tạo ở `/admin/users/new`, sửa ở `/admin/users/:id/edit`.
3. Xem phiếu tại `/admin/intake`, tạo tại `/admin/intake/new`, xem/bổ sung ảnh tại `/admin/intake/:id`.
   Danh sách dùng server-side pagination mặc định 20 phiếu/trang và 9 lựa chọn sort; API chỉ nhận sort trong whitelist.
4. Backend lưu customer → receipt → nhiều sample → attachment; hồ sơ nguồn có người/ngày lấy, vùng trồng, cơ sở đóng gói, xe/moóc/container và sinh mã dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`.
5. File dev lưu cục bộ ở `source/be/storage/receipts`; loại hồ sơ, actor, thời điểm chụp, metadata và SHA-256 checksum lưu MySQL.
6. Lập/sửa Phiếu 4 ở trạng thái `DRAFT`, gửi thành `PENDING`, KCS/PTN nhận hoặc từ chối tại `/admin/transfers`; mẫu chỉ bị khóa khi gửi và thành `TRANSFERRED` sau khi nhận.
7. Lập biên bản 5 tại `/admin/testing`; `DRAFT/RETURNED → SUBMITTED → REVIEWED`, đồng bộ mẫu `IN_TESTING → AWAITING_REVIEW → TEST_REVIEWED`.

## API và dữ liệu thực tế

- Auth: `GET /api/v1/auth/csrf`, `POST /login`, `POST /refresh`, `POST /logout`, `GET /me`.
- User: `GET|POST /api/v1/users`, `PATCH /api/v1/users/{id}`; Admin-only.
- Intake: `GET /api/v1/receipts?page=0&size=20&sort=receivedAt,desc`, `POST /receipts`, `GET /receipts/{id}`, `POST /receipts/{id}/attachments`, `GET /attachments/{attachmentId}/content`.
- Transfer: `GET|POST /api/v1/transfers`, `GET|PUT /api/v1/transfers/{id}`, `POST /submit`, `POST /decision`.
- Testing: `GET|POST /api/v1/testing-records`, `GET|PUT /api/v1/testing-records/{id}`, `POST /submit`, `POST /review`.
- Bảng hiện có: 6 bảng nền/intake, `sample_transfers`, `sample_transfer_items`, `testing_records`, `testing_equipment`, `testing_reagents`, `testing_measurements`.
- Schema hiện dùng SQL thủ công + Hibernate `validate`; có script nâng cấp idempotent `db/migration-phase1-phase3.sql`, chưa có Flyway/Liquibase. Chuỗi MySQL dùng `VARCHAR/TEXT` + `utf8mb4`, không dùng `NVARCHAR`.

## Xác minh gần nhất

- Backend `mvn test` trên Java 21: 9/9 pass; có test nhiều mẫu/attachment, pagination + sort whitelist, sửa nháp và quyền ẩn nháp khỏi KCS.
- Frontend `npm run build`: pass, 63 modules.
- MySQL smoke Phase 1/3: tạo phiếu hai mẫu, upload Form 3, tạo/sửa/gửi Phiếu 4, Admin nhận và đồng bộ `TRANSFERRED`: pass.
- Google Chrome thật qua CDP: 7 route intake/transfer, xác nhận 9 sort options + thanh phân trang và route lập Phiếu 4 tương thích contract mới; 1424px/390px không horizontal overflow, console không có lỗi.
- Chưa có bộ integration/Testcontainers hoàn chỉnh; kiểm thử trước đây của Phiếu 5 vẫn pass nhưng chưa chạy lại trong slice Phase 1/3 này.

## Việc tiếp theo

1. Triển khai `plans/intake/OVERVIEW.md` I1: query/aggregate API và index/test; nối dashboard/list rồi xóa dữ liệu hard-code.
2. I2–I3: notification nội bộ lưu bền, header ba tin + page riêng, sau đó WebSocket authenticated user destination và REST reconcile.
3. I4–I5: dashboard/list theo role, form sáu mục Phiếu 3, product catalog cho nhập tay có kiểm soát và draft/completion.
4. Sau đó làm N0–N2 customer notification trong `docs/INTAKE_NOTIFICATIONS.md`, rồi Phase 2/gate mã hóa trong `docs/SAMPLE_CODING.md`.
5. Cuối cùng mới separation of duties, `result_releases`, PDF/QR và public lookup.

## Cách nạp ngữ cảnh tiết kiệm token

- Bắt đầu bằng file này; dùng CodeGraph để lấy symbol/call path trước khi đọc source.
- Chỉ đọc `plans/CURRENT.md` khi lập kế hoạch; đọc `plans/intake/OVERVIEW.md` và role plan tương ứng khi làm workspace tiếp nhận/WebSocket nội bộ; đọc `docs/INTAKE_NOTIFICATIONS.md` khi làm Zalo/SMS; `docs/API_CONVENTIONS.md` khi đổi API; `docs/DATABASE.md` khi đổi schema; `docs/RBAC.md` khi đổi quyền.
- Chỉ mở skill phù hợp tác vụ. Không tải toàn bộ references của skill hoặc toàn bộ `docs/`.
- `docs/ai/CHANGELOG.md` là lịch sử, không phải file cần đọc đầu phiên.
