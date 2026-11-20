# Quyết định kiến trúc

- Modular monolith Spring Boot, React/Vite, MySQL/Flyway. Java `String` ánh xạ sang `VARCHAR/TEXT` và dùng `utf8mb4`.
- Cookie JWT cho request đã xác thực, refresh session phía server, CSRF cho thao tác ghi phù hợp. Admin cấp tài khoản.
- Số yêu cầu, số của mỗi biểu mẫu, mã mẫu và mã tra cứu có vai trò riêng. **Số/No.** là định danh duy nhất của phiếu tiếp nhận (V16, V18) và cũng là **số phiếu KQKN** (`/R1` cho bản phát hành lại, `-2` cho mẫu thứ hai — cần LAB xác nhận).
- Phiếu KQKN có một nguồn duy nhất là mẫu Word `templates/kqkn-dv.docx`. **PDF luôn là DOCX đã điền rồi chuyển bằng LibreOffice**, nên PDF và Word giống nhau. Không vẽ PDF bằng code. Image backend cài font Microsoft để giữ Times New Roman.
- Có 2 cách lập phiếu kết quả: nhập kết quả trên hệ thống (mặc định) hoặc tải file DOCX/PDF có sẵn. Admin duyệt và phát hành; người duyệt độc lập với người lập và người phân tích.
- QR trỏ tới `PUBLIC_LOOKUP_URL` + `?code=…`. Đường dẫn `/tracuu/?code=` phải giữ cố định; đổi tên miền chỉ thay phần host, rồi tạo lại PDF hàng loạt.
- REST và MySQL là nguồn thông báo chính. WebSocket gửi từng người nhận sau commit. Dữ liệu actor dùng FK nullable và avatar qua API xác thực. Kết nối hiện tại nằm trong từng backend instance.
- PWA service worker giới hạn ở điều hướng và chỉ cache trang mất mạng. API, tài liệu, avatar và hồ sơ tiếp tục qua mạng.
- Dữ liệu danh sách phân trang, lọc và sắp xếp phía server. Dashboard tổng hợp riêng. Không chia phòng KCS thành nhiều đơn vị trên UI.
- Triển khai:
  - **Linux** dùng Docker Compose cho backend + frontend; MySQL/Redis ở ngoài và script chỉ kiểm tra kết nối.
  - **Windows server** không dùng Docker: Laragon + Java + LibreOffice cài trực tiếp.
- Zalo/SMS là kênh riêng. Thiết kế dự kiến dùng outbox, idempotency và fallback sau lỗi vĩnh viễn của kênh trước. Xem [thiết kế thông báo khách](../INTAKE_NOTIFICATIONS.md).
- Dùng [quy chuẩn frontend](../../source/fe/AGENTS.md) cho bố cục responsive. Lịch sử quyết định nằm trong Git.
