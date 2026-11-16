# Architecture

**Phiên bản:** 0.3 · **Cập nhật:** 13/11/2026 · **Trạng thái:** Đang triển khai

## Runtime hiện tại

- Backend: Java 21, Spring Boot 4.1.2, Maven, Spring MVC/Security/JPA/Validation, JWT resource server.
- Frontend: React 19, Vite 6, JavaScript/JSX, React Router 7.
- Database: MySQL 8.4 là profile mặc định; Flyway chạy migration có version khi startup và Hibernate dùng `validate` để kiểm tra schema.
- Dev runtime: backend có thể chạy bằng Docker Maven/JDK vì host không bắt buộc cài Java/Maven.

## Biên module

- Backend package-by-feature: `auth`, `user`, `intake`, `transfer`, `testing`; `config` và `common` chỉ chứa cross-cutting concerns.
- Frontend feature-based: `auth`, `users`, `intake`, `transfers`, `testing`, `admin`; HTTP client dùng chung ở `shared/api`.
- Auth stateless bằng access JWT trong cookie; refresh token được hash và lưu server-side để rotation/revoke.
- CSRF dùng cookie token; login và refresh được bỏ qua CSRF, các mutation đã đăng nhập gửi CSRF header.
- Attachment dev lưu local, metadata/checksum lưu DB. Production chưa chốt object storage và backup.
- Vite dev lắng nghe `0.0.0.0:5173`; frontend gọi `/api/v1` cùng origin và proxy `/api` đến backend để truy cập qua IP LAN không phụ thuộc hostname.

## Luồng hiện có

```text
Browser → React/Vite → /api/v1 → Spring Security → feature controller/service/repository → MySQL
                                      └────────── attachment metadata → MySQL
                                                   file bytes → local storage (dev)
```

## Quy tắc thay đổi

- Giữ package-by-feature và API contract hiện có; chỉ tạo module khi có vertical slice thực tế.
- Dùng CodeGraph trước khi đọc nhiều source file hoặc đánh giá blast radius.
- Schema mới phải thêm bằng migration Flyway có version trong `source/be/src/main/resources/db/migration/`; không tiếp tục phụ thuộc vào SQL chạy thủ công.
- Không ghi secrets vào source/tài liệu; `.env` chỉ dùng local và phải nằm ngoài Git.
