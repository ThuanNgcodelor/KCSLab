# CFC Lab Management — Bàn giao để tiếp tục ở máy khác

**Cập nhật:** 14/11/2026
**Nhánh:** `main`  
**Mốc Git tiếp nhận/pagination:** `ca3b44a`
**Ưu tiên hiện tại:** Intake I1 — Dashboard/API query thật; tiếp theo notification nội bộ/WebSocket và form Phiếu 3

> Đây là file cần đọc đầu tiên khi tiếp tục dự án ở máy khác. Không ghi mật khẩu, JWT secret hoặc thông tin đăng nhập vào file này; lấy chúng từ `.env` cục bộ.

## 1. Tóm tắt ngắn

Hệ thống hiện đã chạy được luồng nội bộ:

`Đăng nhập → quản lý nhân viên → tiếp nhận mẫu (Phiếu 3) → chuyển mẫu (Phiếu 4) → thử nghiệm và kiểm tra nội bộ (Phiếu 5)`

Phase 1 và Phase 3 vừa được hardening: Phiếu 3 lưu hồ sơ nguồn/nhiều mẫu/ảnh bằng chứng; Phiếu 4 có bản nháp sửa được, chỉ khóa mẫu khi gửi và không lộ bản nháp cho KCS/PTN.

Theo ưu tiên mới, module Tiếp nhận được tách thành plan page chung và ba role tại `plans/intake/`. Làm API dashboard/search/filter thật trước, rồi notification nội bộ lưu bền + header ba tin + page riêng + WebSocket; sau đó refactor form Phiếu 3. ZBS/SMS gửi khách vẫn là plan riêng tại `docs/INTAKE_NOTIFICATIONS.md`.

Ranh giới runtime hiện tại dừng ở trạng thái `TEST_REVIEWED`. Chưa có phát hành Phiếu kết quả/PDF, thu hồi hoặc đính chính kết quả, và chưa có trang khách hàng tra cứu bằng mã + đuôi số điện thoại.

Việc phải làm tiếp trước tiên không phải là PDF. Làm Intake I1–I5 để bỏ dashboard hard-code, đưa query về backend, thêm notification/WebSocket và sửa form theo Phiếu 3; sau đó tiếp tục customer notification và gate 5A theo CFC-TN-14.

## 2. Những gì đã triển khai

### Backend

- Java 21, Spring Boot 4.1.2, Maven, package-by-feature.
- Đăng nhập bằng email; JWT access token trong cookie, refresh session và CSRF.
- Không có đăng ký công khai. Admin khởi tạo từ biến môi trường.
- Quản lý nhân viên: tạo, sửa quyền và khóa tài khoản.
- Tiếp nhận mẫu/Phiếu 3: customer, receipt, nhiều sample, hồ sơ nguồn, actor, attachment phân loại, ảnh chụp mobile, checksum SHA-256 và mã mẫu gốc.
- Danh sách tiếp nhận dùng pagination phía server mặc định 20/trang, tối đa 100 và sort whitelist; frontend có 9 lựa chọn sort cùng thanh điều hướng trang.
- Chuyển mẫu/Phiếu 4: tạo/sửa nháp, gửi bàn giao, giao/nhận hoặc từ chối, lưu/hoàn mẫu, cập nhật trạng thái; KCS/PTN không thấy bản nháp.
- Thử nghiệm/Phiếu 5: thiết bị, hóa chất, đường chuẩn, công thức, QC, raw data, submit, trả lại hoặc kiểm tra.
- MySQL dùng schema SQL thủ công và Hibernate `validate`; profile mặc định dùng H2.

### Frontend

- React 19, Vite 6, JSX; không dùng TypeScript hoặc UI library.
- Các route chính:
  - `/login`
  - `/admin`
  - `/admin/users`, `/admin/users/new`, `/admin/users/:id/edit`
  - `/admin/intake`, `/admin/intake/new`, `/admin/intake/:id`
  - `/admin/transfers`, `/admin/transfers/new`, `/admin/transfers/:id`, `/admin/transfers/:id/edit`
  - `/admin/testing`
