# Trạng thái hệ thống

Cập nhật 20/11/2026.

| Hạng mục | Trạng thái trong mã nguồn |
|---|---|
| Tài khoản | Ba vai trò, Admin quản lý nhân viên. Mỗi vai trò tự sửa tên, ảnh và mật khẩu |
| Yêu cầu, lấy mẫu và tiếp nhận | Liên kết theo số yêu cầu, có nhánh tiếp nhận trực tiếp. Tiếp nhận nhập mã mẫu, nhập riêng niêm phong mẫu và seal container |
| Chuyển mẫu và KCS | Lập nháp, gửi, xem ảnh hồ sơ và xác nhận nhận hoặc từ chối |
| Thử nghiệm và kết quả | Nhập Đơn vị/Kết quả/Ghi chú trên hệ thống (thông tin mẫu tự điền) hoặc tải DOCX/PDF. Phiếu KQKN tạo từ mẫu Word, PDF chuyển bằng LibreOffice, có trang ảnh mẫu. Số phiếu = Số/No. Admin duyệt, phát hành, trả lại, thu hồi, lập phiên bản mới, tạo lại PDF (từng phiếu hoặc hàng loạt) |
| Tra cứu | `/tracuu/` tìm phiếu đã phát hành bằng mã tra cứu/QR và bốn số cuối điện thoại. QR hiện trỏ IP nội bộ, chờ tên miền |
| Triển khai | Linux: Docker Compose (backend + LibreOffice, frontend nginx). Windows server: Laragon + Java + LibreOffice (chưa thử trên máy thật) |
| Thông báo | Lưu nội bộ theo người nhận. Có avatar người thực hiện, WebSocket, đồng bộ REST và đồng bộ giữa các tab trên cùng trình duyệt |
| PWA | Bản production có manifest, icon, hướng dẫn cài và trang mất mạng |
| Gửi khách qua Zalo/SMS | Đang ở giai đoạn thiết kế |

## Kiểm thử

- Backend MySQL/Redis cô lập qua `bash scripts/run-be-tests-docker.sh`. Kết quả ngày 20/11/2026: 107 bài đạt, 1 bài bỏ qua (chạy trong image có LibreOffice vì PDF tạo từ DOCX). Hai kết nối WebSocket xác thực thật cùng nhận sự kiện sau commit. Giao dịch rollback không phát tin.
- Frontend production build thành công. Chrome chạy 55 lượt đường dẫn theo vai trò ở 6 kích thước, gồm 320 px và màn hình ngang. Dữ liệu UI được mô phỏng. Chrome xác nhận manifest cài đặt, thao tác thử lại khi mất mạng và cache chỉ chứa trang ngoại tuyến.
- Đối chiếu màn hình và luồng nghiệp vụ bằng [bộ ca kiểm thử](TEST_CASES_TOAN_BO_QUY_TRINH.md).

## Nghiệm thu triển khai

- Thử đăng nhập và cài PWA trên iPhone dùng Safari và Android dùng Chrome qua HTTPS.
- Mở file KQKN DOCX trên Microsoft Word. PDF đã được kiểm tra khớp DOCX khi chuyển bằng LibreOffice.
- Thử luồng nhập kết quả bằng tài khoản KCS và Admin thật. Hiện mới kiểm tra giao diện bằng API giả lập và test backend.
- Có tên miền: dựng Cloudflare Tunnel, đổi `PUBLIC_LOOKUP_URL`, tạo lại PDF hàng loạt, thử QR từ ngoài mạng.
- Kiểm tra phương án sao lưu MySQL và thư mục đính kèm. Với nhiều backend instance, WebSocket hiện lưu kết nối theo từng instance. REST giúp trình duyệt đồng bộ lại khi cần.
