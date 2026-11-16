# Thông báo nội bộ realtime

## Phạm vi

- Hiển thị toast CFC cho notification mới nhận qua WebSocket; không thêm âm thanh hoặc thông báo ra ngoài trình duyệt.
- Chuông và trang thông báo vẫn lấy dữ liệu từ REST; toast có thể đóng hoặc mở chi tiết, tối đa ba thông báo, tự đóng sau tám giây.
- Khi WebSocket kết nối lại, đọc lại REST để đồng bộ số chưa đọc và danh sách. Không tạo toast hàng loạt cho thông báo cũ vừa đồng bộ.
- Giữ nguyên người nhận và các quyền backend; không thay đổi gửi Zalo/SMS.

## Kiểm tra

- Mất kết nối và kết nối lại đồng bộ số chưa đọc; event trùng không tạo hai toast hoặc tăng bộ đếm hai lần.
- Toast mở chi tiết/đánh dấu đã đọc, đóng và tự hết hạn; 401/403 hoặc lỗi mạng không giả vờ đánh dấu đã đọc.
- Kiểm tra bốn breakpoint chuẩn, accessibility và build frontend. Thử nghiệm thực tế hai phiên sau khi có dữ liệu thử phù hợp.

## Kết quả 16/11/2026

- Đã triển khai toast, chống event lặp trong cache gần đây và refresh REST khi socket mở/kết nối lại; giữ quyền người nhận hiện có.
- `npm run build` pass. Playwright với WebSocket/REST giả lập kiểm tra sự kiện mới/trùng, đóng, tự đóng, mở chi tiết, đồng bộ sau kết nối lại và bốn breakpoint không tràn ngang toàn trang.
- Chưa kiểm thử WebSocket thực với hai phiên và dữ liệu thử độc lập; chưa kiểm thử riêng lỗi 401/403 hoặc mạng khi đánh dấu đã đọc. Không xem giả lập là E2E production.
