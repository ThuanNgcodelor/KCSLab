# Current Plan — Giai đoạn 1: User và Login

## Goal

Triển khai authentication và quản lý tài khoản nhân viên bằng email; không có đăng ký công khai.

## Scope

- Backend Spring Boot: auth, user, JWT cookie, refresh session, CSRF, Admin-only endpoints.
- Frontend React: login page và user management page.
- Không triển khai các module LAB khác.

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

- Mở frontend và kiểm thử login/user trên browser; sau đó bổ sung integration tests.
