# Project Changelog

## 2026-11-12

- Cài và validate 5 external skills vào `.agents/skills/`: frontend-design, react-best-practices, web-design-guidelines, java-spring-best-practices và spring-boot.
- Xóa `spring-boot` của SivaLabs vì trùng phạm vi với `java-spring-best-practices`; giữ lại bộ skill tối thiểu gồm 4 external skills và 1 skill nội bộ.
- Hoàn thành tài liệu Giai đoạn 0: PRD, RBAC, database/ERD logic, API conventions và roadmap.
- Cập nhật kế hoạch gốc lên phiên bản 1.1; thay định hướng Node.js/TypeScript/PostgreSQL bằng đề xuất Java 21/Spring Boot, React JSX và MySQL cần đánh giá.
- Không tạo source code, dependency, database, migration hoặc API runtime.
- Thiết lập AI instructions, Project Memory và custom `backend-api` skill.
- Ghi nhận trạng thái các external skills chưa cài trong `docs/ai/SKILLS.md`.
- Tạo khung frontend/backend theo cấu trúc hiện có.
- Verification: cấu trúc repository đã kiểm tra; chưa có build/test do chưa có application source.
