# Tổng hợp hiện trạng hệ thống CFC Lab

> **Bản đọc nhanh và kết quả mới nhất:** xem `docs/DOC_HIEU_HE_THONG.md` và `plans/SYSTEM_COMPLETION_ROADMAP.md`. Bên dưới là chi tiết và lịch sử; các đoạn cũ có thể mâu thuẫn. Ngày 17/11 đã thử upload DOCX, gửi duyệt, phát hành, tra cứu và tải PDF trên môi trường cô lập; chưa triển khai public hoặc nghiệm thu production. V1–V11 có trong source; V10–V11 chưa được xác nhận trên database làm việc.

> **Cập nhật 19/11/2026 — quy trình đã đổi:** Phiếu 1 (yêu cầu) → Phiếu 7/2 (tùy chọn) → Phiếu 3 → Phiếu 4 (bàn giao, KCS nhận/từ chối) → Phiếu 5 (tải file kết quả) → ADMIN phát hành. Mã mẫu **nhập tay tại Phiếu 3**, **bỏ xác minh hai người** và bỏ bước duyệt thứ hai trước phát hành (feature `sampling`, migration V11). Mục 4 bên dưới là thiết kế cũ (V7) được giữ để tham chiếu; nếu mâu thuẫn, theo ghi chú này và `docs/ai/CONTEXT.md`.

**Mốc đối chiếu:** 19/11/2026 (phần kiểm thử mục 7 còn mốc 17/11) · Phạm vi: source và tài liệu trong workspace.

Tài liệu này là bản tổng hợp hiện hành cho người dùng và phiên AI tiếp theo. Source và kết quả test thực tế vẫn là nguồn quyết định; các ghi chép cũ trong Project Memory và plan là lịch sử, có thể không còn đúng. Không xem “đã có code” là đồng nghĩa “đã nghiệm thu” hoặc “đã sẵn sàng production”.

## 1. Mục tiêu và quy trình

Hệ thống quản lý luồng: **tiếp nhận/Phiếu 3 → chuyển mẫu/Phiếu 4 → KCS nhận hoặc từ chối → thử nghiệm/Phiếu 5 → hồ sơ kết quả → tra cứu**. Đã thử tới tra cứu trong môi trường cô lập; chưa nghiệm thu hoặc triển khai công khai.

Ba role hiện có:

| Role | Phạm vi chính | Lưu ý |
|---|---|---|
| RECEPTIONIST | Tiếp nhận, quản lý Phiếu 3, lập/gửi Phiếu 4; dashboard riêng | Không được truy cập workspace nhận mẫu KCS. |
| TECHNICIAN | Nhận/từ chối Phiếu 4; tải file để lập Biên bản 5; tạo hồ sơ kết quả nháp | Không được gọi API Phiếu 3 đầy đủ. File gốc được chuyển và ghi chú vẫn có thể chứa thông tin khách. |
| ADMIN | Quản lý nhân viên, thao tác nội bộ theo quyền và kiểm tra biên bản | Có dashboard tổng hợp `/admin` (19/11), chưa nghiệm thu E2E; phát hành hồ sơ kết quả trực tiếp. |

Không tạo role SAMPLE_RECEIVER. `receivedBy` là nhân viên lập Phiếu 3; `deliveredBy` là người giao được nhập trên form. Public tương lai không trả các thông tin này hoặc thông tin liên hệ đầy đủ.

## 2. Nền tảng và chạy cục bộ

- Backend: Java 21, Spring Boot, Maven, package-by-feature tại `source/be`. Frontend nội bộ: React/Vite/JSX tại `source/fe`.
- MySQL lưu dữ liệu; Redis hỗ trợ refresh session. Cấu hình được lấy từ môi trường/.env, không ghi credentials vào tài liệu.
- Flyway quản lý migration `source/be/src/main/resources/db/migration/`; Hibernate dùng `validate`, không tự viết migration. Trong source có V1–V11 (V7–V9: mã hóa, hồ sơ kết quả, audit tra cứu, PDF KQKN; V11: Phiếu 1/7/2). Các migration mới đã được thử trên database tạm, **chưa xác nhận đã chạy trên database làm việc** (kiểm tra `flyway_schema_history`). Backup trước khi migrate DB làm việc. Kiểm tra `flyway_schema_history` trên đúng database trước khi kết luận.
- File hồ sơ/avatar hiện lưu filesystem local; cần xác định backup, retention và lưu trữ khi triển khai thực tế.
- Lệnh chạy backend/frontend và hướng dẫn theo hệ điều hành: `docs/ai/START_HERE.md`. Không thay đổi database đang có chỉ để kiểm tra tài liệu.

