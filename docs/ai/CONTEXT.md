# Ngữ cảnh CFC Lab

Cập nhật 25/11/2026. Đọc file này để bắt đầu. Khi cần chi tiết, chọn tài liệu theo tác vụ ở [START_HERE.md](START_HERE.md).

## Hệ thống đang chạy

- **Khóa thông báo theo khâu, 25/11/2026 (V23, đã triển khai):** `StaffNotificationPolicy` áp dụng cùng một danh sách loại cho tạo tin, list/search/count, markRead/markAll và WebSocket (kiểm role/enabled hiện tại trước push). ADMIN nhận tất cả sự kiện, kể cả thao tác của chính Admin; RECEPTIONIST nhận yêu cầu mới và phản hồi nhận/từ chối của KCS; TECHNICIAN chỉ nhận `TRANSFER_SUBMITTED`. Tạo tiếp nhận chỉ báo ADMIN; không còn báo KCS trước khi chuyển mẫu. Phản hồi KCS gửi đúng người chuyển + toàn bộ Admin; không gửi cho Tiếp nhận khác.
  - V23 bổ sung bản sự kiện lịch sử còn thiếu cho Admin đang hoạt động, giữ trạng thái đã đọc của bản cũ; chuẩn hóa tin chuyển mẫu cũ có người chuyển + Số/No., không có tên khách. Tin sai khâu cũ không bị xóa DB nhưng bị chặn khỏi danh sách/count/markRead. Không có backfill yêu cầu vốn chưa từng tạo sự kiện; không phát toast hàng loạt khi chạy migration.
  - Kiểm tra: toàn suite 148 đạt + 1 skip; sau thêm ca từ chối, 7 test routing/realtime/luồng đạt; frontend build và Playwright role filters ở 390/1024/1440/2560 px đạt. Đã build/restart backend/frontend theo yêu cầu, DB vận hành V23; app 200/auth không đăng nhập 401, không restart lỗi, thiếu bản Admin=0 và tin chuyển cũ thiếu số CFC=0. Zalo vẫn OFF. Backup DB + storage trước triển khai: `/home/kali/backups/cfc-lab-before-routing-20261125-c9c055c5/`.
  - Quản lý hồ sơ 10 năm, export và đổi ổ upload **đã commit `8a286d1` và đang chạy** (V24, V25); bản chỉnh giao diện đồng bộ + tách folder hồ sơ trùng số đã kiểm thử cô lập, chưa commit/triển khai. Xem mục 0 của [DOSSIER_ARCHIVE_EXPORT_PLAN.md](../../plans/DOSSIER_ARCHIVE_EXPORT_PLAN.md). Mọi nơi đọc/ghi ảnh tiếp nhận và file kết quả phải qua `DossierStorageService` (`read`/`write`), không tự ghép đường dẫn. Export chạy trên luồng riêng để không chặn scheduler của Zalo. Hai thư mục receipts/results cũ giữ nguyên.

- **Triển khai nội bộ 25/11/2026:** đã build/recreate đúng hai container `cfc-lab-prod-backend`/`cfc-lab-prod-frontend` từ working tree hiện tại (base `290bab8` + sửa khối lượng/thông báo). Flyway nâng DB vận hành từ V21 lên V22, thêm `samples.weight` nullable. Bản frontend đang phục vụ có ô weight riêng ở tạo tiếp nhận/chi tiết/chuyển mẫu; backend khởi động thành công, hai container không restart lỗi. HTTP app/tra cứu 200; API auth không đăng nhập 401 như thiết kế. Không đổi cấu hình gửi Zalo: vẫn OFF/live gate false.
  - Backup trước triển khai: `/home/kali/backups/cfc-lab-before-weight-20261125-1a805ac4/` (database SQL, storage tar đã kiểm tra đọc được, JAR backend cũ và frontend cũ). Không đọc/ghi `.env` hoặc log dữ liệu khách. Không sửa MySQL/Redis service, không tự đổi khối lượng/đơn vị của hồ sơ cũ.
  - Kiểm tra nghiệp vụ trước triển khai: 146 test backend đạt, 1 skip; 4 test mapping frontend đạt; production build và Playwright mock đã đạt ở các breakpoint. Sau triển khai chỉ kiểm tra dịch vụ/schema/bundle, chưa thao tác bằng tài khoản thật chị Thúy/chị Thảo; không tạo phiếu/tin giả trong DB vận hành. Các ghi chú “chưa deploy” bên dưới là trạng thái lịch sử trước lần triển khai này.

