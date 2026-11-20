# CFC Lab — AI Context

**Cập nhật:** 19/11/2026 · **Nguồn sự thật ngắn cho phiên AI mới**

> Đọc `docs/ai/START_HERE.md` và `docs/SYSTEM_STATUS.md` trước. Thứ tự file: mục "Hiện trạng" và "Việc tiếp theo" là hiện hành; các mục có ngày bên dưới là nhật ký bổ sung, mục nào mâu thuẫn thì ưu tiên mục **19/11/2026**. Kết quả kiểm thử luôn ghi kèm ngày; đối chiếu source khi làm việc.

## Runtime DOCX KQKN — 17/11/2026

- Khi chạy trực tiếp trên host, `OfficeConverter` cần executable `soffice` trong PATH.
- Cách chạy local được hỗ trợ khi host chưa có LibreOffice: `./scripts/run-be-docker.sh`. Script build `source/be/Dockerfile`, trong image đã có LibreOffice Writer headless và font CJK/Latin cần thiết, rồi chạy backend Java 21 với `.env` đã được parse đúng.
- Đã kiểm tra backend container: `soffice --version` trả `LibreOffice 24.2.7.2`; file KQKN mẫu chuyển thành PDF thành công; backend MySQL/Flyway chạy cổng 8080.

## Cập nhật workspace KCS — 16/11/2026

- Có workspace `/admin/receiving` cho Technician: dashboard, danh sách Phiếu 4, chi tiết và gallery file, quyết định nhận/từ chối. API projection riêng `/api/v1/receiving/**` dành cho Admin/Technician; receipt API không mở thêm quyền.
- Migration `V5__receiving_query_indexes.sql` thêm hai index transfer; quyết định bàn giao dùng khóa hàng, không thêm cột version.
- Backend 29 test pass trên MySQL/Redis Docker tạm với `KCS_ISOLATED_DB=true` (gồm test hai giao dịch và security); frontend build pass. Chrome đã kiểm tra không tràn ngang trên bốn kích thước với API mô phỏng; chưa chạy E2E thật với hai role và file upload.
- API transfer cũ giữ contract cho Admin/Receptionist và trả `customerName=null` với Technician; receiving DTO mới không có trường này. File gốc/ghi chú vẫn có thể chứa PII. Dashboard đã nhóm xu hướng theo ngày tại database; chưa load test quy mô production.

## Thứ tự bàn giao và xác minh mã — 17/11/2026 (ĐÃ BỊ THAY THẾ)

- Quy trình 17/11 (gửi mã `PENDING_VERIFICATION`, KCS xác minh sau bàn giao, chỉ mẫu `VERIFIED` vào Phiếu 5) **đã được thay bằng quyết định 18–19/11**: mã nhập tay tại Phiếu 3, bỏ xác minh hai người, KCS nhận/từ chối ở Phiếu 4. Xem mục "P0–P4" bên dưới. Giữ lại đây chỉ để tra lịch sử.

## Hiện trạng

