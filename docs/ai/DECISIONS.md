# Quyết định kiến trúc

- Modular monolith Spring Boot, React/Vite, MySQL/Flyway. Java `String` ánh xạ sang `VARCHAR/TEXT` và dùng `utf8mb4`.
- Cookie JWT cho request đã xác thực, refresh session phía server, CSRF cho thao tác ghi phù hợp. Admin cấp tài khoản.
- Số yêu cầu, số của mỗi biểu mẫu, mã mẫu và mã tra cứu có vai trò riêng. **Số/No.** là định danh duy nhất của phiếu tiếp nhận (V16, V18) và cũng là **số phiếu KQKN** (`/R1` cho bản phát hành lại, `-2` cho mẫu thứ hai — cần LAB xác nhận).
- Từ 23/11/2026, theo xác nhận người dùng, trường từng ghi “Nguồn gốc” ở Phiếu yêu cầu được hiểu thống nhất là **Mã vùng trồng**. Giao diện/model dùng `growingArea`; `origin` trong API/DB Phiếu yêu cầu và `orchardCode` ở KQKN là các khóa tương thích của cùng thông tin. `sourceCode` của mã mẫu là khái niệm riêng. Giữ snapshot từng phiếu; không tự ghi đè hồ sơ đã lưu khi sửa phiếu trước.
- Phiếu KQKN có một nguồn duy nhất là mẫu Word `templates/kqkn-dv.docx`. **PDF luôn là DOCX đã điền rồi chuyển bằng LibreOffice**, nên PDF và Word giống nhau. Không vẽ PDF bằng code. Image backend cài font Microsoft để giữ Times New Roman.
- Có 2 cách lập phiếu kết quả: nhập kết quả trên hệ thống (mặc định) hoặc tải file DOCX/PDF có sẵn. Admin duyệt và phát hành; người duyệt độc lập với người lập và người phân tích.
- QR trỏ tới `PUBLIC_LOOKUP_URL` + `?code=…`. Đường dẫn `/tracuu/?code=` phải giữ cố định; đổi tên miền chỉ thay phần host, rồi tạo lại PDF hàng loạt.
- REST và MySQL là nguồn thông báo chính. WebSocket gửi từng người nhận sau commit. Dữ liệu actor dùng FK nullable và avatar qua API xác thực. Kết nối hiện tại nằm trong từng backend instance.
- 25/11/2026: người dùng chốt khóa thông báo theo khâu. ADMIN nhận mọi loại/kể cả tự thao tác; Tiếp nhận nhận yêu cầu mới và phản hồi KCS cho phiếu mình chuyển; KCS chỉ nhận tin khi Phiếu 4 được gửi, có người chuyển + số CFC. Policy áp dụng server-side cả lịch sử, unread và realtime. Giữ bản ghi cũ để audit, ẩn sai khâu; không gửi tin tiếp nhận sớm tới KCS.
- PWA service worker giới hạn ở điều hướng và chỉ cache trang mất mạng. API, tài liệu, avatar và hồ sơ tiếp tục qua mạng.
- Dữ liệu danh sách phân trang, lọc và sắp xếp phía server. Dashboard tổng hợp riêng. Không chia phòng KCS thành nhiều đơn vị trên UI.
- Triển khai:
  - **Linux** dùng Docker Compose cho backend + frontend; MySQL/Redis ở ngoài và script chỉ kiểm tra kết nối.
  - **Windows server** không dùng Docker: Laragon + Java + LibreOffice cài trực tiếp.
- Zalo tiếp nhận dùng outbox/snapshot theo phiếu và consent cùng transaction; gọi mạng ngoài transaction. `ACCEPTED` khác `DELIVERED`; timeout giữ UNKNOWN, không tự POST lại. Webhook lưu bền trước 2xx, polling có giới hạn. Token mã hóa ngoài bảng nghiệp vụ, refresh lease theo App/OA; mất kết quả refresh cần cấp lại quyền. LIVE có adapter và cổng bật riêng, không tái sử dụng queue test. SMS/fallback và thông báo phát hành kết quả chưa triển khai. Xem [runbook](../ZALO_OPERATIONS_RUNBOOK.md).
- Cookie JWT vẫn cần CSRF cho thao tác ghi; Spring Security resource-server tự miễn cả token lấy từ cookie nên matcher cuối được cấu hình riêng và dùng SPA handler. Explicit Authorization Bearer và route webhook có chữ ký giữ ngoại lệ phù hợp.
- Dùng [quy chuẩn frontend](../../source/fe/AGENTS.md) cho bố cục responsive. Lịch sử quyết định nằm trong Git.
