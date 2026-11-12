# Architecture Decisions

## 2026-11-12 — Giai đoạn 0 không tạo code

- Quyết định: chỉ cập nhật Markdown; chưa tạo application, dependency, database, migration hoặc API runtime.
- Lý do: yêu cầu giai đoạn hiện tại là phân tích/thiết kế và quy trình LAB chưa được xác nhận.
- Tác động: mọi module, endpoint và schema trong tài liệu đều là đề xuất.

## 2026-11-12 — Định hướng kỹ thuật mới

- Quyết định đề xuất: Java 21 LTS + Spring Boot stable + Spring Security/JPA/Validation/JWT; React + Vite + JavaScript/JSX; MySQL cần đánh giá.
- Lý do: yêu cầu Giai đoạn 0 mới thay thế định hướng Node.js/TypeScript/PostgreSQL trong tài liệu cũ.
- Tác động: chưa cài dependency; version Spring Boot và database vẫn cần phê duyệt.

## 2026-11-12 — Giữ nguyên vùng source hiện có

- Quyết định: dùng `source/fe` và `source/be` làm vùng frontend/backend hiện tại.
- Lý do: repository đã có hai thư mục này; chưa có code để biện minh cho việc đổi tên hoặc di chuyển.
- Phương án khác: đổi thành `frontend` và `backend`, nhưng có rủi ro phá đường dẫn tooling tương lai.
- Tác động: tài liệu scoped instructions trỏ theo đường dẫn thực tế.

## 2026-11-12 — Chưa tạo module nghiệp vụ rỗng

- Quyết định: chưa tạo `auth`, `user`, `employee`, `booking` khi chưa có application source.
- Lý do: tránh tạo cấu trúc giả và giữ module map trung thực.
- Tác động: các module sẽ được tạo khi có feature thực tế và stack đã xác minh.
