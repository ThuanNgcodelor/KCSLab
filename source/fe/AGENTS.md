# Frontend instructions

- Frontend hiện dùng React 19 + Vite 6 + JSX. Đọc `docs/ai/CONTEXT.md` khi cần tổng quan và cập nhật Project Memory sau thay đổi có ý nghĩa.
- Giữ kiến trúc feature-based trong `src/features/` khi triển khai tính năng.
- Code dùng chung chỉ đưa vào `src/shared/` khi có nhu cầu tái sử dụng rõ ràng.
- Tách API calls khỏi UI; mỗi feature sở hữu API, components, hooks, pages và types liên quan.
- Giữ nguyên router, state management, UI library và ngôn ngữ hiện có sau khi được xác minh.
- Mọi màn hình cần xử lý loading, error, empty state, responsive và accessibility phù hợp.
- Dự án hiện không dùng TypeScript hoặc UI library; không tự chuyển stack nếu chưa được yêu cầu.

## Responsive contract cho Admin UI

- Page quản trị mới phải dùng class shell chung `admin-page` (các class cũ `dashboard-page`, `staff-page`, `intake-page` đang được hỗ trợ tương đương). Không tự đặt `max-width` riêng cho từng page nếu không có lý do nghiệp vụ.
- Khung desktop co giãn theo viewport và dừng ở `1680px`; không kéo full-width vô hạn vì bảng/dòng chữ sẽ khó đọc.
- Breakpoint chuẩn: `<=1080px` chuyển sidebar thành mobile drawer; `<=520px` chuyển bảng dày dữ liệu thành card/stack; `>=1600px` tăng spacing/mật độ; `>=2200px` tăng typography cho màn hình 2K/ultrawide.
- Không dùng font dữ liệu nhỏ hơn `12px` ở màn hình `>=2200px`; trạng thái và hành động phải có độ tương phản đọc được kể cả khi disabled.
- Mỗi thay đổi page/layout phải test tối thiểu tại `390x844`, `1024x768`, `1440x900`, `2560x952`; không được có horizontal overflow ngoài vùng bảng được chủ động cho phép cuộn.
- Ưu tiên CSS Grid/Flex và `clamp()`/`min()` cho kích thước fluid; không đo viewport bằng JavaScript để bố trí giao diện.
