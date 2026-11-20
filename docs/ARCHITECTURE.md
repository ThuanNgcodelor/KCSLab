# Architecture

**Cập nhật 20/11/2026**

## Runtime hiện tại

- Backend: Java 21, Spring Boot 4.1.2, Maven, Spring MVC/Security/JPA/Validation, JWT resource server.
- Frontend: React 19, Vite 6, JavaScript/JSX, React Router 7.
- Database: MySQL 8.4 là profile mặc định; Flyway chạy migration có version khi startup và Hibernate dùng `validate` để kiểm tra schema.
- Dev runtime: backend có thể chạy bằng Docker Maven/JDK vì host không bắt buộc cài Java/Maven.
- Chạy trên Linux: `docker-compose.yml` gồm backend `.jar` + LibreOffice (`Dockerfile.prod`) và frontend nginx (`source/fe/Dockerfile`), cả hai dùng `network_mode: host`. MySQL/Redis ở ngoài compose. Windows server chạy `java -jar`, LibreOffice và Laragon (Apache proxy `/api`, `/ws`), không Docker.
- Phiếu KQKN: `KqknDocxExporter` điền mẫu Word, `OfficeConverter` (LibreOffice) chuyển PDF, `ReportFileStamper` đóng QR/mã tra cứu. Bản xem trước có watermark.

## Biên module

- Xem [bản đồ module](ai/MODULE_MAP.md) để định vị backend và frontend. HTTP client dùng chung nằm ở `shared/api`.
- Auth stateless bằng access JWT trong cookie; refresh token được hash và lưu server-side để rotation/revoke.
- CSRF dùng cookie token; login và refresh được bỏ qua CSRF, các mutation đã đăng nhập gửi CSRF header.
- Attachment dev lưu local, metadata/checksum lưu DB. Production chưa chốt object storage và backup.
- Frontend dùng cùng origin cho API và WebSocket. Vite proxy `/api` và `/ws` đến backend trong môi trường dev. Proxy production cần hỗ trợ WebSocket Upgrade. Đặt `FRONTEND_ORIGIN` bằng origin người dùng truy cập, ví dụ `https://lab.example.com`; có thể phân cách nhiều origin bằng dấu phẩy.
- WebSocket gửi thông báo đến các phiên của người nhận sau commit. MySQL và API là nguồn dữ liệu dùng khi trình duyệt đồng bộ lại.
- Bản frontend production đăng ký service worker trên origin bảo mật. Chỉ trang hướng dẫn mất mạng được lưu trong CacheStorage.

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
