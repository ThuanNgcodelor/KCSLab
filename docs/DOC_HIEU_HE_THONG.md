# CFC Lab — Hệ thống hiện tại và việc cần làm

**Cập nhật:** 17/11/2026. Đây là trang dành cho người dùng muốn nắm nhanh dự án. Chi tiết kỹ thuật nằm ở `SYSTEM_STATUS.md`; kế hoạch công việc ở `../plans/SYSTEM_COMPLETION_ROADMAP.md`.

**Quan trọng:** “đã có và thử được” không có nghĩa là đã nghiệm thu để vận hành thật. Một số code, trong đó có phần tải phiếu kết quả, vẫn chưa commit.

## 1. Hệ thống đang làm gì?

```text
Tiếp nhận mẫu → Chuyển mẫu → KCS nhận mẫu → Thử nghiệm
→ Lập hồ sơ kết quả → Admin duyệt/phát hành → Khách tra cứu
```

| Người dùng | Công việc |
|---|---|
| Tiếp nhận | Tạo Phiếu 3, ghi nhận mẫu và lập Phiếu 4 để chuyển mẫu. |
| KCS / Technician | Đối chiếu, nhận hoặc từ chối mẫu; lập Phiếu 5 và hồ sơ kết quả. |
| Admin | Quản lý nhân viên và duyệt/phát hành kết quả theo quyền. Dashboard tổng hợp còn cần hoàn thiện. |
| Khách hàng | Tra cứu hồ sơ đã phát hành bằng mã và 4 số cuối điện thoại; có thể tải file được phép. Chức năng này mới được thử trong môi trường cô lập, chưa phải đã triển khai công khai. |

## 2. Đã làm đến đâu?

- **Đăng nhập và tài khoản:** có đăng nhập, ba role, thông tin cá nhân, đổi mật khẩu và phiên đăng nhập.
- **Tiếp nhận / Phiếu 3:** đã tạo được phiếu, nhiều mẫu và ảnh/file. Có dashboard riêng. Lưu nháp/hoàn tất còn cần làm tiếp.
- **Chuyển mẫu / Phiếu 4:** có nháp, gửi, nhận hoặc từ chối. KCS có dashboard, danh sách, chi tiết và xem bằng chứng.
- **Mã hóa mẫu:** đã có cấp số nguyên tử, xác minh hai người, lịch sử và gate chặn mẫu chưa xác minh. Chưa đủ CFC-TN-14: còn `-V/-R`, QC/mã mù và các quy tắc LAB cần xác nhận.
- **Thử nghiệm / Phiếu 5:** đã có biên bản và luồng gửi/kiểm tra. Chưa hoàn tất đính kèm nhiều file trực tiếp vào Phiếu 5.
- **Hồ sơ kết quả:** có nháp, gửi duyệt, phát hành, thu hồi và phiên bản. Có thể nhập bảng kết quả hoặc tải phiếu DOCX/PDF; DOCX cần LibreOffice để chuyển thành PDF. Phần tải phiếu còn chưa commit và chưa nghiệm thu đầy đủ.
- **Tra cứu:** đã có trang và API, thử được bằng dữ liệu giả, bao gồm tải PDF sau khi nhập mã + 4 số cuối điện thoại. Chưa đưa ra Internet, chưa có domain/TLS và chưa kiểm thử bảo mật production đầy đủ.
- **Thông báo:** đã có thông báo nội bộ và realtime; chưa triển khai Zalo ZBS/SMS cho khách.

## 3. Kết quả kiểm tra gần nhất

Ngày 17/11, trên MySQL/Redis thử nghiệm riêng:

- Backend: **83 test, 0 lỗi, 1 bỏ qua**; test bị bỏ qua cần đường dẫn mẫu PDF.
- Luồng HTTP nhập bảng kết quả: **77/77 kiểm tra đạt**.
- Chrome MCP với file DOCX giả: tải lên → gửi duyệt → Admin phát hành → khách tra cứu và tải PDF thành công.
- Trang hồ sơ kết quả và trang tra cứu không tràn ngang toàn trang ở 390×844, 1024×768, 1440×900 và 2560×952.

