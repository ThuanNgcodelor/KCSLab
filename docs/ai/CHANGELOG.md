# Project Changelog

## 2026-11-13

- Cập nhật báo cáo sơ bộ hiện hành: xác nhận runtime dừng tại `TEST_REVIEWED`, dashboard còn dữ liệu mẫu và giai đoạn release/PDF chưa có code.
- Bổ sung rủi ro ưu tiên: `TestingRecordService.review` chưa chặn analyst tự kiểm tra; đưa tách người nhập–người duyệt thành gate đầu tiên của Giai đoạn 5.
- Chuẩn hóa responsive Admin UI: shell fluid tối đa 1680px, drawer từ 1080px, tỷ lệ riêng cho màn 2K và checklist viewport 390/1024/1440/2560px.
- Hoàn thành phiếu chuyển mẫu số 4: schema transfer/item, API list/detail/create/decision, giao diện responsive và đồng bộ trạng thái mẫu.
- Hoàn thành vertical slice biên bản thử nghiệm số 5: thiết bị, hóa chất, đường chuẩn/công thức, QC/raw data, submit, review/return và unit test service.
- Thêm route `/admin/testing`, Playwright E2E đến trạng thái mẫu `TEST_REVIEWED`, frontend build và backend test pass.
- Cố định sidebar theo viewport, thêm footer liên kết website CFC; thay huy hiệu chữ bằng logo `guideline-assets/cfc-008.png`.
- Chuyển Admin Portal sang tone sáng CFC để tăng độ đọc; Vite nghe trên `0.0.0.0` và proxy API cùng origin để hỗ trợ IP LAN.
- Chuyển kế hoạch hiện tại sang Giai đoạn 5 — duyệt/phát hành PDF.
- Đồng bộ toàn bộ Project Memory theo source hiện tại; loại bỏ mô tả cũ “placeholder/chưa có runtime”.
- Thêm `docs/ai/CONTEXT.md` làm nguồn context ngắn mặc định và đổi `AGENTS.md` sang đọc tài liệu theo nhu cầu.
- Cập nhật architecture, API, database, RBAC, roadmap và current plan; giai đoạn tiếp theo là phiếu chuyển mẫu số 4.
- Đánh giá chiến lược token: ưu tiên CodeGraph + progressive disclosure; chưa thêm Gravity vì trùng chức năng nếu mục tiêu chỉ là codebase retrieval.
- Tối ưu giao diện Admin trên mobile: container intake responsive, timeline dọc, bảng cuộn trong card, nút full-width và drawer đóng đúng.
- Playwright viewport 390px cho `/admin`, `/admin/users`, `/admin/intake`, `/admin/intake/new`: không còn horizontal overflow; frontend build pass.

## 2026-11-12

- Triển khai module tiếp nhận mẫu đầu tiên: customer, receipt, sample, attachment; API tạo/list/detail/upload và giao diện Admin `/admin/intake`.
- Sinh mã mẫu theo CFC-TN-14 dạng `CT001-SR-DDMMYYYY-SEQ`, lưu metadata/checksum file phiếu 3 và liên kết người tiếp nhận.
- Sửa HTTP client để upload `FormData` không bị gán `Content-Type: application/json`; đổi cột SQL `condition` thành `sample_condition` vì `condition` là từ khóa MySQL.
- Playwright smoke test pass: login Admin → tạo phiếu → upload DOCX → kiểm tra mã phiếu/mã mẫu → mở danh sách; dữ liệu tạm đã được dọn.

- Tạo schema MySQL tại `source/be/src/main/resources/db/schema.sql` cho `users` và `auth_sessions`.
- Tạo database `cfc_lab`, cấp quyền user kết nối hiện tại và xác minh backend kết nối thành công.
- Backend `mvn test` pass trong Docker Maven/JDK 21; smoke-test login email và `/auth/me` trả HTTP 200.
- Đã start backend tại `http://localhost:8080` và frontend Vite tại `http://localhost:5173`.
- Tạo Admin Portal frontend với dashboard vận hành, hoạt động gần đây, tình trạng phòng lab và quick actions.
- Bổ sung module quản lý nhân viên: thống kê, danh sách tài khoản, khóa/mở khóa và modal tạo nhân viên.
- Thêm routing `/admin` và `/admin/users`, giữ `/users` chuyển hướng tương thích.
- Loại bỏ topbar cũ; mobile navigation chuyển thành drawer có backdrop, trạng thái mở/đóng và đóng sau khi chuyển trang.
- Bổ sung ma trận quyền cho ba vai trò ADMIN, RECEPTIONIST và TECHNICIAN; hỗ trợ sửa vai trò, bật/tắt tài khoản và cập nhật mật khẩu.
- Sửa Spring Security JWT converter để claim `role=ADMIN` được map thành `ROLE_ADMIN`; xác minh login, GET users và POST users lần lượt trả 200/200/201.
- Chuyển luồng thêm/sửa nhân viên từ popup sang `/admin/users/new` và `/admin/users/:id/edit`.
- Đọc và đối chiếu các biểu mẫu tiếp nhận/chuyển mẫu/biên bản thử nghiệm, quy trình mã hóa CFC-TN-14 và frame video 00:29.
- Cập nhật kế hoạch, roadmap, PRD, RBAC, database và API conventions theo luồng tiếp nhận → chuyển mẫu → thử nghiệm → duyệt → PDF → tra cứu khách.

- Bắt đầu Giai đoạn 1 theo yêu cầu: triển khai login bằng email và user management Admin-only, không có đăng ký.
- Frontend build pass; npm audit pass 0 vulnerabilities. Backend chưa xác minh do môi trường thiếu Java/Maven.
- Cài và validate 5 external skills vào `.agents/skills/`: frontend-design, react-best-practices, web-design-guidelines, java-spring-best-practices và spring-boot.
- Xóa `spring-boot` của SivaLabs vì trùng phạm vi với `java-spring-best-practices`; giữ lại bộ skill tối thiểu gồm 4 external skills và 1 skill nội bộ.
- Hoàn thành tài liệu Giai đoạn 0: PRD, RBAC, database/ERD logic, API conventions và roadmap.
- Cập nhật kế hoạch gốc lên phiên bản 1.1; thay định hướng Node.js/TypeScript/PostgreSQL bằng đề xuất Java 21/Spring Boot, React JSX và MySQL cần đánh giá.
- Không tạo source code, dependency, database, migration hoặc API runtime.
- Thiết lập AI instructions, Project Memory và custom `backend-api` skill.
- Ghi nhận trạng thái các external skills chưa cài trong `docs/ai/SKILLS.md`.
- Tạo khung frontend/backend theo cấu trúc hiện có.
- Verification: cấu trúc repository đã kiểm tra; chưa có build/test do chưa có application source.
