# Quyết định kiến trúc

- Modular monolith Spring Boot, React/Vite, MySQL/Flyway. Java `String` ánh xạ sang `VARCHAR/TEXT` và dùng `utf8mb4`.
- Cookie JWT cho request đã xác thực, refresh session phía server, CSRF cho thao tác ghi phù hợp. Admin cấp tài khoản.
- Số yêu cầu, số của mỗi biểu mẫu, mã mẫu và mã tra cứu có vai trò riêng. Tiếp nhận nhập mã mẫu và KCS xác nhận tại bước bàn giao.
- Luồng kết quả ưu tiên DOCX/PDF đã chuẩn bị. Admin xem trước PDF rồi phát hành trực tiếp. Dữ liệu hồ sơ cũ có đường đọc riêng.
- REST và MySQL là nguồn thông báo chính. WebSocket gửi từng người nhận sau commit. Dữ liệu actor dùng FK nullable và avatar qua API xác thực. Kết nối hiện tại nằm trong từng backend instance.
- PWA service worker giới hạn ở điều hướng và chỉ cache trang mất mạng. API, tài liệu, avatar và hồ sơ tiếp tục qua mạng.
- Dữ liệu danh sách phân trang, lọc và sắp xếp phía server. Dashboard tổng hợp riêng. Không chia phòng KCS thành nhiều đơn vị trên UI.
- Zalo/SMS là kênh riêng. Thiết kế dự kiến dùng outbox, idempotency và fallback sau lỗi vĩnh viễn của kênh trước. Xem [thiết kế thông báo khách](../INTAKE_NOTIFICATIONS.md).
- Dùng [quy chuẩn frontend](../../source/fe/AGENTS.md) cho bố cục responsive. Lịch sử quyết định nằm trong Git.
