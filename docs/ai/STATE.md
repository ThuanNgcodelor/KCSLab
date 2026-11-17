# Project State

> **17/11/2026:** `docs/SYSTEM_STATUS.md` tổng hợp trạng thái mới nhất; các mục dưới đây lưu lịch sử theo phiên, có thể mâu thuẫn khi chức năng được triển khai về sau. Không có test mới trong lần tổng hợp tài liệu này.

## Workspace nhận mẫu KCS — 16/11/2026

- Đã thêm API `/api/v1/receiving/**` cho dashboard, list, chi tiết, metadata/content file và quyết định; role `ADMIN|TECHNICIAN`, không mở receipt API. DTO mới không có `customerName`; file gốc có thể chứa dữ liệu khách.
- Dashboard lọc theo `Asia/Ho_Chi_Minh`, tối đa 366 ngày, đếm tại DB; list phân trang tối đa 50, search theo mã, sort whitelist. Khóa pessimistic tại `TransferService.decide` bảo vệ cả API cũ và mới.
- `V5__receiving_query_indexes.sql` thêm index `(status, submitted_at)` và `(received_at)`; EXPLAIN trên database tạm quy mô nhỏ đã dùng hai index, chưa load test quy mô thực.
- Frontend thêm dashboard/list/detail/gallery KCS và navbar theo role, giữ dashboard Receptionist và chuyển link transfer cũ của Technician theo role. Chưa chạy Chrome E2E thật với dữ liệu/ảnh và backend mới.
- Kiểm tra: 29 test Java pass trên MySQL 8.4 + Redis tạm (bao gồm 5 test security, timezone, projection, traversal giả lập và test hai giao dịch thực); `npm run build` pass. Chrome kiểm tra bốn kích thước dashboard/list/detail bằng API mô phỏng, không tràn ngang ngoài bảng cuộn.
- Chưa hoàn tất nghiệm thu: kiểm tra luồng thật Receptionist → Phiếu 4 → KCS xem file → nhận/từ chối và notification, file thực tế/401/403/409 trên UI; test tải, xác minh attachment chéo trên MySQL và biểu mẫu mobile. API transfer cũ giữ quyền đọc cho Technician nhưng trả `customerName=null`; Admin/Receptionist vẫn nhận giá trị cũ.
- Dashboard nhóm dữ liệu từng ngày bằng query ở database thay vì chạy ba query cho mỗi ngày; khoảng chọn tối đa 366 ngày. Chưa load test production. Chưa chạy V5 trên database người dùng; Flyway sẽ chạy V5 khi backend khởi động với database phù hợp.

## Xác nhận ROLE Tiếp nhận và plan ROLE Người nhận mẫu — 16/11/2026

- Người dùng xác nhận vertical slice `RECEPTIONIST` hiện tại đã ổn: tài khoản role thật đăng nhập và tạo thành công Phiếu 3 trên Chrome hiển thị; backend sinh mã phiếu và mã mẫu.
- Phạm vi xác nhận không bao gồm draft/completion, gate mã hóa hai người, gửi khách ZBS/SMS hoặc export Word nghiệm thu cuối.
- Receptionist chưa có dashboard riêng: `/admin/intake` đang gộp dải aggregate với danh sách và navbar đã ẩn mục Tổng quan.
- Người nhận mẫu KCS hiện dùng runtime role `TECHNICIAN`, xem/nhận/từ chối Phiếu 4 nhưng chưa có dashboard ngày/tuần/tháng và chưa có quyền xem ảnh theo projection transfer.
- Người dùng xác nhận `TECHNICIAN` vừa nhận mẫu vừa lập Phiếu 5 cùng `ADMIN`; giữ ba role hiện tại. Cần thêm upload nhiều file PDF/tài liệu vào Phiếu 5; chưa thấy runtime attachment riêng cho Phiếu 5.
- Ba dashboard cần riêng biệt: Receptionist theo tiếp nhận, Technician theo nhận mẫu/thử nghiệm, Admin tổng hợp toàn hệ thống. Chưa đặt SLA; thiếu ảnh tạm đề xuất cảnh báo.
- KCS được xem toàn bộ file gốc của hồ sơ được chuyển giao; file gốc có thể chứa thông tin khách hàng, dù API projection hạn chế các trường liên hệ riêng.
- `GET /api/v1/transfers` hiện lấy top 50, chưa phân trang/filter DB; `TransferResponse` còn trả `customerName`; quyết định đồng thời chưa có row/optimistic lock.
- Plan chi tiết được cập nhật tại `plans/intake/RECEPTIONIST.md`, `plans/intake/TECHNICIAN_KCS.md` và `plans/intake/OVERVIEW.md`.

