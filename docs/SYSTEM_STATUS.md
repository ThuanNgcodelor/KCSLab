# Tổng hợp hiện trạng hệ thống CFC Lab

**Mốc đối chiếu:** 17/11/2026 · Phạm vi: source và tài liệu trong workspace, gồm cả thay đổi chưa commit.

Tài liệu này là bản tổng hợp hiện hành cho người dùng và phiên AI tiếp theo. Source và kết quả test thực tế vẫn là nguồn quyết định; các ghi chép cũ trong Project Memory và plan là lịch sử, có thể không còn đúng. Không xem “đã có code” là đồng nghĩa “đã nghiệm thu” hoặc “đã sẵn sàng production”.

## 1. Mục tiêu và quy trình

Hệ thống nội bộ quản lý luồng: **tiếp nhận/Phiếu 3 → chuyển mẫu/Phiếu 4 → KCS nhận hoặc từ chối → thử nghiệm/Phiếu 5 → hồ sơ kết quả**. Các phần phát hành và tra cứu công khai mới ở giai đoạn nền/xem trước; chưa có chuỗi hoàn chỉnh đến khách.

Ba role hiện có:

| Role | Phạm vi chính | Lưu ý |
|---|---|---|
| RECEPTIONIST | Tiếp nhận, quản lý Phiếu 3, lập/gửi Phiếu 4; dashboard riêng | Không được truy cập workspace nhận mẫu KCS. |
| TECHNICIAN | Nhận/từ chối Phiếu 4; lập biên bản thử nghiệm; tạo nháp hồ sơ kết quả | Không được gọi API Phiếu 3 đầy đủ. File gốc được chuyển và ghi chú vẫn có thể chứa thông tin khách. |
| ADMIN | Quản lý nhân viên, thao tác nội bộ theo quyền và kiểm tra biên bản | Có trang admin, nhưng dashboard tổng hợp toàn hệ thống chưa được coi là hoàn chỉnh/nghiệm thu. |

Không tạo role SAMPLE_RECEIVER. `receivedBy` là nhân viên lập Phiếu 3; `deliveredBy` là người giao được nhập trên form. Public tương lai không trả các thông tin này hoặc thông tin liên hệ đầy đủ.

## 2. Nền tảng và chạy cục bộ

- Backend: Java 21, Spring Boot, Maven, package-by-feature tại `source/be`. Frontend nội bộ: React/Vite/JSX tại `source/fe`.
- MySQL lưu dữ liệu; Redis hỗ trợ refresh session. Cấu hình được lấy từ môi trường/.env, không ghi credentials vào tài liệu.
- Flyway quản lý migration `source/be/src/main/resources/db/migration/`; Hibernate dùng `validate`, không tự viết migration. Trong source có V1–V8; V7/V8 (mã hóa, quy trình hồ sơ kết quả, audit tra cứu) đã được thử trên database tạm trống và trên DB có dữ liệu cũ, **chưa xác nhận đã chạy trên database làm việc**. Backup trước khi migrate DB làm việc. Kiểm tra `flyway_schema_history` trên đúng database trước khi kết luận.
- File hồ sơ/avatar hiện lưu filesystem local; cần xác định backup, retention và lưu trữ khi triển khai thực tế.
- Lệnh chạy backend/frontend và hướng dẫn theo hệ điều hành: `docs/ai/START_HERE.md`. Không thay đổi database đang có chỉ để kiểm tra tài liệu.

## 3. Tính năng nội bộ

