# Backend instructions

- Backend hiện dùng Java 21 + Spring Boot 4.1.2 + Maven. Đọc `docs/ai/CONTEXT.md` khi cần tổng quan.
- Tổ chức theo package-by-feature trong `com.example.app`.
- `config/` chỉ chứa cấu hình toàn hệ thống; `auth/` chỉ xử lý authentication.
- Feature có thể chứa controller, service, repository, entity, dto, mapper và exception khi thực sự cần.
- Controller không chứa business logic và không trả trực tiếp JPA entity.
- Dùng constructor injection, validation tại API boundary và transaction ở service khi phù hợp.
- Giữ tương thích với Spring Boot/Java hiện có; không thêm Spring Modulith hoặc dependency khác nếu chưa có lý do và xác minh.
- Không bịa package, database schema, token storage hoặc API contract khi chưa kiểm tra code thực tế.