## Cập nhật cấu hình migration MySQL — 16/11/2026

- Đã thêm Spring Boot Flyway starter và module MySQL vào backend để Flyway tự chạy khi startup.
- `source/be/src/main/resources/db/migration/V1__baseline_schema.sql` là bản baseline sao từ schema hiện tại.
- Database `cfc_lab` đã có dữ liệu sẽ được Flyway baseline tự động ở version `1`, sau đó chạy `V2`–`V4` để bù các thay đổi Phase 1/3, intake workspace và hardening Phiếu 3; database rỗng sẽ chạy toàn bộ V1–V4.
- Hibernate đã chuyển sang `ddl-auto=validate`; rename cột, backfill, index, foreign key và stored procedure phải thêm thành migration SQL có version.
- Các lần thay đổi schema sau này chỉ cần thêm migration tăng dần từ `V5__...sql` rồi bấm Run lại. Flyway lưu lịch sử tại `flyway_schema_history`.

## Cấu hình local/test MySQL — 16/11/2026

- Backend mặc định dùng profile `mysql`; H2 đã được loại khỏi dependency và cấu hình runtime.
- `application-mysql.yml` dùng URL, username và password từ `.env`/environment variables.
- Flyway chạy migration khi backend khởi động; Hibernate dùng `spring.jpa.hibernate.ddl-auto=validate` để kiểm tra entity/schema.
- H2 đã được loại khỏi dependency và cấu hình runtime; kết nối dùng URL, username và password MySQL từ `.env`/environment variables.

## Cập nhật phiên làm việc — sửa template export Phiếu 3 — 16/11/2026

- Đã sửa `source/be/src/main/resources/templates/phieu-tiep-nhan-ban-giao-mau.docx`: thay các thuộc tính namespace XML không hợp lệ `d5p1/d8p1:space` bằng `xml:space` chuẩn.
- Đã sửa `scripts/prepare-form3-template.ps1` để các lần tạo template sau dùng đúng namespace `xml:space`, không tái tạo lỗi Word.
- `ReceiptForm3Exporter` đã escape giá trị động trước khi chèn vào XML, tránh dữ liệu như `&` hoặc `<` làm hỏng file DOCX.
- Đã thêm `ReceiptForm3ExporterTest` để kiểm tra `word/document.xml` hợp lệ và dữ liệu đặc biệt được export đúng.
- Xác minh được: ZIP DOCX hợp lệ, toàn bộ XML trong package hợp lệ với `xmllint`, không còn prefix namespace sai, `git diff --check` pass.
- Chưa chạy được Maven/JDK hoặc mở Word thật trong môi trường hiện tại; vì vậy export Phiếu 3 vẫn chờ nghiệm thu cuối bằng Microsoft Word tại máy công ty.

## Cập nhật phiên làm việc mới — 15/11/2026