| Hạng mục | Có trong source / đã ghi nhận | Chưa hoàn tất hoặc cần nghiệm thu |
|---|---|---|
| Auth và tài khoản | Login, cookie/session, CSRF, refresh Redis, logout, remember-me; Admin quản lý nhân viên; profile, avatar, đổi mật khẩu. | Rà lại E2E thật và vận hành bảo mật khi triển khai. |
| Phiếu 3 | Nhiều mẫu/hồ sơ nguồn, điều kiện nhận, niêm phong, chỉ tiêu, ảnh/file có phân loại và checksum; tìm kiếm/phân trang; dashboard Tiếp nhận. Người dùng đã xác nhận tạo phiếu bằng role thật trên Chrome. | Draft/completion và các quy định liên quan chưa đầy đủ. Export DOCX đã sửa lỗi XML nhưng **chưa nghiệm thu mở bằng Word thật**. |
| Phiếu 4 | Nháp, sửa, gửi, nhận/từ chối; khóa mẫu khi gửi; tìm hồ sơ có thể chuyển ở server. | Nghiệm thu lại chuỗi thật với file và các lỗi phát sinh. |
| Workspace KCS | Dashboard `/admin/receiving`, danh sách, chi tiết, gallery, nhận/từ chối; projection riêng; aggregate tại DB, tìm mã, phân trang, sort whitelist; khóa hàng khi quyết định. | Chưa nghiệm thu E2E bản cuối với hai role, file thật, 401/403/409 và realtime; không coi K1+K2 đã hoàn thành. Thiếu ảnh hiện chỉ cảnh báo; LAB cần xác nhận. |
| Phiếu 5 | Biên bản thử nghiệm: thiết bị, hóa chất, QC/raw data, gửi, trả lại và review nội bộ. | Chưa hoàn tất upload nhiều file cho Phiếu 5; cần kiểm tra tách biệt người phân tích/người kiểm tra. |
| Thông báo nội bộ | Lưu DB, notification center, WebSocket và toast; event tiếp nhận/chuyển/nhận/từ chối. | Chưa nghiệm thu đầy đủ hai phiên WebSocket thật, sau commit/rollback và tình huống lỗi. Chính sách hiển thị tên khách trong notification cần LAB xác nhận. |
| Dashboard Admin | Có route/trang Admin hiện hữu. | Chưa coi dashboard bao quát toàn hệ thống là hoàn thành. |

## 4. Mã hóa CFC-TN-14 (P2 — đã code và test, chờ LAB chốt một phần)

- **Đã có (V7):** sequence nguyên tử theo ngày giờ Việt Nam (`sample_code_counters`, `INSERT … ON DUPLICATE KEY UPDATE`; không dùng `COUNT+1`; không tái sử dụng kể cả khi hiệu chỉnh); trạng thái `PENDING_CODING → PENDING_VERIFICATION → VERIFIED` và `CORRECTION_REQUIRED`; người mã hóa không thể tự xác minh (403); lịch sử append-only `sample_code_events` (gửi/xác minh/không đạt/hiệu chỉnh có mã cũ–mới, người, lý do, thời điểm); **gate chuyển mẫu**: tạo/sửa/gửi Phiếu 4 và nhận phiếu đều từ chối mẫu chưa `VERIFIED` (409), kể cả qua API transfer cũ; danh sách “có thể chuyển” chỉ gồm mẫu đã xác minh. API: `/api/v1/samples/{id}/coding[/submit|verify|reject|correct|events]` (ADMIN, RECEPTIONIST). UI: ô trạng thái/hành động trên chi tiết Phiếu 3.
- **Giả định tạm, cần LAB xác nhận:** (1) mẫu có sẵn trước V7 được đánh dấu `VERIFIED` + `coding_legacy=true`; (2) vai trò được đối chiếu = ADMIN/RECEPTIONIST khác người mã hóa (Technician không đối chiếu); (3) hiệu chỉnh cấp mã mới từ counter ngày, chưa có bước “người duyệt hiệu chỉnh” riêng; (4) mã nguồn `SOURCE` chưa kiểm định dạng `2 chữ + 3 số`.
- **Chưa làm (chờ LAB):** hậu tố `-V/-R`, mẫu QC (`BL/SP/D`), mã mù và bảng đối chiếu có quyền riêng, nhãn in. Không coi mã hiện tại tuân thủ đầy đủ CFC-TN-14.

## 5. Hồ sơ kết quả và tra cứu public (P3/P4 — đã code và test, chưa deploy)