Đây là kiểm tra trong môi trường tạm; không dùng kết quả đó để khẳng định dữ liệu thật hoặc toàn bộ chức năng đã nghiệm thu.

## 4. Lỗi và việc chưa xong quan trọng

1. **Lỗi khi duyệt hồ sơ:** file DOCX/PDF nguồn bị hiển thị như file có thể chọn công khai. Nếu Admin tích chọn rồi phát hành, backend báo lỗi. Không tích chọn thì đã phát hành thử thành công. Cần sửa giao diện và kiểm tra lại.
2. **Phiếu Word/PDF:** chưa nghiệm thu Phiếu 3 bằng Microsoft Word thật. PDF chuyển từ mẫu KQKN bằng LibreOffice từng có thêm một trang gần như trống; cần đối chiếu Word, kiểm tra chữ ký/bố cục và quét QR trên bản in bằng điện thoại.
3. **Quy trình thật:** cần nghiệm thu toàn bộ Tiếp nhận → KCS → Phiếu 5 với ảnh/file thật và hai phiên thông báo realtime; cần kiểm tra các lỗi 401/403/409 trên UI.
4. **Mã hóa:** làm tiếp `-V/-R`, QC/mã mù sau khi LAB xác nhận. Quy tắc mẫu cũ được đánh dấu verified là giả định cần duyệt.
5. **Hồ sơ kết quả và public:** kiểm tra quyền/tài liệu, an toàn file, quét virus, backup, rate limit khi chạy nhiều máy chủ và bảo vệ hạ tầng trước khi public. PDF kết quả có thể chứa thông tin khách theo biểu mẫu; không hiểu “JSON không trả PII” là PDF không có PII.
6. **Dashboard Admin, ZBS/SMS và frontend:** còn việc phải hoàn thiện. Trang hồ sơ có `main` lồng nhau; nội dung hướng dẫn tra cứu còn câu chữ cũ.

## 5. Nên làm tiếp theo thứ tự nào?

1. Sửa lỗi chọn công khai nhầm file nguồn và kiểm tra lại luồng phát hành bằng Chrome.
2. Nghiệm thu Phiếu 3 bằng Word thật, PDF chuyển đổi, QR in và hai phiên thông báo; rà lại toàn bộ quy trình bằng dữ liệu thử.
3. LAB xác nhận phần mã hóa còn thiếu, người ký/duyệt, quy tắc số phiếu và chính sách thiếu ảnh; triển khai các phần còn lại.
4. Hoàn thiện bảo mật, lưu trữ và hạ tầng tra cứu; chỉ triển khai public sau khi được duyệt.
5. Hoàn thiện dashboard Admin và ZBS/SMS theo nhu cầu.

## 6. Cách chạy và tài liệu cần đọc

- Cách chạy backend/frontend: `ai/START_HERE.md`. Cấu hình và mật khẩu để trong môi trường máy, không chép vào tài liệu.
- Việc cần làm chi tiết: `../plans/SYSTEM_COMPLETION_ROADMAP.md`.
- Trạng thái, API, quyền và bằng chứng kỹ thuật: `SYSTEM_STATUS.md`, `API_CONVENTIONS.md`, `RBAC.md`.
- Các tài liệu/plan khác là tài liệu chuyên sâu hoặc lịch sử; không cần đọc hết để hiểu trạng thái chung. Nếu thấy mâu thuẫn, đối chiếu ngày, source và kết quả test mới nhất.
- MySQL, Redis và Flyway được dùng cho backend; source hiện có migration V1–V10. **Chưa xác nhận V7–V10 đã chạy trên database làm việc**. Không chạy thử migration trên database thật nếu chưa kiểm tra và backup.
