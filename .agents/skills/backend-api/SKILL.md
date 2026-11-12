---
name: backend-api
description: Thiết kế hoặc chỉnh sửa REST API Spring Boot theo package-by-feature trong repository này, khi đã xác minh được stack và module liên quan.
---

# Backend API skill

Áp dụng cho endpoint mới, thay đổi API contract có chủ đích, hoặc sửa logic REST trong `source/be`. Không áp dụng để tự ý khởi tạo framework, nâng version hay tạo module rỗng.

## Quy trình

1. Đọc `AGENTS.md`, `source/be/AGENTS.md`, `docs/ai/STATE.md` và `plans/CURRENT.md`.
2. Xác minh `pom.xml`/`build.gradle`, Java/Spring Boot version, package root, security, persistence và test trước khi sửa.
3. Xác định feature sở hữu endpoint. Giữ controller, service, repository, entity và DTO trong feature đó; chỉ đặt code dùng chung thật sự vào `common/`.
4. Tách request/response DTO khỏi entity. Áp dụng validation ở boundary, constructor injection và transaction tại service khi cần.
5. Dùng exception mapping thống nhất; không nuốt lỗi hoặc trả stack trace ra API.
6. Phân biệt authentication trong `auth/` với authorization policy ở security config và nghiệp vụ tài khoản trong feature phù hợp.
7. Thêm hoặc cập nhật test cho happy path, validation, authorization và lỗi chính; giữ nguyên contract ngoài phạm vi yêu cầu.
8. Cập nhật API documentation nếu repository đã có cơ chế tài liệu.
9. Chạy test/build khả dụng và ghi kết quả trung thực vào Project Memory.

## Ràng buộc

- Không đổi database schema, token storage, package name hoặc framework version nếu chưa được yêu cầu.
- Không tạo dependency mới chỉ để giải quyết một endpoint đơn giản.
- Không truy cập trực tiếp implementation nội bộ của feature khác nếu có thể dùng public interface.
- Nếu thiếu manifest hoặc source code, dừng ở phân tích/kế hoạch và ghi rõ blocker thay vì tạo code giả.
