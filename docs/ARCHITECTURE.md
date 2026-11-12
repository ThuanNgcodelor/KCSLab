# Architecture

**Phiên bản:** 0.1 · **Ngày:** 12/11/2026 · **Trạng thái:** Thiết kế sơ bộ, chưa phê duyệt

## Current state

Repository hiện mới có khung thư mục và tài liệu; chưa có backend/frontend implementation để mô tả luồng tích hợp cụ thể. Giai đoạn 0 không tạo code.

## Target direction

- Backend: Java 21 LTS + Spring Boot stable là định hướng; modular monolith, package-by-feature. `config/` chứa cấu hình toàn hệ thống; `auth/` chỉ authentication; feature nghiệp vụ sở hữu controller/service/repository/entity/dto khi cần.
- Frontend: React + Vite + JavaScript ES Modules/JSX; feature-based. Mỗi feature sở hữu API, components, hooks, pages và validation; `shared/` chỉ chứa phần tái sử dụng rõ ràng.
- Database: MySQL là phương án chính cần đánh giá; PostgreSQL là phương án thay thế. Chưa tạo database.
- Public API: `/api/v1`, resource số nhiều, pagination và error contract được mô tả trong `docs/API_CONVENTIONS.md`.

## Xác minh công nghệ tại Giai đoạn 0

Tài liệu Spring chính thức hiện liệt kê Spring Boot 4.1.2; trang system requirements ghi Java tối thiểu 17 và tương thích đến Java 27. Đây chỉ là thông tin tham khảo tại ngày 12/11/2026, không phải quyết định cài đặt. Dự án vẫn phải chốt version stable, Maven và compatibility matrix trước Giai đoạn 1.

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- Project Memory là snapshot và kế hoạch hỗ trợ Agent, không thay thế source code hoặc Git history.

## Migration rule

Không di chuyển hoặc refactor hàng loạt để khớp sơ đồ. Chỉ tạo module khi có feature thực tế, xác minh stack trước và kiểm tra behavior/API contract sau mỗi thay đổi.