- Ứng dụng đang chạy theo modular monolith: Spring Boot 4.1.2/Java 21 ở `source/be`, React 19/Vite 6/JSX ở `source/fe`, MySQL 8.4 cho profile `mysql`.
- Đã hoàn thành vertical slice: auth email/session/CSRF, user, Phase 1/Phiếu 3 có hồ sơ nguồn + nhiều mẫu + bằng chứng ảnh/file, Phase 3/Phiếu 4 có nháp/gửi/nhận và biên bản 5 đến kiểm tra/trả lại. CFC-TN-14 là quy trình chuẩn nhưng theo quyết định 18/11 mã mẫu nhập tay ở Phiếu 3 và bỏ xác minh hai người trên hệ thống; runtime chưa có `-V/-R`, QC/mã mù (cần LAB xác nhận việc cập nhật CFC-TN-14).
- Hồ sơ kết quả (`result_releases`, đã commit): TECHNICIAN/ADMIN tải file DOCX/PDF, ADMIN phát hành trực tiếp (file-first); API tra cứu public có trong source nhưng chưa deploy/nghiệm thu production; chưa có ZBS/SMS. Export Phiếu 3 đã sửa XML nhưng vẫn cần test mở Word thật.
- Chưa có gửi tin khách hàng. Phase 1B đã chốt plan dùng ZBS Template Message ưu tiên, SMS Brandname/MultiSMS dự phòng qua database outbox; WebSocket được tách riêng cho nhân viên nội bộ.
- Có dashboard Receptionist riêng ở `/admin/intake`, danh sách ở `/admin/intake/receipts`; KCS có dashboard, danh sách và projection xem file ở `/admin/receiving`. Dashboard Admin toàn hệ thống chưa nghiệm thu hoàn chỉnh. Notification center lưu bền, trang riêng và WebSocket nội bộ.
- Không có đăng ký công khai. Admin được seed từ biến môi trường; không ghi credentials vào tài liệu.
- Frontend mở từng route theo role: `ADMIN` quản lý user/review; `ADMIN|RECEPTIONIST` tiếp nhận và lập/sửa/gửi Phiếu 4; `ADMIN|TECHNICIAN` thử nghiệm và quyết định nhận mẫu. KCS/PTN không nhìn thấy Phiếu 4 còn `DRAFT`; Technician không có quyền gọi đầy đủ API Phiếu 3, nhưng notification có thể chứa tên người ghi nhận.
- Người dùng xác nhận `TECHNICIAN` vừa nhận mẫu vừa lập Phiếu 5 cùng `ADMIN`; không tạo role mới. Cần dashboard riêng cho từng role, Admin xem tổng hợp toàn hệ thống, và bổ sung upload nhiều file vào Phiếu 5.
- UI dùng tone sáng CFC, logo từ `guideline-assets/cfc-008.png`; admin shell fluid tối đa 1680px, sidebar cố định ở desktop và chuyển drawer từ 1080px trở xuống, typography tăng trên màn 2K; footer liên kết website CFC.

## Luồng đã chạy

1. Admin đăng nhập tại `/login`.
2. Quản lý nhân viên tại `/admin/users`, tạo ở `/admin/users/new`, sửa ở `/admin/users/:id/edit`.
3. Dashboard Receptionist tại `/admin/intake`, danh sách tại `/admin/intake/receipts`, tạo tại `/admin/intake/new`, xem/bổ sung ảnh tại `/admin/intake/:id`.
   Danh sách dùng server-side pagination mặc định 20 phiếu/trang và 9 lựa chọn sort; API chỉ nhận sort trong whitelist.
