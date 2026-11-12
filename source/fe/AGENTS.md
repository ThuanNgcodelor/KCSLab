# Frontend instructions

- Trong Giai đoạn 0 chỉ cập nhật tài liệu; chưa tạo component, hook, route, dependency hoặc API client.
- Giữ kiến trúc feature-based trong `src/features/` khi triển khai tính năng.
- Code dùng chung chỉ đưa vào `src/shared/` khi có nhu cầu tái sử dụng rõ ràng.
- Tách API calls khỏi UI; mỗi feature sở hữu API, components, hooks, pages và types liên quan.
- Giữ nguyên router, state management, UI library và ngôn ngữ hiện có sau khi được xác minh.
- Mọi màn hình cần xử lý loading, error, empty state, responsive và accessibility phù hợp.
- Không suy đoán React/Vite/TypeScript version khi chưa có manifest; kiểm tra trước khi thêm code.