- Backend modular monolith Java 21, Spring Boot 4, MySQL/Flyway và Redis. Frontend React/Vite. API và WebSocket chạy cùng origin: lúc dev qua Vite proxy, bản build qua nginx (Linux) hoặc Apache Laragon (Windows).
- Ba quyền `ADMIN`, `RECEPTIONIST` và `TECHNICIAN`. Xem [RBAC](../RBAC.md) và [bản đồ module](MODULE_MAP.md).
- Luồng chính: yêu cầu kiểm nghiệm → (tùy chọn) kế hoạch lấy mẫu và lấy mẫu hiện trường → tiếp nhận (Phiếu 3) → chuyển mẫu (Phiếu 4) → KCS nhận mẫu → thử nghiệm và phiếu KQKN → Admin duyệt và phát hành → khách tra cứu.
- **Số/No.** (dạng `CFC26.0001`) là định danh duy nhất của yêu cầu và phiếu tiếp nhận. Mã hóa mẫu do người tiếp nhận nhập tay.
- **Thông báo/khối lượng (24/11):** tạo Phiếu yêu cầu phát `TEST_REQUEST_CREATED` cho ADMIN/RECEPTIONIST đang bật, trừ người lập; cập nhật không tạo lại tin. KCS nhận tin tiếp nhận không có tên công ty; phản hồi lịch sử và tìm kiếm cũng bỏ phần công ty của tin cũ. Tin chuyển mẫu ghi người chuyển + số mẫu + Số/No., mở `/admin/receiving/transfers/{id}`. Thông báo tiếp nhận KCS mở dashboard nhận mẫu, không mở route Tiếp nhận bị cấm cho TECHNICIAN.
  - V22 thêm `samples.weight` tùy chọn, tách với `quantity`. Form tiếp nhận nhập khối lượng kèm đơn vị; API trả `samples[].weight`; Phiếu chuyển kế thừa weight riêng, không chép quantity vào weight. KCS nhận đúng snapshot; Phiếu 3/KQKN có cả lượng và khối lượng khi đã nhập. Dữ liệu quantity cũ giữ nguyên, weight cũ để null, không đoán đơn vị hay sửa phiếu đã nhận/phát hành.
  - Số lượng hiện trường kế thừa theo tên mẫu, không chép tổng lượng vào mọi mẫu. Giữ nguyên đơn vị/chữ đã nhập và cho nhân viên sửa trước lưu. Kiểm tra: toàn suite backend 146 đạt, 1 bỏ qua (MySQL/Redis cô lập, V22); 4 test Node mapping đạt; frontend build đạt; Playwright API giả kiểm tra lưu amount riêng → chuyển mẫu → KCS, 4 màn ở 390/1024/1440/2560 px không tràn ngang. Chưa restart/migrate DB vận hành; cần bản backend/frontend mới và V22 khi triển khai.
- **Mã vùng trồng**: người dùng chốt ngày 23/11/2026 dùng tên này xuyên suốt Phiếu yêu cầu → Kế hoạch lấy mẫu → Lấy mẫu hiện trường → Tiếp nhận → KQKN, thay các nhãn “Nguồn gốc”, “Vùng trồng / nguồn mẫu”. Model frontend dùng `growingArea`; API/DB Phiếu yêu cầu giữ khóa `origin` cũ và KQKN giữ `orchardCode` để tương thích. Tiếp nhận điền sẵn ưu tiên hiện trường → kế hoạch → yêu cầu; giá trị rỗng đã lưu ở bước trước được giữ nguyên. `sourceCode` (Mã nguồn tạo mã mẫu) vẫn là thông tin riêng. Không tự cập nhật dây chuyền hồ sơ/PDF đã lưu. Kiểm tra: frontend build đạt; ánh xạ API cũ/giá trị rỗng và kế thừa đạt; Playwright API giả trên 5 màn ở 390/1024/1440/2560 px không tràn ngang. Chưa deploy lại bản vận hành hoặc sửa dữ liệu thật.

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
- **Windows server:** không dùng Docker. Laragon (MySQL, Redis, Apache phục vụ `dist`), Java 21 chạy `.jar`, LibreOffice cài trên Windows. Có launcher thử nghiệm `scripts/windows/Run-CFC-Lab.cmd` và stop `scripts/windows/Stop-CFC-Lab.cmd`; launcher build/chạy Vite preview trên `dist` + backend, tự yêu cầu quyền Administrator qua UAC nếu cần bật Cloudflared service và báo rõ nếu Tunnel chưa chạy. Stop chỉ dừng tiến trình được ghi nhận của launcher rồi yêu cầu quyền Administrator qua UAC để dừng Tunnel; nếu UAC bị hủy hoặc service lỗi, stop trả mã lỗi và báo tunnel có thể còn chạy. Production vẫn dùng Apache phục vụ `dist`. Xem [RUN_WINDOWS_SERVER.md](../../RUN_WINDOWS_SERVER.md).
- Test backend: `./scripts/run-be-tests-docker.sh` chạy trong image có LibreOffice. Lần chạy 20/11/2026: 107 đạt, 1 bỏ qua.
  - Sửa quyền build ngày 23/11/2026: output `build/` và `target/` cũ do Docker tạo với owner root khiến Java IDE không ghi được `.class`. Đã trả quyền về user máy dev; script test có EXIT cleanup trả quyền `build/` về UID/GID người chạy (không theo symlink). Compile main/test bằng Maven Wrapper với user `kali` đạt; chạy script với 2 test resolver đạt và output sau Docker đều ghi được. Nếu IDE còn giữ lỗi cũ, chạy `Java: Clean Java Language Server Workspace` để nạp lại.
  - Kiểm tra phạm vi sửa QR ngày 23/11/2026: `ReportFileStamperTest`, `KqknDocxExporterTest`, `ResultReleaseWorkflowTest`, `PublicResultFlowTest`: 34 đạt, 1 bỏ qua (test tùy chọn PDF thật). Đã giải mã ảnh QR từ PDF thử nghiệm để xác nhận đúng URL và kiểm tra không in mã dưới QR. Không chạy lại toàn bộ suite hoặc nghiệm thu tài khoản thật trong lần này.