## 3. Tính năng nội bộ

| Hạng mục | Có trong source / đã ghi nhận | Chưa hoàn tất hoặc cần nghiệm thu |
|---|---|---|
| Auth và tài khoản | Login, cookie/session, CSRF, refresh Redis, logout, remember-me; Admin quản lý nhân viên; profile, avatar, đổi mật khẩu. | Rà lại E2E thật và vận hành bảo mật khi triển khai. |
| Phiếu 3 | Nhiều mẫu/hồ sơ nguồn, điều kiện nhận, niêm phong, chỉ tiêu, ảnh/file có phân loại và checksum; tìm kiếm/phân trang; dashboard Tiếp nhận. Người dùng đã xác nhận tạo phiếu bằng role thật trên Chrome. | Draft/completion và các quy định liên quan chưa đầy đủ. Export DOCX đã sửa lỗi XML nhưng **chưa nghiệm thu mở bằng Word thật**. |
| Phiếu 4 | Nháp, sửa, gửi, nhận/từ chối; khóa mẫu khi gửi; tìm hồ sơ có thể chuyển ở server. | Nghiệm thu lại chuỗi thật với file và các lỗi phát sinh. |
| Workspace KCS | Dashboard `/admin/receiving`, danh sách, chi tiết, gallery, nhận/từ chối; projection riêng; aggregate tại DB, tìm mã, phân trang, sort whitelist; khóa hàng khi quyết định. | Chưa nghiệm thu E2E bản cuối với hai role, file thật, 401/403/409 và realtime; không coi K1+K2 đã hoàn thành. Thiếu ảnh hiện chỉ cảnh báo; LAB cần xác nhận. |
| Phiếu 5 | Chọn mẫu KCS đã nhận và tải một file kết quả DOCX/PDF hoàn chỉnh. | Luồng tạo mới không còn bắt nhập thiết bị, hóa chất, QC/raw data; Admin kiểm tra và phát hành ở hồ sơ kết quả. |
| Thông báo nội bộ | Lưu DB, notification center, WebSocket và toast; event tiếp nhận/chuyển/nhận/từ chối. | Chưa nghiệm thu đầy đủ hai phiên WebSocket thật, sau commit/rollback và tình huống lỗi. Chính sách hiển thị tên khách trong notification cần LAB xác nhận. |
| Phiếu 1 / 7 / 2 | Feature `sampling` (V11): yêu cầu kiểm nghiệm, kế hoạch lấy mẫu, lấy mẫu hiện trường; tìm/chọn Phiếu 1 và tự điền sang Phiếu 3. | Chưa export DOCX theo mẫu; validation và số phiếu cần LAB xác nhận. |
| Dashboard Admin | API `/api/v1/admin/dashboard` (ADMIN-only) và UI `/admin` (19/11). | Chưa nghiệm thu Chrome với ADMIN/RECEPTIONIST; backend đang chạy cần restart để có endpoint. |

## 4. Mã hóa CFC-TN-14 (thiết kế V7 — ĐÃ BỊ THAY THẾ MỘT PHẦN ngày 18–19/11)

> **Hiện hành:** mã nhập tay tại Phiếu 3 (kiểm tra trùng, ghi lịch sử chỉnh mã; chỉ ADMIN sửa), không chặn bàn giao vì xác minh hai người; KCS nhận/từ chối ở Phiếu 4. Các gate `VERIFIED`, quy tắc "người mã hóa không tự xác minh" và API `/coding/verify` dưới đây là thiết kế cũ, chưa được xác nhận còn nguyên trong source — đối chiếu `SampleCodingService` trước khi dùng. Test cũ `SampleCodingServiceTest` thuộc quy trình này.

