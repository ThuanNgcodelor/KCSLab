# CFC Lab — AI Context

## Cập nhật workspace KCS — 16/11/2026

- Có workspace `/admin/receiving` cho Technician: dashboard, danh sách Phiếu 4, chi tiết và gallery file, quyết định nhận/từ chối. API projection riêng `/api/v1/receiving/**` dành cho Admin/Technician; receipt API không mở thêm quyền.
- Migration `V5__receiving_query_indexes.sql` thêm hai index transfer; quyết định bàn giao dùng khóa hàng, không thêm cột version.
- Backend 29 test pass trên MySQL/Redis Docker tạm với `KCS_ISOLATED_DB=true` (gồm test hai giao dịch và security); frontend build pass. Chrome đã kiểm tra không tràn ngang trên bốn kích thước với API mô phỏng; chưa chạy E2E thật với hai role và file upload.
- API transfer cũ giữ contract cho Admin/Receptionist và trả `customerName=null` với Technician; receiving DTO mới không có trường này. File gốc/ghi chú vẫn có thể chứa PII. Dashboard đã nhóm xu hướng theo ngày tại database; chưa load test quy mô production.

## Thứ tự bàn giao và xác minh mã — 17/11/2026

- Tiếp nhận hoàn tất dán nhãn và gửi mã (`PENDING_VERIFICATION`) trước khi lập/gửi Phiếu 4.
- KCS nhận bàn giao trước, sau đó đối chiếu/xác minh mã trên Phiếu 4. Chỉ mẫu `VERIFIED` mới xuất hiện trong ứng viên lập Biên bản thử nghiệm/Phiếu 5.
- Không dùng xác minh mã của người tiếp nhận làm điều kiện chặn bàn giao; đây là xác minh độc lập sau bàn giao. Test Docker Java 21/MySQL 8.4/Redis đã pass `Tests run: 85, Failures: 0, Errors: 0, Skipped: 1`; frontend build cũng đã pass.

**Cập nhật:** 17/11/2026 · **Nguồn sự thật ngắn cho phiên AI mới**

> Đọc `docs/ai/START_HERE.md` và `docs/SYSTEM_STATUS.md` trước. Phần kiểm thử bên dưới là kết quả lịch sử, không phải test mới; đối chiếu source khi làm việc.

## Hiện trạng

- Ứng dụng đang chạy theo modular monolith: Spring Boot 4.1.2/Java 21 ở `source/be`, React 19/Vite 6/JSX ở `source/fe`, MySQL 8.4 cho profile `mysql`.
- Đã hoàn thành vertical slice: auth email/session/CSRF, user, Phase 1/Phiếu 3 có hồ sơ nguồn + nhiều mẫu + bằng chứng ảnh/file, Phase 3/Phiếu 4 có nháp/gửi/nhận và biên bản 5 đến kiểm tra/trả lại. CFC-TN-14 đã được xác nhận là quy trình chuẩn; runtime hiện mới có mã gốc, chưa đủ `-V/-R`, QC/mã mù và xác minh độc lập.
- Ranh giới thử nghiệm là `TEST_REVIEWED`; đã có code chưa commit cho nháp `result_releases`, nhưng chưa có PDF phát hành, thu hồi/đính chính hoặc tra cứu khách. Export Phiếu 3 đã sửa XML nhưng vẫn cần test mở Word thật.
- Chưa có gửi tin khách hàng. Phase 1B đã chốt plan dùng ZBS Template Message ưu tiên, SMS Brandname/MultiSMS dự phòng qua database outbox; WebSocket được tách riêng cho nhân viên nội bộ.
- Có dashboard Receptionist riêng ở `/admin/intake`, danh sách ở `/admin/intake/receipts`; KCS có dashboard, danh sách và projection xem file ở `/admin/receiving`. Dashboard Admin toàn hệ thống chưa nghiệm thu hoàn chỉnh. Notification center lưu bền, trang riêng và WebSocket nội bộ.
- Không có đăng ký công khai. Admin được seed từ biến môi trường; không ghi credentials vào tài liệu.
- Frontend mở từng route theo role: `ADMIN` quản lý user/review; `ADMIN|RECEPTIONIST` tiếp nhận và lập/sửa/gửi Phiếu 4; `ADMIN|TECHNICIAN` thử nghiệm và quyết định nhận mẫu. KCS/PTN không nhìn thấy Phiếu 4 còn `DRAFT`; Technician không có quyền gọi đầy đủ API Phiếu 3, nhưng notification có thể chứa tên người ghi nhận.
- Người dùng xác nhận `TECHNICIAN` vừa nhận mẫu vừa lập Phiếu 5 cùng `ADMIN`; không tạo role mới. Cần dashboard riêng cho từng role, Admin xem tổng hợp toàn hệ thống, và bổ sung upload nhiều file vào Phiếu 5.
- UI dùng tone sáng CFC, logo từ `guideline-assets/cfc-008.png`; admin shell fluid tối đa 1680px, sidebar cố định ở desktop và chuyển drawer từ 1080px trở xuống, typography tăng trên màn 2K; footer liên kết website CFC.

