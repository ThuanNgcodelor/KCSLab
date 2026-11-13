# Architecture Decisions

## 13/11/2026 — Context tối giản cho AI

- `docs/ai/CONTEXT.md` là nguồn tổng quan ngắn mặc định.
- CodeGraph được dùng trước grep/read cho câu hỏi về source; tài liệu chuyên biệt chỉ đọc khi task cần.
- Changelog và toàn bộ skill references không được nạp mặc định để tránh tốn token.

## 12/11/2026 — Stack và cấu trúc đã chốt

- Java 21 + Spring Boot 4.1.2 + Maven; React 19 + Vite 6 + JSX; MySQL profile cho dữ liệu thực.
- Giữ `source/be` và `source/fe`; backend package-by-feature, frontend feature-based.
- Kiến trúc modular monolith, chưa tách microservice.

## 12/11/2026 — Authentication và tài khoản

- Login bằng email; access JWT cookie ngắn hạn, refresh session hash phía server, CSRF cookie và logout revoke.
- Không có public registration; chỉ Admin tạo tài khoản.
- Role hiện có: `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`.

## 12/11/2026 — Tiếp nhận mẫu

- Phiếu 3 là vertical slice nghiệp vụ đầu tiên.
- Mã mẫu dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`, unique và không tái sử dụng.
- Attachment lưu local trong dev, có metadata và SHA-256 checksum; production cần object storage/backup.

## Quyết định còn mở

- Người ký/xác nhận phiếu 4 và người duyệt/phát hành kết quả.
- Có thêm role `LAB_MANAGER`/`REVIEWER` hay không.
- Flyway hay Liquibase; object storage và chính sách retention/backup.