- Đã bỏ phần “topup”; form thêm nhân viên dùng trang riêng.
- Giao diện dùng nền sáng/trắng, xanh CFC làm điểm nhấn để giữ độ tương phản.
- Logo dùng asset thật tại `guideline-assets/cfc-008.png`.
- Sidebar cố định theo viewport trên desktop, chuyển thành drawer khi màn hình `<=1080px`.
- Page shell co giãn theo viewport, giới hạn đọc tối đa `1680px`; bảng dày dữ liệu chuyển card/stack ở mobile.
- Footer CFC đã được làm lại và có liên kết trực tiếp tới `https://www.cfccobay.com/`.
- Responsive contract đã được ghi vào `source/fe/AGENTS.md`; page mới phải tuân theo contract này.

## 3. Quyết định nghiệp vụ đã chốt

`CFC14Quy trình mã hóa mẫu trái cây.doc` (CFC-TN-14) là nguồn chuẩn cho quy trình mã hóa.

- Phải tách ba định danh:
  1. mã hồ sơ tiếp nhận;
  2. mã mẫu;
  3. số Phiếu kết quả.
- Mã mẫu dùng công thức `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`.
- Ví dụ hợp lệ: `CT001-SR-23092026-001`.
- Ví dụ `SR-CT001-...` trong phiếu kết quả là sai lệch tài liệu cần rà soát; không đổi generator theo ví dụ đó.
- Mẫu có thể tách phần `-V` và `-R`.
- QC có các loại BL/SP/D; mapping mã mù phải được bảo vệ quyền truy cập.
- Gán mã và đối chiếu mã cần hai người độc lập.
- Không xóa/ghi đè mã cũ khi sửa; phải lưu actor, thời điểm và lý do hiệu chỉnh.
- Chưa chốt quy tắc tạo số Phiếu kết quả. Không được tự suy ra `SRMMYYYY-SEQ` chỉ từ một phiếu ví dụ.

Phân tích chi tiết và acceptance criteria nằm trong `docs/SAMPLE_CODING.md`.

## 4. Trạng thái theo module

| Module | Trạng thái | Ghi chú |
|---|---|---|
| Auth/session | Đã có vertical slice | Login email, refresh, logout, `/me`, CSRF; không đăng ký |
| User/RBAC | Đã có vertical slice | Admin tạo/sửa/khóa; quyền cơ bản theo role |
| Phiếu 3 — Tiếp nhận | Phase 1 hoàn thành | Có hồ sơ nguồn/nhiều mẫu/bằng chứng; còn thiếu gate xác minh mã của Phase 2 |
| Thông báo Zalo/SMS | Đã chốt plan, chưa triển khai | ZBS ưu tiên, SMS dự phòng; chưa có consent/outbox/provider/webhook |
| Phiếu 4 — Chuyển mẫu | Phase 3 hoàn thành | Có nháp/sửa/gửi/nhận; vẫn cần chặn chuyển nếu mã chưa được người thứ hai xác minh |
| Phiếu 5 — Thử nghiệm | Chạy tới `TEST_REVIEWED` | Cần chặn analyst tự review hồ sơ của mình |
| Dashboard | Chỉ là giao diện mẫu | Số liệu và hoạt động đang hard-code |
| Phiếu kết quả/PDF | Chưa triển khai | Chưa có `result_releases`, version/checksum hoặc file bất biến |
| Tra cứu khách hàng | Chưa triển khai | Chưa có mã + đuôi điện thoại, rate limit hoặc public projection |

## 5. Các lỗi thiết kế/rủi ro đã biết

- `ReceiptService.create` đang sinh sequence bằng `countByReceivedDate + 1`: có race condition khi hai hồ sơ được tạo đồng thời; timezone nghiệp vụ đã dùng `Asia/Ho_Chi_Minh`.
- Model chưa có sample part `-V/-R`, QC, mã mù, người xác minh và lịch sử hiệu chỉnh mã.
- Chưa có WebSocket thông báo chuyển mẫu; QR chưa được sinh vì điểm sinh phải nằm sau gate xác minh mã hoặc tại phát hành kết quả.
- Chưa có notification runtime: form consent/kênh, template đã duyệt, outbox/delivery, worker, ZBS/SMS adapter và callback đều chưa được triển khai.
- Receipt list chưa có search/filter phía server; dashboard đang hard-code; chưa có app header, notification persistence/page hoặc WebSocket dependency.
- `TestingRecordService.review` chưa kiểm tra người review phải khác analyst.
- Dashboard không được dùng làm báo cáo thật vì đang hard-code.
- File đang lưu local; chưa có object storage, retention và backup production.
- Chưa có Flyway/Liquibase, audit log tổng quát, rate limit và bộ integration/Testcontainers đầy đủ.