4. Backend lưu customer → receipt → nhiều sample → attachment; hồ sơ nguồn có người/ngày lấy, vùng trồng, cơ sở đóng gói, xe/moóc/container và sinh mã dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`.
5. File dev lưu cục bộ ở `source/be/storage/receipts`; loại hồ sơ, actor, thời điểm chụp, metadata và SHA-256 checksum lưu MySQL. Avatar lưu ở `source/be/storage/avatars`; chưa có object storage/backup production.
6. Lập/sửa Phiếu 4 ở trạng thái `DRAFT`, gửi thành `PENDING`, KCS/PTN nhận hoặc từ chối tại `/admin/transfers`; mẫu chỉ bị khóa khi gửi và thành `TRANSFERRED` sau khi nhận.
7. Lập biên bản 5 tại `/admin/testing`; `DRAFT/RETURNED → SUBMITTED → REVIEWED`, đồng bộ mẫu `IN_TESTING → AWAITING_REVIEW → TEST_REVIEWED`.

## API và dữ liệu thực tế

- Auth: `GET /api/v1/auth/csrf`, `POST /login`, `POST /refresh`, `POST /logout`, `GET /me`.
- User: `GET|POST /api/v1/users`, `PATCH /api/v1/users/{id}`; Admin-only.
- Intake: `GET /api/v1/receipts?page=0&size=20&sort=receivedAt,desc`, `POST /receipts`, `GET /receipts/{id}`, `POST /receipts/{id}/attachments`, `GET /attachments/{attachmentId}/content`, `GET /receipts/{id}/form-3` (export DOCX đang cần kiểm tra Word thật).
- Transfer: `GET|POST /api/v1/transfers`, `GET|PUT /api/v1/transfers/{id}`, `POST /submit`, `POST /decision`.
- Testing: `GET|POST /api/v1/testing-records`, `GET|PUT /api/v1/testing-records/{id}`, `POST /submit`, `POST /review`.
- Bảng hiện có: 6 bảng nền/intake, `sample_transfers`, `sample_transfer_items`, `testing_records`, `testing_equipment`, `testing_reagents`, `testing_measurements`.
- Schema hiện được quản lý bằng Flyway: baseline là `db/migration/V1__baseline_schema.sql`, migration mới chạy tự động khi backend startup. Hibernate dùng `validate`; các file `db/migration-*.sql` cũ chỉ giữ để tham chiếu. Chuỗi MySQL dùng `VARCHAR/TEXT` + `utf8mb4`, không dùng `NVARCHAR`.

## Xác minh Phase 1/3 (lịch sử, đầu 16/11/2026 — không phải kết quả mới nhất; kết quả mới nhất ở mục P5–P6 19/11)

- Backend `mvn test` trên Java 21: 9/9 pass; có test nhiều mẫu/attachment, pagination + sort whitelist, sửa nháp và quyền ẩn nháp khỏi KCS.
- Frontend `npm run build`: pass, 63 modules.
- MySQL smoke Phase 1/3: tạo phiếu hai mẫu, upload Form 3, tạo/sửa/gửi Phiếu 4, Admin nhận và đồng bộ `TRANSFERRED`: pass.
- Google Chrome thật qua CDP: 7 route intake/transfer, xác nhận 9 sort options + thanh phân trang và route lập Phiếu 4 tương thích contract mới; 1424px/390px không horizontal overflow, console không có lỗi.
- Chưa có bộ integration/Testcontainers hoàn chỉnh; kiểm thử trước đây của Phiếu 5 vẫn pass nhưng chưa chạy lại trong slice Phase 1/3 này.

## Việc tiếp theo

1. Nghiệm thu Chrome E2E bằng tài khoản thật cho từng role (ADMIN, RECEPTIONIST, TECHNICIAN) đi hết Phiếu 1 → 7 → 2 → 3 → 4 → 5 → phát hành, kèm file thật; restart backend để dashboard Admin hết 404.
2. Nghiệm thu export DOCX Phiếu 3 bằng Word thật; export DOCX Phiếu 1/7/2 chưa có.
3. Chốt với LAB: format mã nhập tay, quyền sửa mã (chỉ ADMIN), xử lý trùng/dữ liệu cũ, số niêm phong container, số/No. từng phiếu, metadata file kết quả, cập nhật CFC-TN-14; dọn test cũ `SampleCodingServiceTest` nếu còn lệch quy trình.
4. Chạy migration V11 trên database làm việc sau khi backup; quyết định lưu trữ/backup file, domain/TLS cho tra cứu public.
5. ZBS/SMS (P6) chỉ làm khi LAB cung cấp OA/provider, template, consent, sandbox.

## Cách nạp ngữ cảnh tiết kiệm token

- Bắt đầu bằng file này; dùng CodeGraph để lấy symbol/call path trước khi đọc source.
- Chỉ đọc `plans/CURRENT.md` khi lập kế hoạch; đọc `plans/intake/OVERVIEW.md` và role plan tương ứng khi làm workspace tiếp nhận/WebSocket nội bộ; đọc `docs/INTAKE_NOTIFICATIONS.md` khi làm Zalo/SMS; `docs/API_CONVENTIONS.md` khi đổi API; `docs/DATABASE.md` khi đổi schema; `docs/RBAC.md` khi đổi quyền.
- Chỉ mở skill phù hợp tác vụ. Không tải toàn bộ references của skill hoặc toàn bộ `docs/`.
- `docs/ai/CHANGELOG.md` là lịch sử, không phải file cần đọc đầu phiên.

## Bổ sung KCS 16/11/2026

- Chi tiết receiving hiển thị riêng số lượng/khối lượng và thông tin bàn giao có sẵn; reload sau 409 thất bại không còn báo tải thành công. Dashboard hiển thị thời gian chờ, không đặt SLA.
- Thêm test HTTP/MySQL chặn truy cập attachment khác receipt, KPI/phân trang 21 phiếu và ranh giới receivedAt. Bộ test Java 21 chạy trên MySQL/Redis tạm: 32 test, 0 failures/errors/skipped; frontend build pass.
- Chưa nghiệm thu Chrome E2E bản cuối, notification sau commit/rollback và đầy đủ sort/ranh giới thời gian. Thiếu ảnh vẫn là giả định MVP; chính sách tên khách trong notification cần LAB xác nhận.

## Hồ sơ kết quả — bước nền

- V6 và API nội bộ chỉ tạo/đọc nháp cho Phiếu 5 `REVIEWED`; chưa có upload hoặc phát hành. Người dùng yêu cầu chặn phát hành đến khi hoàn tất gate mã hóa CFC-TN-14.
- QR và tra mã nhập tay tương lai đều yêu cầu 4 số cuối SĐT; mặc định chỉ PDF public, Admin có thể bật Word/Excel; 10 MB/file và 10 file/hồ sơ. Tra cứu và các bảo vệ công khai chưa triển khai.
- `source/public/tracuu/` là landing xem trước không login; form bị khóa, chưa tra cứu được. Không public trước khi hoàn thành và kiểm thử bảo mật backend/phát hành.

## P0–P4 hồ sơ yêu cầu và lấy mẫu — cập nhật 19/11/2026

- Đã triển khai feature `sampling` và migration `V11__test_request_sampling_flow.sql` cho Phiếu 1, kế hoạch Phiếu 7, Phiếu 2; các hồ sơ liên kết về Phiếu 3 bằng khóa nội bộ và có UI chọn/tự điền.
- Phiếu 3 nhận một hoặc nhiều mẫu/lô, có mã mẫu nhập tay, tách seal mẫu/container và kiểm tra trùng. Theo quy trình mới, không chặn bàn giao vì xác minh hai người; KCS nhận/từ chối ở Phiếu 4.
- Đã nối Phiếu 5 với luồng tải file kết quả DOCX/PDF cho TECHNICIAN/ADMIN, tạo hồ sơ kết quả và phát hành trực tiếp bởi ADMIN; màn hình tạo mới không bắt nhập các bảng thiết bị, hóa chất, QC và số liệu thô.
- Kiểm tra gần nhất: Java 21/Docker compile `BUILD SUCCESS`; `npm run build` `✓ built in 951ms`.
- Full backend suite trên DB test cô lập: `Tests run: 85, Failures: 2, Errors: 1, Skipped: 3`; `BUILD FAILURE`. Lỗi còn lại là test cũ của quy trình xác minh hai người, chưa được chuyển đổi; chưa được coi là pass.
- Chưa nghiệm thu Chrome E2E tài khoản thật, Word thật, WebSocket hai phiên, upload file thật toàn luồng và migration trên database làm việc. Không sửa V1–V4, `.env` hoặc secrets trong phiên này.

## P5–P6 — cập nhật 19/11/2026

- P5: dashboard Admin đã có API riêng `/api/v1/admin/dashboard` và UI `/admin`, tổng hợp Phiếu 1/mẫu theo kỳ, Phiếu 4, Phiếu 5 và hồ sơ kết quả. Endpoint chỉ cho ADMIN.
- Chrome MCP đã kiểm tra phiên TECHNICIAN thật ở 390×844, 1024×768, 1440×900 và 2560×952; role redirect và layout không tràn ngang. Chưa có bằng chứng Chrome với ADMIN/RECEPTIONIST cho dashboard mới.
- P6: phần gửi Zalo/SMS chưa triển khai vì thiếu OA/provider, template, consent và sandbox. Chỉ được triển khai tiếp sau khi LAB cung cấp các đầu vào này; hiện chưa có credential/webhook/worker gửi thật.
- Kiểm tra lại sau khi cập nhật P5: backend full suite MySQL/Redis test cô lập `Tests run: 85, Failures: 0, Errors: 0, Skipped: 3`, `BUILD SUCCESS`; frontend `npm run build` `✓ built in 1.03s`. Runtime backend đang mở chưa restart nên Chrome gọi endpoint Admin mới nhận 404; chưa coi E2E Admin hoàn tất.

## UI tiếp nhận mẫu — cập nhật 19/11/2026

- Nhóm “Chỉ tiêu yêu cầu” trên màn tạo hồ sơ tiếp nhận dùng checklist nhiều lựa chọn gồm Cadimi, Auramine O, dư lượng thuốc BVTV và mục Khác; ô Khác mở trường nhập riêng và dữ liệu được chuẩn hóa trước khi gửi API.
- Đã giới hạn kích thước checkbox và đồng bộ viền, nền, focus, responsive với form tiếp nhận; frontend `npm run build` pass (95 modules), `git diff --check` pass.
- Chrome hiện đăng nhập vai trò KCS nên route `/admin/intake/new` bị chuyển về `/admin/receiving`; chưa chụp nghiệm thu trực quan màn tạo tiếp nhận trong phiên này.

## Phiếu chuyển mẫu và bằng chứng bàn giao — cập nhật 19/11/2026

- Màn tạo/chi tiết Phiếu chuyển mẫu tách rõ `requestNumber` của Phiếu yêu cầu kiểm nghiệm và `receiptCode` của hồ sơ tiếp nhận; bảng chi tiết dùng đúng tên mẫu, mã mẫu, chỉ tiêu yêu cầu và phương pháp thử theo biểu mẫu số 4.
- Bổ sung tải nhiều file hoặc chụp ảnh trực tiếp khi lập/sửa bản nháp chuyển mẫu. File ảnh/PDF/DOCX tối đa 10 MB được lưu vào hồ sơ nguồn với loại `TRANSFER_EVIDENCE`; KCS thấy nhóm riêng “Bằng chứng bàn giao” trong màn nhận mẫu.
- Kiểm tra: frontend production build pass (95 modules), backend Java 21 `mvn -DskipTests compile` trong Docker `BUILD SUCCESS`, `git diff --check` pass. Phiên Chrome đang ở role KCS nên chưa nghiệm thu trực quan màn tạo Phiếu chuyển mẫu.

## Số Phiếu yêu cầu CFC26.xxxx và liên kết Phiếu 07/02 — cập nhật 19/11/2026
- Số/No. Phiếu 1 theo `CFC{YY}.{NNNN}` (≥4 chữ số, quá 9999 tự thêm chữ số, tối đa 6); YY mặc định năm hiện hành giờ Việt Nam và sửa được; hệ thống gợi ý số kế tiếp (`GET /api/v1/test-requests/next-number`), người dùng có thể gõ tay, trùng → 409. Bộ đếm `test_request_counters` (V12, `TestRequestNumberSequence`). Phiếu 07/02 dùng chung số Phiếu 1 (UI chỉ đọc, backend bỏ qua số client gửi).
- Frontend `TestRequestFormPage`: mục III là checklist theo mẫu Word (Cadimi: Ruột; Vàng O: Vỏ/Ruột; phương pháp thử mỗi hàng; không có dòng Khác; một ô tên/mã mẫu mỗi khối, trống → `Mẫu {TT}`; mã mẫu chính thức đặt ở Phiếu 3). 07 tự điền từ Phiếu 1 (người liên hệ, nguồn gốc), 02 tự điền từ 07; có nút "Lấy lại…", tóm tắt chỉ đọc, và khối 07/02 dùng chung style CFC. `ReceiptCreatePage` gom các dòng chỉ tiêu theo mẫu khi tự điền Phiếu 3.
- Kiểm tra: `TestRequestNumberingTest` 9/9 (gồm tạo song song, đổi năm bằng Clock, >9999, 409/400, 403 TECHNICIAN); full backend suite Docker MySQL/Redis cô lập `Tests run: 95, Failures: 0, Errors: 0, Skipped: 1`; `npm run build` pass.
- **Chưa kiểm tra:** Chrome thật ở 4 kích thước và thao tác tay (chưa có phiên ADMIN/RECEPTIONIST), migration V12 trên database làm việc, mục IV/V/VI và nhãn song ngữ của Phiếu 1 (chưa làm).

## Phiếu 07/02 theo mẫu Word — cập nhật 19/11/2026
- Form 07 và 02 trong `TestRequestFormPage` bám hai file `7.Ke_hoach_lay_mau_sau_rieng.docx` và `2. Phiếu lấy mẫu hiện trường.docx`: nhóm I (khách hàng, địa chỉ, email, số giấy đề nghị lấy chỉ đọc từ Phiếu 1; người đại diện/điện thoại lấy sẵn từ người liên hệ/SĐT khách nhưng sửa được), nhóm II (tên mẫu, giống chỉ đọc; mã vùng trồng, cơ sở đóng gói, xe, moóc, container). 07 có checklist dạng mẫu (Nguyên trái/Ruột/Vỏ), cỡ lô → số trái, điều kiện vận chuyển; 02 có tình trạng mẫu, bảo quản (+°C), niêm phong Có/Không và bảng mẫu mỗi dòng tick Vàng O/Cd/Pb/As + khác. Migration V13, bảng `field_sampling_items`.
- Kiểm tra: `TestRequestNumberingTest` 10/10 (có test khứ hồi checklist); `npm run build` pass. **Chưa kiểm tra bằng Chrome thật**; chưa có "Mã số mẫu" ở Phiếu 02 (mã đặt ở Phiếu 3); chưa export DOCX 07/02.
- 19/11/2026 (sau): Phiếu 07 và 02 tách thành hai trang riêng `/admin/requests/:id/sampling-plan` và `/admin/requests/:id/field-sampling` (`SamplingPlanPage`, `FieldSamplingPage`, dùng chung `components/SamplingParts.jsx` + `samplingModel.js`); trang Phiếu yêu cầu chỉ còn form Phiếu 1 và khối "Các bước tiếp theo" (trạng thái Đã lập/Chưa lập). Đã chụp Chrome headless với API giả (không dùng tài khoản thật) ở 1440 và 390 px: không tràn ngang, radio/checkbox đúng kích thước. Chưa kiểm tra bằng backend và tài khoản thật.
- 19/11/2026 (tiếp): Phiếu 1 chia mục I–VI đúng file Word (khách hàng; mẫu + radio nguồn lấy mẫu; bảng III; mục đích tick Nội bộ/Đánh giá lô/Yêu cầu khách/Khác; thời gian trả kết quả + thông báo khi chậm Điện thoại/Email/Khác; xác nhận + hoàn tất), nhãn song ngữ. Mục đích và thông báo khi chậm lưu thành chuỗi "A; B; Khác: …" (không đổi schema). Mọi mục của Phiếu 1 và nhóm trong 07/02 có nút thu gọn/mở rộng. `window.confirm` đã thay bằng `ConfirmDialog`. Chrome headless + API giả: thu gọn, hộp thoại, không tràn ngang 390 px, không lỗi console — chưa thử với backend/tài khoản thật. Chưa có: ô "Mã số mẫu" ở Phiếu 02, khối chữ ký/ngày ký, export DOCX.
- 19/11/2026 (danh sách): `/admin/requests` dùng `ListToolbar` + `Pagination` + `useListState` như các danh sách khác: tìm kiếm có debounce, lọc ngày yêu cầu/trạng thái/nguồn lấy mẫu, 12 kiểu sắp xếp, phân trang 20/trang, bảng responsive, chip tiến độ 07/02. Backend `TestRequestService.search` (Specification, `ListQuery`). Test `TestRequestNumberingTest` 11/11; build FE pass; Chrome headless + API giả ở 1440 và 390 px đúng bố cục. Chưa thử với dữ liệu và tài khoản thật.

## Cập nhật 19/11/2026 — số dùng chung 3 phiếu + kiểm thử thật

- Phiếu yêu cầu, Kế hoạch lấy mẫu (07), Lấy mẫu hiện trường (02) **luôn dùng chung Số/No `CFC{YY}.{NNNN}`**: `savePlan`/`saveField` luôn gán `requestNumber` của Phiếu 1 (kể cả dữ liệu cũ), bỏ qua `formNumber` client gửi; V14 đồng bộ dữ liệu cũ.
- Kiểm thử: `TestRequestNumberingTest` 11/11; toàn bộ backend 97 test, 0 lỗi, 1 skipped (Docker MySQL/Redis). Đã chạy backend thật trên `cfc_lab` (Flyway tới V14) và thử qua API bằng tài khoản RECEPTIONIST: tạo/lưu 07/02/trùng số 409/tìm kiếm/sắp xếp đều đúng. **Chưa** thử bằng Chrome với backend thật.
- Dữ liệu thử còn lại trong `cfc_lab`: phiếu `CFC26.0002` (khách "TEST TỰ ĐỘNG - xóa được") — cần xóa tay nếu không giữ. DB còn phiếu cũ `CFC26.00001` (5 chữ số, hợp lệ theo quy tắc 4–6 chữ số).
- Chưa làm: ô "Mã số mẫu" trong Phiếu 02 (mã đặt ở Phiếu 3), khối chữ ký/ngày và xuất DOCX cho Phiếu 1/07/02.

## Cập nhật 19/11/2026 — Tạo hồ sơ tiếp nhận (Phiếu 3) `/admin/intake/new`

- Ô "Số/No. Phiếu yêu cầu" là combobox (datalist, tìm qua `GET /test-requests?q=`): chọn số → `getTestRequestByNumber` → tự điền khách hàng, lô hàng, lấy mẫu hiện trường và danh sách mẫu; nút "Đổi phiếu" để bỏ liên kết/gõ tay. Link `?requestId=` dùng chung hàm `applyRequest`.
- Chỉ tiêu mỗi mẫu **chọn một** (radio: Cadimi / Auramine O / Dư lượng BVTV / Khác). Khi điền từ Phiếu yêu cầu, mẫu có nhiều chỉ tiêu được tách thành nhiều dòng (mỗi dòng 1 chỉ tiêu) — đã được người dùng xác nhận ngày 19/11/2026.
- "Thêm mẫu" sao chép mẫu liền trước (tên, mã nguồn, bao bì, tình trạng…) trừ chỉ tiêu; mã mẫu tự tăng số đuôi (…-001 → …-002) nếu có.
- Các mục 01–05 và từng mẫu thu gọn/mở được (`FormCard`, `SampleCard`, dùng lại `Chevron` của sampling). Chỉ kiểm bằng Chrome headless + API giả (1440/390 px, không tràn ngang); chưa thử lưu với backend thật.

## Cập nhật 19/11/2026 — Phiếu 3 đồng bộ mẫu Word

- Xuất DOCX (`ReceiptForm3Exporter`, template `templates/phieu-tiep-nhan-ban-giao-mau.docx` đã vá lại): **mỗi mẫu một phiếu** (`GET /receipts/{id}/form-3?sampleId=`; không có `sampleId` = mẫu đầu). Tick các ô ☐ theo dữ liệu (bao bì, niêm phong, tình trạng, nhiệt độ Thường/Lạnh/Khác, chỉ tiêu, kết luận), điền số lượng, ngày giờ nhận, hạn dự kiến, tên người giao/nhận dưới chữ ký; niêm phong in "Còn nguyên"/"Không có/đã mở" (không còn `INTACT`); bỏ chèn đôi nội dung vận chuyển; ô "Người phê duyệt/Ngày" để trống cho ký tay (không còn dòng "Chưa có phê duyệt độc lập").
- Chỉ tiêu ngoài Cadimi/Auramine (vd. "Dư lượng thuốc BVTV") được in vào ô "Khác:" vì mẫu Word chưa có ô riêng — **cần xác nhận** có thêm ô vào mẫu Word không.
- Form `/admin/intake/new`: thêm "Bảo quản sau khi nhận"; "Tình trạng mẫu" thành checklist (Nguyên vẹn loại trừ các mục còn lại; "Dấu hiệu bất thường" bắt buộc mô tả); nút xuất Phiếu 3 theo từng mẫu ở màn hình thành công.
- Niêm phong vẫn 3 trạng thái (Còn nguyên/Không có/Đã mở); Word gộp "Không có/đã mở".
- Ghi chú cuối file Word mẫu (số niêm phong/seal/chỉ tiêu "cần check"; bỏ bước kiểm tra mã hóa, chị Thúy mã hóa nhập tay; thiếu 1 biên bản nhận mẫu; bỏ loại thiết bị khi xuất kết quả) **chưa xử lý — cần LAB xác nhận**, đặc biệt mâu thuẫn với bước xác minh mã mẫu V7.
- Kiểm thử: backend 98 test, 0 lỗi, 1 skipped; đã tạo phiếu thật trên `cfc_lab` qua API, xuất DOCX và render PDF bằng LibreOffice để đối chiếu mẫu. Dữ liệu thử còn lại trong `cfc_lab`: 2 phiếu tiếp nhận (4 mẫu `CT999-SR-19112026-0xx`, khách "TEST TỰ ĐỘNG - xóa được") và Phiếu yêu cầu `CFC26.0002`. Chưa mở bằng Word thật.

## Cập nhật 20/11/2026 — logo 3 màu, trang đăng nhập mới, đồng bộ giao diện
- Logo web dùng con cò 3 màu (`guideline-assets/cfc-006.png`, đã tách nền đen thành trong suốt) tại `source/fe/src/assets/brand/cfc-logo.png`. Favicon `fe/public/favicon.png`, `favicon-48.png`, `apple-touch-icon.png`; trang `/tracuu/` có `cfc-logo.png` và `favicon.png` riêng. Logo tròn cũ `cfc-008.png` không còn được dùng. PDF KQKN và mẫu Word Phiếu 3 đã có logo 3 màu từ trước.
- `LoginPage`: thiết kế tối "aurora" (CSS tiền tố `.auth-*`). Đăng nhập xong vào `/admin`, mỗi role tự chuyển về tổng quan của mình. CSS đăng nhập cũ (`.login-*`) đã gỡ.
- Khung quản trị có mặc định sáng cho ô nhập, ô tick và ô bảng qua `.admin-shell :where(...)`, độ ưu tiên thấp nên style riêng của trang vẫn thắng.
- `DateTimeInput` mới trong `shared/components/DateInput.jsx` (ngày `dd/mm/yyyy` + giờ 24h `hh:mm`, giá trị vẫn `yyyy-mm-ddTHH:mm`). `Field` của Phiếu 1/07/02 tự dùng `DateInput`/`DateTimeInput` khi `type` là `date`/`datetime-local`.
- Danh sách Chuyển mẫu tìm và sắp theo Số/No (`sort=requestNumber,...`). KCS tìm thêm theo Số/No. Thông báo KCS nhận/từ chối ghi "mẫu của Số/No. …".
- Danh sách việc còn mở về giao diện: `plans/UI_AUDIT_2026-11-20.md`, mục 7.