- **Đã có (V7):** sequence nguyên tử theo ngày giờ Việt Nam (`sample_code_counters`, `INSERT … ON DUPLICATE KEY UPDATE`; không dùng `COUNT+1`; không tái sử dụng kể cả khi hiệu chỉnh); trạng thái `PENDING_CODING → PENDING_VERIFICATION → VERIFIED` và `CORRECTION_REQUIRED`; người mã hóa không thể tự xác minh (403); lịch sử append-only `sample_code_events` (gửi/xác minh/không đạt/hiệu chỉnh có mã cũ–mới, người, lý do, thời điểm); **gate chuyển mẫu**: tạo/sửa/gửi Phiếu 4 và nhận phiếu đều từ chối mẫu chưa `VERIFIED` (409), kể cả qua API transfer cũ; danh sách “có thể chuyển” chỉ gồm mẫu đã xác minh. API: `/api/v1/samples/{id}/coding[/submit|verify|reject|correct|events]` (ADMIN, RECEPTIONIST). UI: ô trạng thái/hành động trên chi tiết Phiếu 3.
- **Giả định tạm, cần LAB xác nhận:** (1) mẫu có sẵn trước V7 được đánh dấu `VERIFIED` + `coding_legacy=true`; (2) vai trò được đối chiếu = ADMIN/RECEPTIONIST khác người mã hóa (Technician không đối chiếu); (3) hiệu chỉnh cấp mã mới từ counter ngày, chưa có bước “người duyệt hiệu chỉnh” riêng; (4) mã nguồn `SOURCE` chưa kiểm định dạng `2 chữ + 3 số`.
- **Chưa làm (chờ LAB):** hậu tố `-V/-R`, mẫu QC (`BL/SP/D`), mã mù và bảng đối chiếu có quyền riêng, nhãn in. Không coi mã hiện tại tuân thủ đầy đủ CFC-TN-14.

## 5. Hồ sơ kết quả và tra cứu public (P3/P4 — đã code và test, chưa deploy)

> **Hiện hành (19/11):** luồng **file-first** — TECHNICIAN/ADMIN tải file kết quả DOCX/PDF ở Phiếu 5, ADMIN phát hành trực tiếp. Đoạn PDF KQKN do hệ thống xuất (nhập bảng chỉ tiêu, hai người ký) bên dưới là thiết kế 17/11, có thể chỉ còn dùng cho tương thích; đối chiếu `ReportPdfService` và màn `/admin/results` trước khi dựa vào.