- **Hồ sơ kết quả (V8):** `DRAFT/RETURNED → SUBMITTED → PUBLISHED → REVOKED`, phiên bản `v2…` sau khi thu hồi. Technician lập hồ sơ tải/gỡ file và gửi duyệt (chỉ người lập); ADMIN độc lập (khác người lập và người phân tích) trả lại/phát hành/thu hồi. Phát hành yêu cầu biên bản `REVIEWED`, mẫu đã `VERIFIED` mã hóa, ≥1 file công khai; DB đảm bảo tối đa một phiên bản `PUBLISHED` cho mỗi biên bản; khóa hàng khi chuyển trạng thái; lịch sử bất biến `result_release_events`. File: PDF/DOCX/XLSX, ≤10 MB, ≤10 file; kiểm tra đuôi + MIME khai báo + magic bytes, PDF chủ động (`/JavaScript`, `/Launch`, `/EmbeddedFile`), OOXML (macro, nhúng OLE/ActiveX, liên kết ngoài, mẫu đính kèm, zip-slip); tên lưu ngẫu nhiên, SHA-256, lưu hoặc xóa theo transaction; tải nội bộ qua API kiểm quyền (`nosniff`, `no-store`). Mặc định chỉ PDF công khai; Admin bật Word/Excel khi phát hành. API `/api/v1/result-releases/**`; UI `/admin/results`.
- **Giả định/cần xác nhận:** số phiếu kết quả hiện là `KQ-yyyyMMdd-XXXXXX` ngẫu nhiên (quy tắc LAB chưa chốt); quy tắc người duyệt độc lập; Receptionist không xem hồ sơ kết quả; chưa có quét virus, backup, retention; file kết quả lưu `storage/results` local.
- **Tra cứu public (API):** `POST /api/v1/public/results/lookup` và `/download` — mã tra cứu 16 ký tự (~80 bit) + 4 số cuối SĐT cho cả nhập tay và QR; chỉ `PUBLISHED`; sai mã/sai SĐT/nháp/thu hồi cùng một phản hồi 404 trung tính; kiểm tra lại quyền ở mỗi lần tải; chỉ file công khai; không trả PII; `Cache-Control: no-store`, `X-Robots-Tag: noindex`; mã và SĐT chỉ ở body POST; rate limit trong bộ nhớ (5 lần sai/mã/15 phút, 15 sai/client/15 phút, 60 yêu cầu/client/10 phút; khóa cả mã không tồn tại để không thành “oracle”); audit `public_lookup_audit` không chứa SĐT, mã hoặc IP thô. **Khi chạy sau proxy/Cloudflare phải đặt `app.public.client-ip-header` (ví dụ `CF-Connecting-IP`), nếu không mọi khách dùng chung một IP và bị khóa chung.** Rate limit theo từng instance; scale ngang cần chuyển sang Redis.
- `source/public/tracuu/` đã nối form với API (`lookup.js`); vẫn `noindex`, **chưa deploy**, chưa có domain/TLS/canonical/sitemap/QR in thực tế, chưa kiểm tra tường lửa/headers hạ tầng. ZBS/SMS/outbox/provider chưa triển khai (P6).

## 6. Route và ranh giới

- Nội bộ: `/login`, `/admin/intake` (dashboard Receptionist), `/admin/intake/receipts`, `/admin/transfers`, `/admin/receiving`, `/admin/receiving/transfers`, `/admin/testing`, `/admin/notifications`, `/admin/profile`. Kiểm tra `source/fe/src/app/App.jsx` cho quyền và route chi tiết.
- API nội bộ dùng `/api/v1/**`; workspace KCS dùng `/api/v1/receiving/**`. API transfer cũ vẫn tồn tại, Technician nhận projection giới hạn; file gốc có thể chứa PII. Tra quyền chi tiết ở `docs/RBAC.md` và contract ở `docs/API_CONVENTIONS.md`.
- `/tracuu/` là **site preview riêng**, không phải route React hoặc endpoint public đã phát hành.