## Luồng đã chạy

1. Admin đăng nhập tại `/login`.
2. Quản lý nhân viên tại `/admin/users`, tạo ở `/admin/users/new`, sửa ở `/admin/users/:id/edit`.
3. Dashboard Receptionist tại `/admin/intake`, danh sách tại `/admin/intake/receipts`, tạo tại `/admin/intake/new`, xem/bổ sung ảnh tại `/admin/intake/:id`.
   Danh sách dùng server-side pagination mặc định 20 phiếu/trang và 9 lựa chọn sort; API chỉ nhận sort trong whitelist.
4. Backend lưu customer → receipt → nhiều sample → attachment; hồ sơ nguồn có người/ngày lấy, vùng trồng, cơ sở đóng gói, xe/moóc/container và sinh mã dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`.
5. File dev lưu cục bộ ở `source/be/storage/receipts`; loại hồ sơ, actor, thời điểm chụp, metadata và SHA-256 checksum lưu MySQL. Avatar lưu ở `source/be/storage/avatars`; chưa có object storage/backup production.
6. Lập/sửa Phiếu 4 ở trạng thái `DRAFT`, gửi thành `PENDING`, KCS/PTN nhận hoặc từ chối tại `/admin/transfers`; mẫu chỉ bị khóa khi gửi và thành `TRANSFERRED` sau khi nhận.
7. Lập biên bản 5 tại `/admin/testing`; `DRAFT/RETURNED → SUBMITTED → REVIEWED`, đồng bộ mẫu `IN_TESTING → AWAITING_REVIEW → TEST_REVIEWED`.

## API và dữ liệu thực tế

- Auth: `GET /api/v1/auth/csrf`, `POST /login`, `POST /refresh`, `POST /logout`, `GET /me`.
- User: `GET|POST /api/v1/users`, `PATCH /api/v1/users/{id}`; Admin-only.
- Intake: `GET /api/v1/receipts?page=0&size=20&sort=receivedAt,desc`, `POST /receipts`, `GET /receipts/{id}`, `POST /receipts/{id}/attachments`, `GET /attachments/{attachmentId}/content`, `GET /receipts/{id}/form-3` (export DOCX đang cần kiểm tra Word thật).
- Transfer: `GET|POST /api/v1/transfers`, `GET|PUT /api/v1/transfers/{id}`, `POST /submit`, `POST /decision`.
- Testing: `GET|POST /api/v1/testing-records`, `GET|PUT /api/v1/testing-records/{id}`, `POST /submit`, `POST /review`.
- Bảng hiện có: 6 bảng nền/intake, `sample_transfers`, `sample_transfer_items`, `testing_records`, `testing_equipment`, `testing_reagents`, `testing_measurements`.
- Schema hiện được quản lý bằng Flyway: baseline là `db/migration/V1__baseline_schema.sql`, migration mới chạy tự động khi backend startup. Hibernate dùng `validate`; các file `db/migration-*.sql` cũ chỉ giữ để tham chiếu. Chuỗi MySQL dùng `VARCHAR/TEXT` + `utf8mb4`, không dùng `NVARCHAR`.

## Xác minh gần nhất

- Backend `mvn test` trên Java 21: 9/9 pass; có test nhiều mẫu/attachment, pagination + sort whitelist, sửa nháp và quyền ẩn nháp khỏi KCS.
- Frontend `npm run build`: pass, 63 modules.
- MySQL smoke Phase 1/3: tạo phiếu hai mẫu, upload Form 3, tạo/sửa/gửi Phiếu 4, Admin nhận và đồng bộ `TRANSFERRED`: pass.
- Google Chrome thật qua CDP: 7 route intake/transfer, xác nhận 9 sort options + thanh phân trang và route lập Phiếu 4 tương thích contract mới; 1424px/390px không horizontal overflow, console không có lỗi.
- Chưa có bộ integration/Testcontainers hoàn chỉnh; kiểm thử trước đây của Phiếu 5 vẫn pass nhưng chưa chạy lại trong slice Phase 1/3 này.

## Việc tiếp theo

1. Nghiệm thu workspace KCS với backend/file thật; bổ sung upload nhiều file Phiếu 5 cho `TECHNICIAN`/`ADMIN` theo plan.
2. Hoàn thiện dashboard `ADMIN` tổng hợp toàn hệ thống; dashboard Receptionist riêng đã có.
3. Sửa và nghiệm thu export DOCX Phiếu 3 bằng Word thật.
4. Hoàn tất I4–I6 và Phase 2/5A: draft/completion, catalog, sequence nguyên tử, xác minh độc lập, `-V/-R`, QC/mã mù.
5. Làm ZBS/SMS, `result_releases`, PDF/QR và public lookup theo các plan chuyên biệt.

## Cách nạp ngữ cảnh tiết kiệm token

- Bắt đầu bằng file này; dùng CodeGraph để lấy symbol/call path trước khi đọc source.
- Chỉ đọc `plans/CURRENT.md` khi lập kế hoạch; đọc `plans/intake/OVERVIEW.md` và role plan tương ứng khi làm workspace tiếp nhận/WebSocket nội bộ; đọc `docs/INTAKE_NOTIFICATIONS.md` khi làm Zalo/SMS; `docs/API_CONVENTIONS.md` khi đổi API; `docs/DATABASE.md` khi đổi schema; `docs/RBAC.md` khi đổi quyền.
- Chỉ mở skill phù hợp tác vụ. Không tải toàn bộ references của skill hoặc toàn bộ `docs/`.
- `docs/ai/CHANGELOG.md` là lịch sử, không phải file cần đọc đầu phiên.

## Bổ sung KCS 16/11/2026

- Chi tiết receiving hiển thị riêng số lượng/khối lượng và thông tin bàn giao có sẵn; reload sau 409 thất bại không còn báo tải thành công. Dashboard hiển thị thời gian chờ, không đặt SLA.
- Thêm test HTTP/MySQL chặn truy cập attachment khác receipt, KPI/phân trang 21 phiếu và ranh giới receivedAt. Bộ test Java 21 chạy trên MySQL/Redis tạm: 32 test, 0 failures/errors/skipped; frontend build pass.
- Chưa nghiệm thu Chrome E2E bản cuối, notification sau commit/rollback và đầy đủ sort/ranh giới thời gian. Thiếu ảnh vẫn là giả định MVP; chính sách tên khách trong notification cần LAB xác nhận.

## Hồ sơ kết quả — bước nền

- V6 và API nội bộ chỉ tạo/đọc nháp cho Phiếu 5 `REVIEWED`; chưa có upload hoặc phát hành. Người dùng yêu cầu chặn phát hành đến khi hoàn tất gate mã hóa CFC-TN-14.
- QR và tra mã nhập tay tương lai đều yêu cầu 4 số cuối SĐT; mặc định chỉ PDF public, Admin có thể bật Word/Excel; 10 MB/file và 10 file/hồ sơ. Tra cứu và các bảo vệ công khai chưa triển khai.
- `source/public/tracuu/` là landing xem trước không login; form bị khóa, chưa tra cứu được. Không public trước khi hoàn thành và kiểm thử bảo mật backend/phát hành.