- Bộ hướng dẫn đọc nhanh đã được gom tại `docs/ai/START_HERE.md`.
- `.env` đã có `SPRING_PROFILES_ACTIVE=mysql`; khi chạy đúng profile, backend dùng MySQL `cfc_lab` với Flyway migration và Hibernate `validate`, không dùng H2.
- Đã có thiết kế export Phiếu 3 bằng template placeholder và endpoint `/api/v1/receipts/{id}/form-3`. Tuy nhiên file DOCX export/template vừa bị Microsoft Word báo lỗi mở; chưa đánh dấu nghiệm thu. Cần kiểm tra lại ZIP/XML và mở bằng Word thật.
- Quyền hiển thị `receivedBy`: Admin/Receptionist xem đầy đủ; Technician/KCS không có receipt API đầy đủ nhưng notification nội bộ có thể hiển thị tên người ghi nhận. Public lookup tương lai không được trả trường này.
- Kiến trúc SEO/public lookup được ghi tại `docs/PUBLIC_LOOKUP_SEO.md`; hiện chưa có runtime public route, result release, PDF, QR hoặc Cloudflare Tunnel.

## Cập nhật mới nhất — duy trì đăng nhập bằng Redis

## Cập nhật mới nhất — hardening Phiếu 3

- Phiếu tiếp nhận đã bổ sung `requestNumber` và `sealStatus` với ba giá trị `INTACT`, `NOT_PRESENT`, `OPENED`; dữ liệu legacy vẫn đọc được qua `sealIntact`.
- Backend bắt buộc lý do khi `ACCEPTED_WITH_CONDITION`/`REJECTED`, bắt buộc chỉ tiêu cho từng mẫu và từ chối trạng thái niêm phong thiếu.
- Sự kiện `INTAKE_RECORDED` gửi cho Admin và Technician/KCS, dùng chung notification center + WebSocket toàn hệ thống.
- Migration thật đã áp dụng trên MySQL local: thêm `receipts.request_number`, `receipts.seal_status`, backfill phiếu cũ; không dùng NVARCHAR.
- Bộ nghiệm thu chi tiết nằm tại `plans/intake/TEST_CASES_FORM3.md`.

- Trang đăng nhập có tùy chọn **Ghi nhớ đăng nhập**; hệ thống không lưu mật khẩu. Chọn ghi nhớ tạo cookie HttpOnly tối đa 30 ngày, không chọn dùng cookie phiên trình duyệt.
- Access token mặc định 15 phút. Frontend tự gọi `/api/v1/auth/refresh` khi API trả 401, gom các request đồng thời vào một lần refresh rồi thử lại request ban đầu.
- Refresh token ngẫu nhiên 48 byte, Redis chỉ lưu SHA-256 hash với TTL; token được xoay sau mỗi lần refresh và token cũ không thể dùng lại. Đổi mật khẩu thu hồi mọi phiên của người dùng.
- Redis Laragon 5.x được hỗ trợ bằng Lua `GET + DEL` nguyên tử thay cho lệnh `GETDEL` chỉ có từ Redis 6.2.
- Cookie resolver bỏ qua access token cũ tại `csrf/login/refresh`, nên access token hết hạn không chặn chính endpoint làm mới phiên.
- Trang thông tin cá nhân đã khóa overflow theo content area; dưới 1280 px chuyển layout hồ sơ thành một cột và các ô mật khẩu giữ cùng chiều cao.
- Xác minh: backend 16/16 test pass; frontend production build 71 modules; Redis runtime smoke đạt remembered/session cookie, 401 → refresh → 200 và chống tái sử dụng token; Chrome 1424/1024/390 px không tràn ngang.

## Cập nhật mới nhất — bộ chọn Phiếu 4 và responsive

- Màn lập Phiếu chuyển mẫu tìm phiếu tiếp nhận phía server theo mã phiếu, mã mẫu, khách hàng hoặc số điện thoại; debounce 300 ms, 20 kết quả/trang và có điều hướng trang.
- API `GET /api/v1/receipts` hỗ trợ `transferable=true`, chỉ trả phiếu không bị từ chối và còn ít nhất một mẫu `RECEIVED`; không còn tải 100 phiếu rồi lọc ở trình duyệt.
- MySQL có composite index `idx_samples_status_receipt(status, receipt_id)` phục vụ truy vấn ứng viên chuyển mẫu; migration idempotent đã được áp dụng trên CSDL local.
- Navbar của `RECEPTIONIST` không còn mục Tổng quan. Trang `/admin/transfers` chuyển bảng thành card có nhãn ở màn hình nhỏ và không tràn ngang.
- Đã xác minh bằng test `ReceiptServiceTest`, frontend production build và Google Chrome CDP ở breakpoint mobile; API MySQL trả đúng 3/3 ứng viên hiện có, tối đa 20/trang.

