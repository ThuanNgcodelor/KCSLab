# Current Plan — Giai đoạn 2: Tiếp nhận, chuyển mẫu và kết quả

## Goal

Hoàn thiện nền tảng Admin và chuẩn bị triển khai quy trình mẫu thực tế: tiếp nhận phiếu 3, chuyển mẫu bằng phiếu 4, thử nghiệm bằng biên bản 5 và phát hành kết quả PDF/tra cứu khách.

## Scope

- Backend Spring Boot: auth, user, JWT cookie, refresh session, CSRF, Admin-only endpoints.
- Frontend React: login page và user management page.
- Giai đoạn tiếp theo: intake, sample coding, custody, testing record, approval/release và customer lookup.

## Tasks

- [x] Kiểm tra cấu trúc repository hiện tại.
- [x] Tạo root và scoped instructions.
- [x] Tạo `backend-api` custom skill.
- [x] Tạo Project Memory.
- [x] Phân tích kế hoạch gốc và cập nhật định hướng kỹ thuật/nghiệp vụ.
- [x] Tạo PRD, RBAC, DATABASE, API conventions và roadmap.
- [x] Ghi nhận open questions và ranh giới chưa xác nhận.
- [x] Tạo backend Maven/Spring Boot.
- [x] Tạo login bằng email, refresh, logout và `/auth/me`.
- [x] Tạo user management Admin-only; không có registration endpoint.
- [x] Tạo frontend login/user management.
- [x] Dùng guideline asset cho visual direction.
- [x] Chạy backend build/test bằng Docker Maven/JDK 21.
- [x] Tạo schema MySQL và khởi động backend với profile `mysql`.
- [x] Smoke-test login bằng email và `/auth/me`.
- [x] Tạo Admin Portal frontend: dashboard tổng quan, sidebar và khu vực quản trị.
- [x] Tạo module quản lý nhân viên frontend: danh sách, trạng thái tài khoản và modal thêm nhân viên.
- [x] Loại bỏ topbar cũ, tối ưu mobile navigation dạng drawer/backdrop.
- [x] Bổ sung ma trận quyền theo vai trò và chỉnh sửa vai trò/trạng thái nhân viên.
- [x] Sửa lỗi JWT `role` chưa được map thành `ROLE_*`, gây 403 khi Admin quản lý nhân viên.
- [x] Chuyển thêm/sửa nhân viên sang trang riêng, loại bỏ popup.
- [x] Đọc và đối chiếu biểu mẫu 3, 4, 5, quy trình mã hóa CFC-TN-14 và video tại giây 29.
- [x] Cập nhật roadmap theo luồng nghiệp vụ thực tế.
- [ ] Thiết kế ERD/API chi tiết cho receipt, sample, custody, testing record, result, release và public lookup.
- [ ] Xác nhận kênh tiếp nhận thứ hai ngoài phiếu giấy.
- [ ] Xác nhận người có quyền nhận mẫu, kiểm tra, duyệt và phát hành.
- [ ] Triển khai module tiếp nhận và sinh mã mẫu trước.
- [ ] Kiểm thử refresh, logout, CSRF và Admin-only user endpoints trên browser.

## Verification

- Frontend `npm run build`: pass.
- Frontend `npm audit --omit=dev`: 0 vulnerabilities.
- Backend `mvn test` trong Docker: pass.
- Backend MySQL profile: started trên `http://localhost:8080`.
- Frontend Vite: started trên `http://localhost:5173`.

## Blockers

- Không còn blocker cho việc kiểm tra local; host vẫn không có Java/Maven nên backend đang chạy qua Docker.

## Next actions

- Chốt câu hỏi nghiệp vụ còn mở, sau đó triển khai module tiếp nhận và mã hóa mẫu.