## Điểm còn mở

- Chưa thử trên Windows thật: các script PowerShell, cấu hình Apache, `soffice.com`.
- Domain `cfc.io.vn` đã được thêm vào Cloudflare Free; Cloudflare Tunnel Windows có một replica Healthy và route đang được cấu hình tới `http://localhost:5173`. Chưa xác nhận truy cập HTTPS từ mạng ngoài, chưa chuyển Vite dev sang Apache production, chưa cập nhật canonical/sitemap hoặc QR PDF theo hostname mới.
- **Cần LAB xác nhận:**
  - quy ước số phiếu `/R1` và `-2`;
  - có công khai ảnh mẫu trong PDF cho khách không;
  - có bỏ hẳn form biên bản chi tiết cũ (thiết bị, hóa chất, QC) không;
  - quy tắc mật khẩu 10 hay 12 ký tự.
- Cần nghiệm thu: iPhone/Android, mở file Word trên Microsoft Word, sao lưu database và thư mục `storage`, HTTPS.
- Zalo tiếp nhận: P1–P4 đã có mã nguồn theo [plan](../../plans/ZALO_INTAKE_TEST_PLAN.md), migration V20–V21. Mặc định `OFF`; `FAKE` mô phỏng, `ZALO_DEVELOPMENT` ép development + allowlist/cap. `ZALO_LIVE` có adapter/gate riêng, token quản lý READY + refresh + polling, pilot allowlist và cap; chưa bật gửi thật. Mỗi phiếu một sự kiện, consent/payload snapshot cùng transaction tiếp nhận; mạng ngoài transaction, UNKNOWN không tự POST lại.
  - P3: webhook xác minh raw body/App/OA/header/timestamp, inbox chống trùng trước 2xx; callback sớm/muộn không hạ trạng thái. Polling có giới hạn với msg_id + số snapshot. DELIVERED chỉ xác nhận thiết bị nhận, không phải đã đọc.
  - P4: token AES-GCM (key ngoài DB), lease refresh theo App/OA/cặp mới lưu nguyên tử; mất kết quả refresh yêu cầu cấp quyền lại. Hàng đợi chờ khi refresh đang chạy, không biến thành lỗi vĩnh viễn. Admin `/admin/customer-notifications` có tổng hợp, lọc, cảnh báo, pause/resume/audit; cấp cặp token qua API Admin, không trả secret. [Runbook Linux/Windows](../ZALO_OPERATIONS_RUNBOOK.md), [hướng dẫn test](../ZALO_INTAKE_TESTING.md).
  - Sửa phát hiện khi kiểm tra P4: resource-server mặc định miễn CSRF cả JWT lấy từ cookie; matcher cuối và SPA handler hiện bắt CSRF cho cookie ghi dữ liệu, giữ ngoại lệ explicit Bearer/public/webhook. Test dùng cookie JWT thực với/không header XSRF.
  - Kiểm tra 23/11: toàn suite backend 142 đạt, 1 bỏ qua; sau sửa cuối liên quan token rotation/worker, 37 test Zalo đạt với JVM `Asia/Ho_Chi_Minh`. Frontend build đạt. Playwright API giả: form/lịch sử/retry P1–P2 và Admin filter/pause/resume P4, không tràn ngang ở 390/1024/1440/2560 px.
  - P0 vẫn chưa đủ OA/template/token/SĐT quản trị từ tài khoản: chưa gửi Zalo thật, chưa mua/kích hoạt dịch vụ, chưa dựng tunnel, chưa restart/migrate DB vận hành hoặc thử Windows thật. Chính sách lưu dữ liệu mới là đề xuất, LAB chưa chốt; không có tự xóa. Pilot LIVE, RESULT_PUBLISHED và SMS còn mở. Không coi test mock là nghiệm thu provider thật.