## Cập nhật mới nhất — thông tin cá nhân

- Cả `ADMIN`, `RECEPTIONIST`, `TECHNICIAN` có trang `/admin/profile`; tự đổi tên hiển thị, avatar và mật khẩu, còn email/role/trạng thái là chỉ đọc.
- Avatar JPG/PNG được frontend co về 640×640 để giảm lưu lượng upload; backend kiểm tra tối đa 5 MB/40 MP, cắt vuông và chỉ lưu JPEG 320×320 chất lượng 82%. Ảnh gốc không được lưu và file mới ghi đè file cũ của cùng user.
- Avatar dùng URL versioned và cache private 30 ngày, hiển thị tại sidebar và danh sách nhân viên. Đổi mật khẩu thu hồi toàn bộ refresh session.
- MySQL đã có cột nullable `users.avatar_updated_at` bằng migration idempotent; file hiện lưu local tại `storage/avatars`.
- Xác minh: backend 12/12 test pass, frontend build 71 modules, Chrome profile desktop 1424 px/mobile 390 px không overflow; ảnh smoke thực tế trả đúng 320×320 rồi được xóa.

**Cập nhật:** 15/11/2026

## Đã hoàn thành

- Nền tảng Spring Boot 4.1.2/Java 21, React 19/Vite 6 và MySQL profile.
- Login email, JWT cookie, refresh session, CSRF, logout, `/auth/me`; không có registration.
- Quản lý nhân viên Admin-only: danh sách, tạo, sửa role/mật khẩu, khóa/mở khóa.
- Admin Portal responsive theo tone sáng CFC; navbar/sidebar cố định theo viewport, mobile drawer và footer website CFC.
- Phase 1/Phiếu 3: hồ sơ nguồn có cấu trúc, nhiều mẫu trên một phiếu, actor tiếp nhận, ảnh/file phân loại có checksum và tải/xem lại trên desktop/mobile.
- Phase 3/Phiếu 4: vòng đời `DRAFT → PENDING → ACCEPTED|REJECTED`, sửa nháp, chỉ khóa mẫu khi gửi và ẩn bản nháp khỏi KCS/PTN.
- Danh sách Phiếu 3 phân trang phía server: mặc định 20/trang, tối đa 100, 9 lựa chọn sort trên UI và whitelist sort ở backend.
- Đã chốt kế hoạch Phase 1B thông báo khách hàng: `INTAKE_RECORDED`, ZBS Template Message ưu tiên, SMS Brandname/MultiSMS dự phòng và outbox/idempotency/webhook; phần gửi khách chưa có runtime.
- Workspace Tiếp nhận đã có dashboard/query server-side, notification center lưu bền, trang thông báo và WebSocket nội bộ; form Phiếu 3 vẫn còn draft/completion/catalog duyệt.
- Vertical slice biên bản thử nghiệm 5: thiết bị, hóa chất, QC, raw data, submit, trả lại và kiểm tra hoàn tất.
- Schema MySQL hiện có 12 bảng, bổ sung `sample_transfers`, `sample_transfer_items` và 4 bảng `testing_*`.

## Đã xác minh

- Frontend production build: pass (63 modules).
- Backend Maven test: 9/9 pass trên Java 21.
- MySQL smoke Phase 1/3: tạo phiếu hai mẫu, upload Form 3, sửa/gửi Phiếu 4 và nhận mẫu: pass.
- Google Chrome CDP: 7 route desktop/mobile, phân trang/sort hiển thị đúng, không tràn ngang tại 390px, không có console error.
- CodeGraph được index và ưu tiên cho khám phá source.