- **Phiếu KQKN-DV do hệ thống xuất PDF (V9, 17/11/2026):** theo biểu mẫu `KQKN-SR-CT001-092026.docx`, khách tra cứu rồi tải PDF này. Kỹ thuật viên nhập bảng chỉ tiêu (chỉ tiêu, đơn vị, phương pháp, kết quả, ghi chú) và tên hai người ký trong `/admin/results/{id}`; thông tin mẫu lấy tự động từ Phiếu 3/4/5 (khách hàng, địa chỉ, tên mẫu, mã vùng trồng, cơ sở đóng gói, xe/mooc/container/seal, mã mẫu, người/ngày lấy mẫu, tình trạng, lượng, ngày nhận, ngày thử nghiệm). Khi **phát hành**, hệ thống cấp số phiếu, mã tra cứu và tạo PDF song ngữ (header công ty + logo, watermark, QR, bảng kết quả, chữ ký, ghi chú, chân trang `BM.QT.14.02`; font Liberation Sans nhúng), lưu bất biến kèm SHA-256. Có “Xem trước PDF” (watermark “BẢN XEM TRƯỚC”) trước khi gửi duyệt. QR = `PUBLIC_LOOKUP_URL` + `?code=<mã>`; mặc định `http://192.168.1.132:5173/tracuu/` (LAN, Vite phục vụ landing) — đổi sang domain bằng biến môi trường, chỉ áp dụng cho phiếu phát hành **sau** khi đổi (PDF đã phát hành bất biến); đặt rỗng thì chỉ in mã dạng chữ.
- **Số phiếu:** `{mã sản phẩm}{MM}{yyyy}-{STT 3 số}` như mẫu (`SR092026-001`), cấp nguyên tử theo kỳ (`result_number_counters`). **Quy tắc này suy ra từ file mẫu, LAB cần xác nhận.** Tên người ký mặc định `Phan Trường Tiền` / `Nguyễn Công Huân` lấy từ biểu mẫu (cấu hình `REPORT_QUALITY_HEAD_NAME`, `REPORT_DIRECTOR_NAME`; sửa được từng hồ sơ) — cần LAB xác nhận người ký.
- **Quy trình file-first:** `DRAFT → PUBLISHED → REVOKED`; hồ sơ cũ `SUBMITTED` vẫn được phát hành để tương thích dữ liệu trước đây. Kỹ thuật viên hoặc Admin tải file KQKN hoàn chỉnh; Admin kiểm tra PDF rồi phát hành trực tiếp, không bắt buộc người duyệt thứ hai. Phát hành vẫn cần biên bản `REVIEWED`, mẫu `VERIFIED` mã hóa; DB bảo đảm tối đa một bản `PUBLISHED`/biên bản; khóa hàng khi chuyển trạng thái; lịch sử bất biến. File đính kèm: PDF/DOCX/XLSX ≤10 MB, ≤10 file, kiểm tra đuôi + MIME + magic bytes, PDF chủ động, macro/OLE/liên kết ngoài/zip-slip; tên lưu ngẫu nhiên. **PDF KQKN luôn công khai; file đính kèm chỉ công khai nếu Admin chọn khi phát hành.**
- **Tra cứu public (API):** `POST /api/v1/public/results/lookup` và `/download` — mã 16 ký tự (~80 bit) + 4 số cuối SĐT cho cả nhập tay và QR; chỉ `PUBLISHED`; sai mã/SĐT/nháp/thu hồi cùng một 404 trung tính; kiểm tra lại ở mỗi lần tải; `no-store`, `noindex`; mã và SĐT chỉ trong body POST. Lookup trả số phiếu, mã mẫu, tên mẫu, ngày, **bảng kết quả** và danh sách file công khai — **không** trả tên/địa chỉ/SĐT khách trong JSON (PDF thì có tên và địa chỉ khách như biểu mẫu LAB, chỉ tải được sau khi xác thực mã + 4 số cuối). Rate limit trong bộ nhớ (5 sai/mã/15 phút, 15 sai/client/15 phút, 60 yêu cầu/client/10 phút; khóa cả mã không tồn tại); audit `public_lookup_audit` không chứa SĐT, mã hoặc IP thô. **Sau proxy/Cloudflare phải đặt `PUBLIC_CLIENT_IP_HEADER`** (`X-Forwarded-For` sau Vite proxy/nginx — lấy giá trị **cuối** do proxy thêm; `CF-Connecting-IP` sau Cloudflare), nếu không mọi khách dùng chung một IP. Rate limit theo từng instance.
- `source/public/tracuu/` đã nối form với API (hiển thị bảng kết quả + nút “Tải PDF”, tự cuộn tới kết quả); Vite dev/preview phục vụ tại `/tracuu/` cùng origin với app và `vite build` chép sang `dist/tracuu`. Đã kiểm thử 17/11/2026 trên stack cô lập: giải mã QR từ PDF → mở link → mã tự điền và bị xóa khỏi URL → nhập 4 số cuối → xem kết quả → tải PDF (Chrome 390×844). Vẫn `noindex`, chưa domain/TLS/canonical/sitemap; **chưa quét QR in giấy bằng điện thoại thật**. Chưa có quét virus, backup, retention. ZBS/SMS chưa triển khai (P6).

## 6. Route và ranh giới

