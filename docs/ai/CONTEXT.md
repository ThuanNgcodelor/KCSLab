# Ngữ cảnh CFC Lab

Cập nhật 20/11/2026. Đọc file này để bắt đầu. Khi cần chi tiết, chọn tài liệu theo tác vụ ở [START_HERE.md](START_HERE.md).

## Hệ thống đang chạy

- Backend modular monolith Java 21, Spring Boot, MySQL/Flyway và Redis. Frontend React/Vite. Vite proxy API và WebSocket qua cùng origin.
- Ba quyền `ADMIN`, `RECEPTIONIST` và `TECHNICIAN`. Xem [RBAC](../RBAC.md) và [bản đồ module](MODULE_MAP.md).
- Luồng chính là yêu cầu kiểm nghiệm → tùy chọn kế hoạch lấy mẫu và lấy mẫu hiện trường → tiếp nhận → chuyển mẫu → KCS nhận mẫu → tải DOCX/PDF kết quả → Admin phát hành. Có thể tiếp nhận trực tiếp từ yêu cầu đã hoàn tất. Kế hoạch và phiếu lấy mẫu hiện trường dùng chung số yêu cầu.
- Nhân viên tiếp nhận nhập mã mẫu thủ công. Phân biệt niêm phong mẫu với seal container. KCS đối chiếu hồ sơ bàn giao để nhận hoặc từ chối.
- Khi tải file kết quả hoàn chỉnh, hệ thống lập biên bản và hồ sơ kết quả. Admin xem PDF rồi phát hành trực tiếp. Dữ liệu biên bản và hồ sơ cũ vẫn có thể tra xem.
- Thông báo nội bộ ghi vào MySQL, truyền qua WebSocket sau commit, đồng bộ REST khi kết nối lại và khi tab đang hiện mà mất kênh trực tiếp. V19 bổ sung người thực hiện để hiển thị avatar hiện tại.
- Bản build production có manifest và service worker để cài PWA. Service worker lưu duy nhất trang báo mất mạng. Xem [hướng dẫn vận hành](../DOC_HIEU_HE_THONG.md).

## Kiểm thử và điểm còn mở

- [Trạng thái và cách xác minh](../SYSTEM_STATUS.md), [ca kiểm thử thủ công](../TEST_CASES_TOAN_BO_QUY_TRINH.md).
- DOCX dùng LibreOffice headless trong image backend, theo [run.md](../../run.md). Tránh thao tác test tạo dữ liệu trên môi trường chính.
- Cần nghiệm thu iPhone/Android vật lý và kiểm tra các biểu mẫu DOCX xuất ra trong Word. Production cần chốt sao lưu file và cấu hình HTTPS.
- Gửi Zalo/SMS là [đề xuất riêng](../INTAKE_NOTIFICATIONS.md), đang chờ provider và nội dung đồng ý nhận tin.
