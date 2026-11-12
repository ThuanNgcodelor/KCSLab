# Project Changelog

## 2026-11-12

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