- Nội bộ: `/login`, `/admin/intake` (dashboard Receptionist), `/admin/intake/receipts`, `/admin/transfers`, `/admin/receiving`, `/admin/receiving/transfers`, `/admin/testing`, `/admin/notifications`, `/admin/profile`. Kiểm tra `source/fe/src/app/App.jsx` cho quyền và route chi tiết.
- API nội bộ dùng `/api/v1/**`; workspace KCS dùng `/api/v1/receiving/**`. API transfer cũ vẫn tồn tại, Technician nhận projection giới hạn; file gốc có thể chứa PII. Tra quyền chi tiết ở `docs/RBAC.md` và contract ở `docs/API_CONVENTIONS.md`.
- `/tracuu/` là trang tĩnh được Vite phục vụ, đã nối API tra cứu trong môi trường thử; chưa được triển khai công khai.

## 7. Bằng chứng kiểm thử đã ghi nhận

**Mới nhất 19/11/2026:** backend full suite trên MySQL/Redis cô lập `Tests run: 85, Failures: 0, Errors: 0, Skipped: 3`, `BUILD SUCCESS`; frontend `npm run build` pass; Chrome TECHNICIAN thật ở 4 kích thước không tràn ngang. Chưa có Chrome E2E ADMIN/RECEPTIONIST cho dashboard mới. (Một lượt trung gian cùng ngày còn 2 failure/1 error ở `SampleCodingServiceTest`, sau đó đã xử lý.)

Lượt 17/11/2026 (P0–P4, lịch sử), chạy trong Docker cô lập (MySQL 8.4 + Redis tạm; **không** đụng database làm việc):

- Backend: `scripts/run-be-tests-docker.sh` → **74 test, 0 failures, 0 errors, 0 skipped** (trong đó có đọc lại nội dung PDF KQKN) (gồm `ReceivingConcurrencyTest` và `SampleSequenceConcurrencyTest` bật bằng `KCS_ISOLATED_DB=true`). Baseline trước thay đổi: 38 test pass khi có MySQL/Redis (29 lỗi nếu không có DB).
- Flyway: V1–V8 chạy trên DB trống; V7/V8 chạy trên DB V6 có dữ liệu cũ (sequence được seed từ dữ liệu cũ, mẫu cũ `VERIFIED/legacy`).
- E2E HTTP thật trên backend cô lập (`scripts/e2e-results-flow.py`): **77/77** kiểm tra (tiếp nhận → mã hóa → chuyển mẫu → KCS → Phiếu 5 → nhập nội dung phiếu → xem trước/phát hành PDF → tra cứu public → tải PDF; danh sách sort/tìm/lọc ngày ở backend; 401/403/409/413/415/429). PDF sinh ra đã được xem dạng ảnh và so với file mẫu.
- Chrome thật (headless, CDP) tại 390×844, 1024×768, 1440×900, 2560×952: **58/58** (tra cứu/hồ sơ kết quả/mã hóa) + **101/101** (danh sách sort/tìm/lọc ngày gửi lên backend, ô chụp ảnh + xem trước ảnh, chuỗi bàn giao) + **38/38** (soạn nội dung phiếu KQKN) (trang tra cứu public từ link QR, validation, lỗi trung tính, kết quả + nút tải, không PII; danh sách/chi tiết hồ sơ kết quả; ô mã hóa trên Phiếu 3; không tràn ngang; không console error).
- Frontend `npm run build` pass.
- **Chưa kiểm chứng:** mở DOCX Phiếu 3 bằng Microsoft Word thật; tải/mở file PDF trong trình duyệt thật; hai phiên WebSocket thật; dashboard Admin; QR in thực tế; hạ tầng/TLS/domain; chạy migration trên database làm việc; thao tác UI mã hóa/phát hành bằng click thật (mới kiểm tra hiển thị).

## 8. Ưu tiên tiếp theo và quyết định còn mở

1. Nghiệm thu thủ công bằng người dùng thật: Word cho Phiếu 3, UI mã hóa/phát hành, hai phiên WebSocket.
2. LAB chốt quy tắc còn mở (mục 4–5) rồi làm `-V/-R`, QC/mã mù, số phiếu kết quả, quy tắc người duyệt.
3. Quyết định lưu trữ/backup/quét virus file; sinh QR; domain/TLS/proxy (`app.public.client-ip-header`), rồi mới deploy trang tra cứu.
4. Nghiệm thu E2E dashboard Admin (P5 đã code); P6 ZBS/SMS chưa làm, chờ đầu vào từ LAB.
5. Triển khai ZBS/SMS khi có tài khoản provider, template được duyệt và quyết định về consent.