## 6. Việc tiếp theo — làm theo thứ tự

### Slice kế tiếp: workspace Tiếp nhận I1–I3

1. Đọc `plans/intake/OVERVIEW.md` và role plan liên quan.
2. Mở rộng receipt query bằng criteria/specification hoặc custom query; hỗ trợ search, khoảng ngày, trạng thái, kết luận, loại mẫu, người nhận, page/size/sort tại database.
3. Tạo aggregate endpoint riêng cho dashboard Receptionist/Admin và activity API; thêm index/test MySQL.
4. Nối dashboard/list với API thật rồi xóa constants `stats/activity`; frontend không tự filter/sort/paginate.
5. Tạo notification nội bộ lưu bền, API latest 3/page 20, app header và notification page.
6. Sau đó thêm Spring WebSocket/STOMP, JWT-cookie handshake, user destination, reconnect/dedupe và REST reconcile.

### Slice sau đó: form Phiếu 3 I4–I5

1. Tách route tổng quan/danh sách/tạo/chi tiết; landing theo role.
2. Refactor form theo sáu mục Phiếu 3, giữ bố cục biên bản số hóa và mobile capture.
3. Thay select `SR/MIT/OTHER` bằng combobox search + nhập mới; product code do catalog/Admin duyệt.
4. Thêm draft/completion, unsaved warning, retry file và event sau commit.
5. KCS là đích cố định; không thêm selector nhiều phòng.

### Slice customer notification

1. Thực hiện N0–N2 trong `docs/INTAKE_NOTIFICATIONS.md` bằng fake provider.
2. Chỉ tích hợp ZBS/SMS khi có account, template duyệt, credential và sandbox.

### Slice sau notification: hoàn thiện nền mã hóa

1. Đọc `docs/SAMPLE_CODING.md` và phần 5A trong `plans/CURRENT.md`.
2. Tạo bộ cấp sequence nguyên tử theo ngày `Asia/Ho_Chi_Minh`; không dùng phép đếm số bản ghi.
3. Viết test cho tạo mã đồng thời và ranh giới nửa đêm Việt Nam.
4. Tách các bước: tạo hồ sơ → chấp nhận mẫu → gán mã → người thứ hai xác minh.
5. Tạo sample part `-V/-R`; chặn Phiếu 4 nếu mã chưa được xác minh.
6. Sau đó mới làm QC/mã mù và projection dữ liệu giới hạn cho kỹ thuật viên.
7. Sau khi event nghiệp vụ ổn định mới thêm WebSocket; QR mẫu chỉ sinh sau xác minh mã.

### Slice phát hành sau đó

1. Chặn analyst tự review hoặc tự phát hành kết quả của chính mình.
2. Chốt quy tắc số Phiếu kết quả và vai trò/người ký với người dùng.
3. Thiết kế `result_releases`: version, trạng thái, approver, thời điểm, checksum và snapshot.
4. Tạo PDF bất biến; thu hồi/đính chính tạo lịch sử mới, không ghi đè.
5. Cuối cùng mới làm public lookup bằng mã + đuôi điện thoại, rate limit và projection an toàn.

Không mở rộng sang QR/OTP, lịch sử vùng trồng đầy đủ hoặc hạ tầng production trước khi các gate trên hoàn thành, trừ khi người dùng đổi ưu tiên.

## 7. File cần đọc, theo mức ưu tiên

1. `AGENTS.md` — quy tắc làm việc chung và yêu cầu dùng CodeGraph.
2. `docs/ai/HANDOFF.md` — file này.
3. `plans/CURRENT.md` — kế hoạch và checklist hiện hành.
4. `plans/intake/OVERVIEW.md` — plan page/API/header/WebSocket/form và thứ tự I1–I6.
5. Role plan tương ứng trong `plans/intake/`.
6. `docs/INTAKE_NOTIFICATIONS.md` — contract ZBS/SMS, outbox, consent và retry.
7. `docs/SAMPLE_CODING.md` — nguồn phân tích mã hóa CFC-TN-14.
8. Chỉ khi sửa tương ứng mới đọc:
   - `docs/API_CONVENTIONS.md`
   - `docs/DATABASE.md`
   - `docs/RBAC.md`
   - `source/fe/AGENTS.md`
   - `source/be/AGENTS.md`

Các tài liệu nghiệp vụ gốc ở root, không tự ý xóa hoặc sửa:

