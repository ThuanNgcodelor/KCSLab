# Ngữ cảnh CFC Lab

Cập nhật 23/11/2026. Đọc file này để bắt đầu. Khi cần chi tiết, chọn tài liệu theo tác vụ ở [START_HERE.md](START_HERE.md).

## Hệ thống đang chạy

- Backend modular monolith Java 21, Spring Boot 4, MySQL/Flyway và Redis. Frontend React/Vite. API và WebSocket chạy cùng origin: lúc dev qua Vite proxy, bản build qua nginx (Linux) hoặc Apache Laragon (Windows).
- Ba quyền `ADMIN`, `RECEPTIONIST` và `TECHNICIAN`. Xem [RBAC](../RBAC.md) và [bản đồ module](MODULE_MAP.md).
- Luồng chính: yêu cầu kiểm nghiệm → (tùy chọn) kế hoạch lấy mẫu và lấy mẫu hiện trường → tiếp nhận (Phiếu 3) → chuyển mẫu (Phiếu 4) → KCS nhận mẫu → thử nghiệm và phiếu KQKN → Admin duyệt và phát hành → khách tra cứu.
- **Số/No.** (dạng `CFC26.0001`) là định danh duy nhất của yêu cầu và phiếu tiếp nhận. Mã hóa mẫu do người tiếp nhận nhập tay.

## Thử nghiệm và phiếu kết quả (KQKN)

- `/admin/testing/new` có 2 chế độ:
  1. **Nhập kết quả trên hệ thống** (mặc định): chọn mẫu đã được KCS nhận; 10 mục thông tin mẫu tự điền từ Phiếu 3/4. Kỹ thuật viên chỉ nhập Đơn vị, Kết quả, Ghi chú cho từng chỉ tiêu (chỉ tiêu và phương pháp lấy từ phiếu chuyển). `POST /result-releases/from-entry` tạo biên bản, hồ sơ và các dòng kết quả trong một giao dịch, có thể gửi duyệt ngay.
  2. **Tải file KQKN có sẵn** (DOCX/PDF): luồng cũ, vẫn giữ.
- **Phiếu KQKN tạo từ mẫu Word** `templates/kqkn-dv.docx`, làm từ mẫu của LAB `docs/template/KQKN-SR-CT001-092026.docx`.
  - **PDF = DOCX chuyển bằng LibreOffice.** Bộ vẽ PDF bằng code (OpenPDF `ReportPdfService`) đã bỏ.
  - QR được đóng vào ô "Dán/in mã QR". Từ 23/11/2026 không in dòng "Mã tra cứu: …" dưới QR; mã vẫn nằm trong URL của QR và dùng cho tra cứu như trước. Nếu không tạo được QR, vẫn giữ phương án mã chữ dự phòng. Image backend có font Microsoft (Times New Roman).
  - PDF đã lưu không tự đổi: cần chạy backend bản mới rồi Admin tạo lại PDF của phiếu do hệ thống tạo. Chưa triển khai lại backend hoặc tạo lại PDF trên dữ liệu thật trong lần sửa này.
- **Số phiếu KQKN = Số/No. của phiếu tiếp nhận.** Bản phát hành lại thêm `/R1`, `/R2`…; mẫu thứ hai cùng Số/No. thêm `-2`. Phiếu không có Số/No. dùng sequence cũ.
- **Ảnh mẫu** của phiếu tiếp nhận (các loại `*_PHOTO`/`OTHER`, tối đa 12 ảnh) được in ở trang "HÌNH ẢNH MẪU" của DOCX/PDF và hiện trong hồ sơ kết quả. Không lấy scan Phiếu 3 và ảnh bàn giao.
  - Ảnh được chọn theo toàn bộ phiếu tiếp nhận, chưa lọc riêng từng mẫu. Trang tra cứu không có gallery/timeline toàn quy trình; API public chỉ trả thông tin phiếu, dòng kết quả và file được công khai. Luồng tải KQKN có sẵn giữ nội dung file, không tự bổ sung ảnh tiếp nhận.
- Admin duyệt: phát hành (cấp số, mã tra cứu, QR) hoặc trả lại kèm lý do. Người duyệt phải khác người lập và người phân tích.
- Admin có nút **Tạo lại PDF** cho từng phiếu và **tạo lại hàng loạt** ở `/admin/results`: giữ nguyên số và mã tra cứu, dùng khi đổi mẫu hoặc đổi địa chỉ QR.

## Tra cứu khách và QR

- Trang `/tracuu/` (tĩnh, `source/public/tracuu/`) dùng tông sáng, cùng logo và màu với trang đăng nhập. Trang được index (SEO: JSON-LD, FAQ, `robots.txt`); API tra cứu vẫn `noindex`/`no-store`.
- QR = `PUBLIC_LOOKUP_URL` + `?code=…`. Hiện đặt là `http://192.168.1.132:5173/tracuu/` (IP nội bộ, khách bên ngoài không mở được). Khi có tên miền: đổi biến này rồi tạo lại PDF hàng loạt. Xem mục 8 của [RUN_WINDOWS_SERVER.md](../../RUN_WINDOWS_SERVER.md).

## Chạy và triển khai

- **Linux (máy dev):** `./scripts/run-all.sh` chỉ kiểm tra MySQL/Redis (không bật tắt gì), rồi chạy `docker-compose.yml`:
  - backend `.jar` + LibreOffice ở cổng 8080;
  - frontend nginx ở cổng 5173.

  Lệnh `stop` chỉ tắt 2 container này. Xem [run.md](../../run.md).
- **Windows server:** không dùng Docker. Laragon (MySQL, Redis, Apache phục vụ `dist`), Java 21 chạy `.jar`, LibreOffice cài trên Windows. Script ở `scripts/windows/`. Xem [RUN_WINDOWS_SERVER.md](../../RUN_WINDOWS_SERVER.md).
- Test backend: `./scripts/run-be-tests-docker.sh` chạy trong image có LibreOffice. Lần chạy 20/11/2026: 107 đạt, 1 bỏ qua.
  - Kiểm tra phạm vi sửa QR ngày 23/11/2026: `ReportFileStamperTest`, `KqknDocxExporterTest`, `ResultReleaseWorkflowTest`, `PublicResultFlowTest`: 34 đạt, 1 bỏ qua (test tùy chọn PDF thật). Đã giải mã ảnh QR từ PDF thử nghiệm để xác nhận đúng URL và kiểm tra không in mã dưới QR. Không chạy lại toàn bộ suite hoặc nghiệm thu tài khoản thật trong lần này.

## Điểm còn mở

- Chưa thử trên Windows thật: các script PowerShell, cấu hình Apache, `soffice.com`.
- Chưa có tên miền. Cần dựng Cloudflare Tunnel, cập nhật canonical/sitemap, rồi tạo lại PDF.
- **Cần LAB xác nhận:**
  - quy ước số phiếu `/R1` và `-2`;
  - có công khai ảnh mẫu trong PDF cho khách không;
  - có bỏ hẳn form biên bản chi tiết cũ (thiết bị, hóa chất, QC) không;
  - quy tắc mật khẩu 10 hay 12 ký tự.
- Cần nghiệm thu: iPhone/Android, mở file Word trên Microsoft Word, sao lưu database và thư mục `storage`, HTTPS.
- Zalo/SMS khách vẫn là [đề xuất riêng](../INTAKE_NOTIFICATIONS.md).