Cần LAB xác nhận: quy định thiếu ảnh, nội dung/tên khách trong notification, người kiểm tra/ký, quyền xem trạng thái kết quả của Receptionist, chứng nhận được công bố, chính sách lưu trữ và nội dung thông báo kết quả.

## 9. Bàn giao và nguồn tham khảo

- `docs/ai/START_HERE.md`, `docs/ai/CONTEXT.md`, `docs/ai/STATE.md`, `docs/ai/HANDOFF.md`: Project Memory.
- `plans/COMPLETION_PLAN.md`, `plans/NOTIFICATION_REALTIME.md`, `plans/intake/`: kế hoạch và tiêu chí kiểm tra; phần cũ có thể khác trạng thái hiện tại.
- `docs/RBAC.md`, `docs/API_CONVENTIONS.md`, `docs/DATABASE.md`, `docs/PUBLIC_LOOKUP_SEO.md`, `docs/INTAKE_NOTIFICATIONS.md`: tài liệu chuyên biệt, đối chiếu source trước khi thực hiện.
- Tại thời điểm lập tài liệu còn nhiều thay đổi chưa commit, gồm code `results`, migration V6, `source/public/`, route/frontend và tài liệu. Không commit, ghi đè hoặc xóa thay đổi đó khi chưa được yêu cầu.

## Danh sách: sort/tìm/lọc ở backend — 17/11/2026

- Mọi danh sách chính do backend sắp xếp, tìm, lọc ngày (giờ Việt Nam) và phân trang (`ListQuery`, whitelist sort, 400 nếu sai): chuyển mẫu (`/transfers`), biên bản thử nghiệm (`/testing-records`), hồ sơ kết quả (`/result-releases`, thêm `/ready` liệt kê biên bản đã kiểm tra chờ lập hồ sơ), thông báo (thêm tìm kiếm, loại, khoảng ngày, sort), nhân viên (`/users`, thêm `GET /users/{id}`), nhận mẫu KCS (thêm ô sort). Technician không tìm/sắp xếp theo tên khách. Form lập biên bản dùng `GET /testing-records/candidates` thay vì tải 50 phiếu chuyển.
- Giao diện: component dùng chung `ListToolbar`/`Pagination`/`PhotoPicker`; ô chọn ảnh có nút **Chụp ảnh** (mở camera trên điện thoại) và **Chọn từ máy**, ảnh hiện thumbnail ngay trong khung phiếu, xóa từng ảnh được; chi tiết phiếu hiện thumbnail ảnh đã tải. Chuỗi bàn giao phóng to chữ/mốc. Vite dev proxy không còn tràn lỗi `EPIPE` khi đóng tab.

## Đồng bộ trạng thái và hồ sơ kết quả — 17/11/2026

- **Một kiểu hiển thị trạng thái:** `shared/status.js` (nhãn + màu theo miền: tiếp nhận, chuyển mẫu, biên bản, hồ sơ kết quả, mã hóa, tài khoản) và `StatusBadge` thay cho các lớp rời (`status-pill`, `transfer-status`, `testing-status`, `result-status`, `coding-badge`). Chrome 4 viewport xác nhận mọi badge cùng kích thước/hình dạng trên các trang.
- **Hồ sơ kết quả nối với Phiếu 5:** TECHNICIAN hoặc ADMIN chọn mẫu KCS đã nhận, tải một file DOCX/PDF kết quả; DOCX được chuyển PDF để kiểm tra. Không nhập lại các bảng thiết bị, hóa chất, QC/raw data.
- **Phát hành file-first:** ADMIN kiểm tra và phát hành trực tiếp; không có bước duyệt độc lập thứ hai. Backend kiểm tra luồng tạo hồ sơ từ file bằng TECHNICIAN và ADMIN.