- `1. Phiếu KQKN-DV.docx`
- `3. Phiếu tiếp nhận và bàn giao mẫu.docx`
- `4.Phieu chuyen mau.docx`
- `5.Bien_ban_thu_nghiem.docx`
- `CFC14Quy trình mã hóa mẫu trái cây.doc`
- `KQKN-SR-CT001-092026.docx`

## 8. Cách chạy lại ở máy nhà

### Chuẩn bị

- Cài Java 21, Maven, Node.js/npm và MySQL nếu muốn dùng profile MySQL.
- Sao chép `.env.example` thành `.env` và điền secret cục bộ. Không commit `.env`.
- Nếu dùng MySQL mới, tạo database và chạy `source/be/src/main/resources/db/schema.sql` trước; Hibernate đang ở chế độ `validate`.
- Nếu database đã có schema cũ, chạy `source/be/src/main/resources/db/migration-phase1-phase3.sql` một lần trước khi khởi động backend.

### Backend với MySQL

Tại root dự án:

```bash
set -a
source .env
set +a
cd source/be
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

Muốn chạy nhanh bằng H2, bỏ `SPRING_PROFILES_ACTIVE=mysql`.

### Frontend

Trong terminal khác:

```bash
cd source/fe
npm install
npm run dev
```

Vite đã bind `0.0.0.0:5173` và proxy `/api` về backend `127.0.0.1:8080`.

- Trên chính máy chạy: `http://localhost:5173`.
- Trên điện thoại cùng Wi-Fi: chạy `hostname -I`, chọn địa chỉ LAN rồi mở `http://<LAN-IP>:5173`.
- Khi dùng IP LAN, thêm origin tương ứng vào `FRONTEND_ORIGIN` trong `.env`, phân tách nhiều origin bằng dấu phẩy, rồi restart backend.

### Kiểm tra cơ bản

```bash
cd source/be
mvn test
```

```bash
cd source/fe
npm run build
```

Sau khi sửa một luồng nghiệp vụ, chạy Playwright trên cả desktop và mobile; với thay đổi layout, kiểm tra tối thiểu `390x844`, `1024x768`, `1440x900`, `2560x952` và bảo đảm không có horizontal overflow.

## 9. Kết quả kiểm tra gần nhất

- Backend `mvn test`: 9/9 pass trên Java 21.
- Frontend `npm run build`: pass, 63 modules.
- MySQL smoke Phase 1/3: tạo Phiếu 3 hai mẫu, upload Form 3, tạo/sửa/gửi Phiếu 4, nhận mẫu và đồng bộ `TRANSFERRED`: pass.
- Google Chrome CDP: 7 route intake/transfer tại desktop 1424px và mobile 390px, pagination/sort hiển thị đúng, không horizontal overflow, console không có lỗi.
- Backend và frontend đã được xác nhận chạy lần cuối tại `127.0.0.1:8080` và `localhost:5173`; vẫn nên kiểm tra lại process khi mở phiên mới.

## 10. Trạng thái Git và cách mang về nhà

Phase 1/3 và pagination nền đã ở commit `ca3b44a` trên `main`/`origin/main`. Worktree hiện còn tinh chỉnh card mobile, Chrome smoke/artifact, Project Memory, plan notification và bộ plan Intake theo role chưa commit; luôn xem `git status` và diff trước khi commit/push.

Không dùng `git reset --hard` hoặc xóa tài liệu nghiệp vụ/asset để “làm sạch”. Luôn xem `git status` trước khi sửa vì thay đổi chưa commit có thể thuộc về người dùng.

## 11. Prompt dùng để tiếp tục với AI ở máy nhà

```text
Đọc AGENTS.md, docs/ai/HANDOFF.md, plans/CURRENT.md và plans/intake/OVERVIEW.md; đọc role plan tương ứng. Dùng CodeGraph trước source. Ưu tiên Intake I1: backend search/filter/page + aggregate dashboard và test; nối frontend rồi xóa hard-code. Tiếp theo làm notification nội bộ lưu bền, header ba tin/page riêng và WebSocket authenticated user destination; sau đó refactor Phiếu 3 theo sáu mục với product catalog nhập tay có kiểm soát. KCS là một phòng. Chưa tích hợp ZBS/SMS nếu thiếu sandbox/template; không thay đổi secret và cập nhật Project Memory sau test.
```