## 7. Bằng chứng kiểm thử đã ghi nhận

Lượt 17/11/2026 (P0–P4), chạy trong Docker cô lập (MySQL 8.4 + Redis tạm; **không** đụng database làm việc):

- Backend: `scripts/run-be-tests-docker.sh` → **66 test, 0 failures, 0 errors, 0 skipped** (gồm `ReceivingConcurrencyTest` và `SampleSequenceConcurrencyTest` bật bằng `KCS_ISOLATED_DB=true`). Baseline trước thay đổi: 38 test pass khi có MySQL/Redis (29 lỗi nếu không có DB).
- Flyway: V1–V8 chạy trên DB trống; V7/V8 chạy trên DB V6 có dữ liệu cũ (sequence được seed từ dữ liệu cũ, mẫu cũ `VERIFIED/legacy`).
- E2E HTTP thật trên backend cô lập (`scripts/e2e-results-flow.py`): **52/52** kiểm tra (tiếp nhận → mã hóa → chuyển mẫu → KCS → Phiếu 5 → hồ sơ kết quả → tra cứu public; 401/403/409/413/415/429).
- Chrome thật (headless, CDP) tại 390×844, 1024×768, 1440×900, 2560×952: **58/58** (trang tra cứu public từ link QR, validation, lỗi trung tính, kết quả + nút tải, không PII; danh sách/chi tiết hồ sơ kết quả; ô mã hóa trên Phiếu 3; không tràn ngang; không console error).
- Frontend `npm run build` pass.
- **Chưa kiểm chứng:** mở DOCX Phiếu 3 bằng Microsoft Word thật; tải/mở file PDF trong trình duyệt thật; hai phiên WebSocket thật; dashboard Admin; QR in thực tế; hạ tầng/TLS/domain; chạy migration trên database làm việc; thao tác UI mã hóa/phát hành bằng click thật (mới kiểm tra hiển thị).

## 8. Ưu tiên tiếp theo và quyết định còn mở

1. Nghiệm thu thủ công bằng người dùng thật: Word cho Phiếu 3, UI mã hóa/phát hành, hai phiên WebSocket.
2. LAB chốt quy tắc còn mở (mục 4–5) rồi làm `-V/-R`, QC/mã mù, số phiếu kết quả, quy tắc người duyệt.
3. Quyết định lưu trữ/backup/quét virus file; sinh QR; domain/TLS/proxy (`app.public.client-ip-header`), rồi mới deploy trang tra cứu.
4. P5 dashboard Admin và rà frontend; P6 ZBS/SMS.
5. Triển khai ZBS/SMS khi có tài khoản provider, template được duyệt và quyết định về consent.

Cần LAB xác nhận: quy định thiếu ảnh, nội dung/tên khách trong notification, người kiểm tra/ký, quyền xem trạng thái kết quả của Receptionist, chứng nhận được công bố, chính sách lưu trữ và nội dung thông báo kết quả.

## 9. Bàn giao và nguồn tham khảo

- `docs/ai/START_HERE.md`, `docs/ai/CONTEXT.md`, `docs/ai/STATE.md`, `docs/ai/HANDOFF.md`: Project Memory.
- `plans/COMPLETION_PLAN.md`, `plans/NOTIFICATION_REALTIME.md`, `plans/intake/`: kế hoạch và tiêu chí kiểm tra; phần cũ có thể khác trạng thái hiện tại.
- `docs/RBAC.md`, `docs/API_CONVENTIONS.md`, `docs/DATABASE.md`, `docs/PUBLIC_LOOKUP_SEO.md`, `docs/INTAKE_NOTIFICATIONS.md`: tài liệu chuyên biệt, đối chiếu source trước khi thực hiện.
- Tại thời điểm lập tài liệu còn nhiều thay đổi chưa commit, gồm code `results`, migration V6, `source/public/`, route/frontend và tài liệu. Không commit, ghi đè hoặc xóa thay đổi đó khi chưa được yêu cầu.
