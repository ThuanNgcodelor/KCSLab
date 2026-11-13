# Frontend instructions

- Frontend hiện dùng React 19 + Vite 6 + JSX. Đọc `docs/ai/CONTEXT.md` khi cần tổng quan và cập nhật Project Memory sau thay đổi có ý nghĩa.
- Giữ kiến trúc feature-based trong `src/features/` khi triển khai tính năng.
- Code dùng chung chỉ đưa vào `src/shared/` khi có nhu cầu tái sử dụng rõ ràng.
- Tách API calls khỏi UI; mỗi feature sở hữu API, components, hooks, pages và types liên quan.
- Giữ nguyên router, state management, UI library và ngôn ngữ hiện có sau khi được xác minh.
- Mọi màn hình cần xử lý loading, error, empty state, responsive và accessibility phù hợp.
- Dự án hiện không dùng TypeScript hoặc UI library; không tự chuyển stack nếu chưa được yêu cầu.