## Khoảng trống

- Chưa có phát hành/version PDF, public lookup và audit log tổng quát.
- Chưa có bộ integration test nghiệp vụ đầy đủ; migration tool Flyway đã được bật cho MySQL.
- Chưa có role reviewer chuyên biệt; bước review biên bản hiện do `ADMIN` thực hiện.
- Chưa có gate mã hóa độc lập (counter nguyên tử, `-V/-R`, người thứ hai xác minh), QR và correction history. WebSocket notification nội bộ đã có; WebSocket không dùng cho Zalo/SMS.
- Chưa có consent/template được duyệt, outbox/delivery, worker, provider adapter hoặc callback ZBS/SMS; chi tiết tại `docs/INTAKE_NOTIFICATIONS.md`.
- Avatar hiện lưu filesystem local; chưa có object storage/CDN, backup và xử lý nhiều instance production.

## Nguồn đọc mặc định

Đọc `docs/ai/CONTEXT.md` để lấy context ngắn; dùng file này khi cần snapshot trạng thái chi tiết hơn.

## Kiểm tra KCS bổ sung — 16/11/2026

- Java 21 + MySQL 8.4 + Redis tạm: `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`; frontend `npm run build`: `✓ built in 944ms`.
- Có test MockMvc + MySQL cho content cùng receipt và chặn attachment của receipt khác; có test 21 phiếu, phân trang và receivedAt nửa đêm Việt Nam. Concurrency test được bật trên DB tạm.
- Chưa kiểm tra Chrome E2E bản cuối cho ba role/4 breakpoint, file thật/keyboard/401/403/409; chưa có test WebSocket sau commit và rollback. Không coi K1+K2 hoàn thành.
- Migration V5 chỉ được áp dụng lên DB tạm trống trong kiểm thử; chưa xác nhận trên DB làm việc. Thiếu ảnh vẫn cảnh báo/cho nhận; chính sách tên khách trong notification chờ LAB xác nhận.

## Giao diện KCS — 16/11/2026

- Điều chỉnh dashboard, danh sách và chi tiết receiving: sửa hàng chờ bị bó hẹp, tăng độ tương phản, làm rõ thông tin và trình bày mẫu dạng thẻ trên điện thoại.
- `npm run build` pass; Playwright với API và role TECHNICIAN giả lập kiểm tra ba màn hình ở 390×844, 1024×768, 1440×900, 2560×952: không thấy tràn ngang toàn trang. Đã xem ảnh dashboard desktop và chi tiết mobile.
- Chưa phải E2E với đăng nhập, dữ liệu, file và WebSocket thật; cần nghiệm thu lại các luồng này trên hệ thống chạy thật.

## Hộp thoại xác nhận CFC — 16/11/2026

- Tạo `shared/components/ConfirmDialog.jsx` để tái sử dụng; xác nhận nhận/từ chối KCS không còn dùng `window.confirm`. Có Escape, focus nút hủy và màu riêng cho thao tác từ chối.
- Frontend build pass; Playwright dùng API giả lập đã kiểm tra hủy/Escape không gửi quyết định, xác nhận gửi đúng loại quyết định và hiển thị tại bốn breakpoint chuẩn. Chưa kiểm thử nhận/từ chối trên dữ liệu thật.

## Đồng bộ dashboard KCS — 16/11/2026

- Dashboard nhận mẫu dùng cùng cách chào, lọc kỳ, làm mới và thẻ chỉ số có mô tả như dashboard Tiếp nhận; giữ nguyên KPI và hàng đợi từ API receiving.
- `npm run build` pass; Playwright với API giả lập đã kiểm tra 390×844, 1024×768, 1440×900, 2560×952 không tràn ngang toàn trang, thao tác tuần/tùy chọn/làm mới. Chưa nghiệm thu dữ liệu thật sau thay đổi này.

## Toast và đồng bộ thông báo — 16/11/2026

- Có toast CFC khi nhận event WebSocket mới, đóng/xem chi tiết, tự đóng sau 8 giây; socket kết nối lại sẽ tải REST, chống event trùng gần đây. Quyền người nhận không đổi, không có âm thanh.
- Frontend build pass; Playwright giả lập WebSocket/REST kiểm tra toast, lặp, đóng, tự đóng, điều hướng, reconnect và bốn breakpoint. Chưa test hai phiên WebSocket thật hoặc lỗi đánh dấu đã đọc; plan tại `plans/NOTIFICATION_REALTIME.md`.

## Rà soát login và frontend — 16/11/2026

- Login dùng logo CFC cùng sidebar, favicon được đổi sang cùng asset sau khi frontend khởi tạo; có nút hiện/ẩn mật khẩu. Giữ nguyên xác thực và `ConfirmDialog`; rà source không có `window.confirm/alert/prompt`.
- `npm run build` pass; kiểm tra logo, toggle, lỗi đăng nhập và bốn breakpoint bằng Playwright/API giả lập. Chưa kiểm tra login thành công và favicon trên bản build thật.
- Các hạng mục chuẩn hóa toast, xác nhận và validation toàn frontend được đưa vào `plans/NOTIFICATION_REALTIME.md`, chưa hoàn thành toàn bộ.

## Khởi đầu kế hoạch hoàn thiện — 16/11/2026

- Đã tách route React bằng `lazy`/`Suspense`. `npm run build` pass, main JS 276.11 kB (87.57 kB gzip), vẫn chưa đạt mục tiêu dưới 200 kB chưa nén; Playwright dùng API giả lập kiểm tra dashboard ba role và bốn kích thước, không tràn ngang toàn trang.
- V6 tạo nền `result_releases` cho nháp phiên bản 1. TECHNICIAN tạo nháp cho biên bản `REVIEWED`; ADMIN/TECHNICIAN đọc nháp. Không có upload, gửi duyệt, phát hành hay public API. Test Java 21/MySQL/Redis tạm: 38 test, 0 failures/errors/skipped; V6 chưa kiểm tra trên database làm việc.
- Quyết định người dùng: chặn phát hành tới khi gate CFC-TN-14 hoàn tất; QR và mã nhập tay đều cần 4 số cuối SĐT; KQKN-DV là hồ sơ được nói đến; file giới hạn 10 MB/file, 10 file/hồ sơ; mặc định chỉ PDF public, Admin có thể bật Word/Excel khi phát hành.
- Chưa làm gate mã hóa, upload/an toàn OOXML, duyệt, version/thu hồi, site/API public, rate limit, SEO, refactor CSS/font, UI releases và E2E dữ liệu thật. Không xem mục 1–5.4 là hoàn thành.

## Trang tra cứu — bản xem trước nội bộ

- Cập nhật giao diện landing với logo CFC và hình phòng kiểm nghiệm minh họa tạo bằng AI; giữ form vô hiệu hóa, thông báo chưa khả dụng và `noindex`. Playwright kiểm tra HTTP 200, ảnh tải được và không tràn ngang ở 1440px và 390px; chưa kiểm thử chức năng tra cứu thật.
- Có landing tĩnh tách biệt tại `source/public/tracuu/`, không yêu cầu đăng nhập. Chạy thử theo `source/public/README.md`; form bị vô hiệu hóa và thông báo rõ chưa thể tra cứu/tải kết quả.
- Playwright đã kiểm tra localhost HTTP 200, không redirect login, không tràn ngang toàn trang tại 390×844, 1024×768, 1440×900, 2560×952. Bản xem trước dùng `noindex`, chưa có domain/canonical/sitemap.
- Chưa có lookup API, hồ sơ phát hành, QR, rate limit, no-store/signed download, SEO production hoặc triển khai Internet. Không xem đây là chức năng tra cứu hoàn chỉnh.
