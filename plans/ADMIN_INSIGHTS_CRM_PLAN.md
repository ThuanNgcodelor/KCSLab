# Plan — Admin Insights: Dashboard nâng cao, Khách hàng (CRM-lite) và tiện ích vận hành

**Ngày lập:** 27/11/2026 · **Người lập:** Claude (plan) · **Người code:** Codex · **Trạng thái:** ĐỀ XUẤT — chưa triển khai, chưa kiểm thử

## 1. Mục tiêu

Biến trang Admin từ "đếm trạng thái" thành công cụ điều hành: thấy xu hướng, thấy khách hàng (mới/cũ/quay lại), thấy điểm nghẽn quy trình, và có lối tắt xử lý. Tham khảo cách CRM/ERP/LIMS làm, nhưng **chỉ lấy phần phù hợp một phòng Lab nhỏ**, không dựng ERP.

## 2. Hiện trạng (có bằng chứng)

| Hạng mục | Hiện trạng | Nguồn |
|---|---|---|
| Dashboard Admin | 4 thẻ KPI + 2 khối (flow, tỷ lệ). Cố định 7 ngày, không biểu đồ, không lọc, không so sánh kỳ trước | `source/fe/src/features/admin/pages/DashboardPage.jsx`, `AdminDashboardService.java` |
| Backend dashboard | `countByStatus` toàn hệ thống (không theo kỳ) + `IntakeDashboardService.summarize(from,to)` | `admin/service/AdminDashboardService.java` |
| Khách hàng | Bảng `customers` (name, phone, email, address); **tự gộp theo `phone`** khi tạo phiếu; `receipts.customer_id` | `V1__baseline_schema.sql`, `ReceiptService.java:162` |
| Trang khách hàng | **Chưa có** route/API riêng | `App.jsx` |
| Admin hiện có | users, test-methods, dossiers (lưu trữ/export), customer-notifications (Zalo) | `App.jsx:75-80` |
| Audit | Có rải rác (receipt edit log V27, dossier, notification op) nhưng **chưa có audit log tập trung** | migrations |
| Đã có nền cho tính năng | `samples.product_type`, `received_at`, `received_by_user_id`, `requested_parameters`, transfer/test/release có mốc thời gian | schema |

Lưu ý: số "khách cũ" tính được ngay từ dữ liệu có sẵn (đếm số phiếu theo `customer_id`). Hạn chế: định danh khách chỉ bằng SĐT — khách đổi số hoặc nhiều đại diện cùng một công ty sẽ bị tách (xem Câu hỏi mở Q1).

## 3. Phạm vi theo giai đoạn

### A. Dashboard nâng cao (ưu tiên 1 — giá trị cao, rủi ro thấp, không cần migration)

- Bộ lọc kỳ: Hôm nay / 7 ngày / 30 ngày / Tháng này / Năm nay / tùy chọn; giờ `Asia/Ho_Chi_Minh`. Mọi số liệu **đếm theo kỳ** (hiện nhiều số đang là toàn thời gian — phải tách rõ).
- So sánh kỳ trước (▲▼ %) cho: số phiếu, số mẫu, số khách, số phiếu KQKN đã phát hành.
- Biểu đồ (SVG/CSS tự viết, **không thêm thư viện UI/chart** theo quy ước FE; nếu cần chart lib phải hỏi lại):
  1. Xu hướng phiếu/mẫu theo ngày/tuần/tháng (line/bar).
  2. Phễu quy trình: Tiếp nhận → Chuyển KCS → KCS nhận → Thử nghiệm → Duyệt → Phát hành (số phiếu **và** số mẫu, tách riêng).
  3. Cơ cấu theo loại sản phẩm (`samples.product_type`) và theo chỉ tiêu yêu cầu (top N).
  4. Workload theo nhân viên (phiếu/mẫu/đang tồn).
- **Thời gian xử lý (turnaround/TAT):** trung vị và P90 cho từng chặng (nhận → chuyển, chuyển → KCS nhận, nhận → phát hành). Danh sách "phiếu tồn lâu nhất". Ngưỡng cảnh báo là **cần xác nhận** (SLA chưa được LAB chốt — không hard-code SLA, để cấu hình hoặc chỉ hiển thị số thô).
- Khối "Cần xử lý ngay": phiếu chờ chuyển quá X ngày, KCS từ chối, hồ sơ chờ duyệt/phát hành, thông báo khách lỗi, export/hồ sơ lỗi — mỗi dòng bấm vào được danh sách đã lọc.
- Click KPI → danh sách lọc đúng cùng điều kiện (drill-down).
- Loading/lỗi/rỗng đầy đủ; responsive 320px đến màn rộng.

### B. Khách hàng — CRM-lite (ưu tiên 2)

- Trang `/admin/customers` (chỉ ADMIN): danh sách phân trang server-side, tìm theo tên/SĐT/email, sort, lọc.
- Cột: tên, SĐT, số phiếu, số mẫu, lần đầu / lần gần nhất, tổng chỉ tiêu, trạng thái phân loại.
- **Phân loại tự động** (tính từ DB, không nhập tay): *Mới* (1 phiếu trong kỳ), *Quay lại* (≥2 phiếu), *Thân thiết* (≥N phiếu/năm — N cần xác nhận), *Ngủ đông* (không có phiếu mới > M ngày — M cần xác nhận).
- Trang chi tiết `/admin/customers/:id`: hồ sơ liên hệ, **timeline** mọi phiếu (Phiếu 3 → 4 → 5 → KQKN, link sang từng phiếu), biểu đồ nhỏ tần suất, loại sản phẩm/chỉ tiêu hay gửi, vùng trồng/cơ sở đóng gói, lịch sử thông báo đã gửi, file KQKN đã phát hành.
- Ghi chú nội bộ + gắn thẻ (VIP, công nợ, lưu ý) — cần bảng mới (xem §5), có người tạo/thời gian.
- **Chỉnh sửa hồ sơ khách + gộp khách trùng** (merge A vào B, chuyển toàn bộ receipts, ghi audit, có xem trước). Đây là việc phải làm vì khóa gộp là SĐT.
- Xuất CSV danh sách khách (giới hạn dòng, ghi audit; có chứa dữ liệu cá nhân nên chỉ ADMIN).
- Gợi ý chăm sóc: "khách ngủ đông cần liên hệ", "khách quay lại hay gửi loại X". Chỉ **hiển thị gợi ý**, chưa tự gửi tin (chưa có provider ZBS/SMS chính thức, Zalo đang OFF).

### C. Tiện ích hệ thống "xịn hơn" (ưu tiên 3 — chọn dần)

So sánh với CRM/ERP/LIMS và đánh giá độ phù hợp cho Lab của bạn:

| Tiện ích | Học từ | Giá trị cho Lab | Độ khó | Đề xuất |
|---|---|---|---|---|
| **Audit log tập trung** (ai/làm gì/lúc nào/trước-sau) + trang xem | ERP, LIMS (ISO 17025 cần truy vết) | Rất cao — hồ sơ chất lượng, tranh chấp khách | TB | **Làm** (nên làm trước/cùng B vì merge khách cần audit) |
| **Tìm kiếm toàn cục** (Ctrl+K: mã mẫu, số phiếu, SĐT, tên khách) | CRM | Cao, dùng hàng ngày | Thấp–TB | **Làm** |
| **Báo cáo định kỳ xuất Excel/PDF** (tuần/tháng: số mẫu, doanh thu-sản lượng, TAT) | ERP | Cao — họp/báo cáo lãnh đạo | TB | **Làm** |
| **Bảng giá/báo giá/doanh thu** theo chỉ tiêu, hóa đơn, công nợ | ERP, CRM | Rất cao nếu LAB thu phí qua hệ thống | **Cao, đụng nghiệp vụ tiền** | Cần LAB xác nhận có làm không (Q3). Chưa lập chi tiết |
| **Quản lý thiết bị & hiệu chuẩn** (lịch hiệu chuẩn, nhắc hạn) | LIMS | Cao với ISO 17025; bảng `testing_equipment` đã có | TB | Đề xuất giai đoạn sau |
| **Quản lý hóa chất/vật tư tồn kho + hạn dùng** | ERP/LIMS | TB–Cao; `testing_reagents` đã có | TB | Giai đoạn sau |
| **Cảnh báo/nhắc việc tự động** (quá hạn, sắp tới hạn hiệu chuẩn) | CRM/LIMS | Cao | TB | Gộp vào A (khối cần xử lý) |
| **Trang tra cứu khách + cổng khách hàng đăng nhập xem lịch sử** | CRM portal | TB; trang `/tracuu/` đã có | Cao (bảo mật/định danh) | Chưa — chờ tên miền + chính sách |
| **Gửi kết quả/nhắc hẹn tự động qua Zalo ZNS/SMS/Email** | CRM | Cao | Đã có khung Zalo, chờ OA/template | Chờ provider; không mô tả là đã chạy |
| **Backup/giám sát sức khỏe hệ thống** (DB, Redis, dung lượng ổ, hàng đợi export) | Ops | Cao, tránh mất dữ liệu | Thấp–TB | **Làm** một trang "Sức khỏe hệ thống" |
| **Phân quyền chi tiết/2FA đăng nhập** | ERP | TB–Cao (bảo mật) | TB | Đề xuất: 2FA cho ADMIN; **không thêm role mới** |
| **Dark mode, dashboard tùy biến widget** | SaaS | Thấp | TB | Không ưu tiên |

## 4. Quyết định đề xuất (cần bạn xác nhận ở §8)

1. Không thêm thư viện UI/chart; dùng SVG/CSS thuần như hiện có.
2. Không thêm role; mọi tính năng mới của §3A–B chỉ cho `ADMIN`.
3. Dữ liệu tổng hợp **tính ở backend/DB** (GROUP BY), không tải về rồi đếm ở FE.
4. Định danh khách vẫn là SĐT chuẩn hóa, bổ sung merge thủ công thay vì tự đoán (tránh gộp nhầm).
5. Phân loại khách là **tính động**, không lưu cột trạng thái (tránh lệch dữ liệu).

## 5. Thiết kế kỹ thuật

### 5.1 Backend (package `admin`, mở rộng `customer` mới hoặc đặt trong `intake`)

Endpoint đề xuất (đều `ADMIN`, theo `docs/API_CONVENTIONS.md` — Codex đối chiếu lại định dạng trước khi code):

| Endpoint | Mục đích |
|---|---|
| `GET /api/v1/admin/analytics/overview?from&to&compare=true` | KPI + kỳ trước |
| `GET /api/v1/admin/analytics/trend?from&to&granularity=day\|week\|month` | Chuỗi thời gian |
| `GET /api/v1/admin/analytics/funnel?from&to` | Phễu phiếu/mẫu |
| `GET /api/v1/admin/analytics/breakdown?by=productType\|parameter\|staff\|growingArea` | Cơ cấu |
| `GET /api/v1/admin/analytics/turnaround?from&to` | TAT trung vị/P90 theo chặng |
| `GET /api/v1/admin/analytics/attention` | Danh sách cần xử lý |
| `GET /api/v1/admin/customers?q&segment&sort&page` | Danh sách khách |
| `GET /api/v1/admin/customers/{id}` · `/timeline` | Chi tiết, timeline |
| `PATCH /api/v1/admin/customers/{id}` | Sửa hồ sơ (có lý do, audit) |
| `POST /api/v1/admin/customers/{id}/merge` (`{targetId, reason}`) + `?dryRun=true` | Gộp khách |
| `GET/POST /api/v1/admin/customers/{id}/notes` | Ghi chú/thẻ |
| `GET /api/v1/admin/customers/export.csv` | Xuất CSV (giới hạn, audit) |
| `GET /api/v1/admin/audit-logs?actor&entity&from&to&page` | Audit log |
| `GET /api/v1/search?q=` | Tìm kiếm toàn cục (theo quyền role) |

Hiệu năng: truy vấn aggregate dùng index sẵn có `idx_receipts_received_at`, `idx_receipts_customer`; **cần kiểm `EXPLAIN`** cho GROUP BY theo kỳ và thêm index phù hợp nếu chậm. Nếu cần, cache ngắn (Redis, 30–60 giây) cho overview — không bắt buộc ở bản đầu.

### 5.2 Migration (kiểm số version thực tế trước khi đặt — hiện cao nhất là **V28**, tức dự kiến V29+)

- `customer_notes(id, customer_id, author_user_id, note, tag, created_at)`.
- `customer_merge_log(id, source_customer_id, target_customer_id, moved_receipts, actor_user_id, reason, created_at)`.
- `audit_logs(id, actor_user_id, action, entity_type, entity_id, before_json, after_json, ip, created_at)` + index `(entity_type, entity_id)`, `(created_at)`. Không lưu secret/token/mật khẩu.
- Có thể thêm cột chuẩn hóa `customers.phone_normalized` + unique nếu dữ liệu hiện tại cho phép — **phải kiểm tra trùng trong DB vận hành trước** (xem Rủi ro).
- Hibernate đang `validate` → entity và migration phải khớp. **Không sửa DB vận hành trực tiếp**; test migration trên bản sao (`cfc_lab_2026-11-27.sql` ở gốc repo là bản dump — dùng cho test cô lập, không lộ dữ liệu khách thật trong log/commit).

### 5.3 Frontend (`source/fe/src/features/`)

- `admin/` mở rộng `DashboardPage` (tách component: `PeriodFilter`, `KpiCard` có delta, `TrendChart`, `FunnelChart`, `BreakdownBars`, `AttentionList`).
- Feature mới `customers/` (`CustomersPage`, `CustomerDetailPage`, `api/customersApi.js`, `customerModel.js` + test).
- Route mới trong `App.jsx` dưới nhóm ADMIN; thêm mục sidebar "Khách hàng", "Báo cáo", "Nhật ký hệ thống".
- Dùng lại `WorkspaceStats`, `PageLoader`, style trong `workspace.css`; theo `source/fe/AGENTS.md`.

## 6. Thứ tự triển khai (giao Codex theo từng PR nhỏ)

| Bước | Nội dung | Phụ thuộc | Có migration |
|---|---|---|---|
| A1 | Overview + trend + so sánh kỳ + bộ lọc kỳ (BE + FE) | — | Không |
| A2 | Phễu, breakdown, workload | A1 | Không |
| A3 | TAT + danh sách "cần xử lý" + drill-down | A1, Q4 | Không |
| C1 | Audit log tập trung + trang xem | — | Có |
| B1 | Danh sách khách + phân loại + chi tiết/timeline (chỉ đọc) | A1 (dùng chung biểu đồ) | Không |
| B2 | Sửa hồ sơ, ghi chú/thẻ, **merge khách** | B1, C1 | Có |
| C2 | Tìm kiếm toàn cục | B1 | Không (có thể thêm index) |
| C3 | Báo cáo xuất Excel/PDF định kỳ | A1–A3 | Không |
| C4 | Trang sức khỏe hệ thống | — | Không |
| D | Thiết bị/hiệu chuẩn, vật tư, báo giá/doanh thu | Q3, Q5 | Có — plan riêng sau |

Đề xuất làm **A1 → A2 → B1** đầu tiên: nhìn thấy kết quả nhanh, không migration, đúng điều bạn yêu cầu (dashboard xịn + xem khách cũ).

## 7. Rủi ro

- **Dữ liệu cá nhân khách hàng** (tên, SĐT, địa chỉ): chỉ ADMIN xem; export CSV có giới hạn + audit; không đưa dữ liệu khách thật vào plan/log/commit/ảnh chụp.
- **Trùng/sai SĐT** làm sai thống kê khách cũ (một người nhiều số, nhiều người một số, định dạng `0912…` vs `+84912…`). Cần chuẩn hóa và báo cáo trùng trước khi thêm unique.
- **Merge khách không hoàn tác dễ** → bắt buộc dry-run, lý do, giao dịch đơn, `customer_merge_log` đủ để khôi phục thủ công.
- **Hiệu năng** aggregate khi dữ liệu tăng nhiều năm (kế hoạch lưu hồ sơ 10 năm): kiểm `EXPLAIN`, index, giới hạn khoảng thời gian tối đa mỗi truy vấn.
- **Phân biệt phiếu và mẫu**: mọi KPI phải ghi rõ đơn vị; không cộng lẫn.
- **Múi giờ**: ranh giới ngày/tháng tính theo `Asia/Ho_Chi_Minh`, trong khi một số cột lưu UTC (`DATETIME(6)` ở V20/V21) — phải chuyển đổi đúng ở query.
- Chưa có SLA/quy tắc phân loại khách do LAB chốt → mọi ngưỡng là tham số cấu hình, **không** coi là quy định.

## 7A. Quyết định đã chốt sau phản hồi 27/11/2026

| # | Trả lời của chủ dự án | Hệ quả cho plan |
|---|---|---|
| Q1 | Có cả công ty và nhiều người liên hệ | **Thêm cấp "Tổ chức/Công ty"** trên "Người liên hệ" (xem §7B). Đây là thay đổi lớn nhất so với bản đầu |
| Q2 | Chưa biết ngưỡng | Dùng **ngưỡng mặc định cấu hình được** (§7B), hiển thị phân bố thực tế để Admin tự chỉnh sau vài tháng |
| Q3 | Có thu phí nhưng **chưa báo giá** | Chưa làm báo giá/công nợ. Chỉ chừa chỗ: chỉ tiêu (`test_methods`) có thể thêm đơn giá ở giai đoạn D. Dashboard **không** hiển thị doanh thu |
| Q4 | Báo cáo lãnh đạo / ISO 17025 / chăm sóc khách đều được | Giữ thứ tự cũ A → B → C; audit log + báo cáo phục vụ cả ISO lẫn lãnh đạo |
| Q5 | Kết nối **email trước**, rồi Zalo, Google | Thêm giai đoạn **E (Email)** (§7C); Zalo đã có khung, Google Sheets để sau |
| Q6 | Cho thêm thư viện chart | Cho phép **một** thư viện chart nhẹ (đề xuất Recharts hoặc Chart.js — Codex chọn theo kích thước bundle, lazy-load trang dashboard). Vẫn không thêm UI library khác |

## 7B. Mô hình khách hàng đề xuất (do Q1)

Hiện trạng: `customers` phẳng, gộp theo SĐT; `receipts.customer_id`. Một công ty có nhiều người đến → bị tách thành nhiều "khách", đếm "khách cũ" sai.

Đề xuất tối thiểu, **tương thích ngược** (không phá `receipts.customer_id`):

- Thêm bảng `organizations(id, name, tax_code NULL, address, created_at, updated_at)`.
- Thêm `customers.organization_id NULL` (FK). `customers` = **người liên hệ**; khách cá nhân để `NULL`.
- Thống kê "khách cũ/quay lại" tính theo **tổ chức nếu có, ngược lại theo người liên hệ**.
- Admin có màn hình gán/gộp: gom nhiều người liên hệ vào một tổ chức (có gợi ý theo tên giống nhau, **không tự gộp**).
- Form Phiếu 3: giữ nguyên; thêm (bước sau, tùy chọn) ô chọn/nhập tên tổ chức để Tiếp nhận không phải nhờ Admin gom về sau. **Cần xác nhận** trước khi đụng form Phiếu 3.
- Mã số thuế: chỉ lưu nếu LAB muốn; **cần xác nhận**.

Ngưỡng phân loại mặc định (cấu hình, chưa phải quy định LAB):
- *Mới*: có đúng 1 phiếu trong 12 tháng gần nhất.
- *Quay lại*: ≥ 2 phiếu trong 12 tháng.
- *Thân thiết*: ≥ 6 phiếu trong 12 tháng (≈ mỗi 2 tháng 1 lần).
- *Ngủ đông*: từng gửi mẫu nhưng không có phiếu mới > 180 ngày. Vì mùa vụ nông sản (sầu riêng, trái cây…) khách có thể chỉ gửi theo vụ, nên **so sánh cùng kỳ năm trước** trước khi coi là "mất khách".
- Bản đầu hiển thị thêm **phân bố thực tế** (histogram số phiếu/khách) để chọn ngưỡng dựa trên dữ liệu.

## 7C. Giai đoạn E — Kết nối Email (nội dung chi tiết)

Hiện trạng: `customers.email` đã có và form Phiếu 3 đã nhập/validate email (tùy chọn); **chưa có hạ tầng gửi email** (không có spring-mail, chưa có `JavaMailSender`). Đã có khung thông báo khách: bảng outbox + worker + giao diện `CustomerMessageProvider` (hiện chỉ nhận `phone/templateId`, thiết kế cho Zalo) và trang Admin theo dõi (`/admin/customer-notifications`).

Đề xuất:
1. Mở rộng outbox sang **kênh** (`ZALO`/`EMAIL`) thay vì dựng hệ thống thứ hai — tái dùng retry, audit, trạng thái, pause. Cần migration và sửa `CustomerMessageProvider` (hoặc thêm `CustomerEmailProvider` song song để không phá Zalo đang OFF).
2. SMTP qua cấu hình môi trường (`.env`/secret, **không commit**): host, port, user, mật khẩu ứng dụng, người gửi. Gợi ý dùng tài khoản email tên miền của LAB hoặc dịch vụ như Amazon SES/Brevo; Gmail cá nhân bị giới hạn gửi và dễ vào spam — **cần LAB chọn nhà cung cấp**.
3. Sự kiện gửi (mỗi loại **cần xác nhận** nội dung): đã tiếp nhận mẫu; KQKN đã phát hành (kèm link tra cứu `/tracuu/` + mã, **không đính kèm PDF** ở bản đầu để tránh rò dữ liệu); nhắc/cập nhật tiến độ (tùy chọn).
4. Khách chưa có email → bỏ qua, hiển thị rõ "chưa có email" trên hồ sơ khách. Có sự đồng ý (consent) theo cơ chế đã có ở V20 (`receipt_notification_consents`) — **cần xác nhận** cách hỏi đồng ý.
5. Mẫu email HTML + text thuần, tiếng Việt, thông tin LAB; có biến mẫu và xem trước. Gửi thử cho Admin trước khi bật. Mặc định **OFF**, giống Zalo.
6. Chống lạm dụng: giới hạn tốc độ, cap theo ngày, chống trùng theo sự kiện, bounce/complaint ghi nhận nếu provider hỗ trợ. Có link từ chối nhận.
7. Báo cáo định kỳ (C3) có thể gửi email nội bộ cho lãnh đạo (danh sách người nhận do Admin cấu hình).

Bước triển khai thêm vào §6: **E1** hạ tầng SMTP + outbox kênh email + trang cấu hình/gửi thử; **E2** gửi khi phát hành KQKN; **E3** gửi báo cáo định kỳ cho lãnh đạo. Phụ thuộc: C1 (audit) nên xong trước E1; chọn nhà cung cấp SMTP.

Google (Sheets/Drive): để sau Email và Zalo. Hướng hợp lý: xuất báo cáo CSV/Excel trước (C3), đồng bộ Google Sheets chỉ khi LAB thật sự cần — tốn OAuth, bảo mật dữ liệu khách.

## 8. Câu hỏi mở (cần LAB/bạn trả lời)

> Q1–Q6 đã được trả lời ở §7A. Còn lại các câu mới: (a) Có chuyển "công ty" thành cấp riêng và có lưu mã số thuế không? (b) Chọn nhà cung cấp SMTP nào? (c) Khách có cần đồng ý nhận email bằng cách nào (tích trên Phiếu 3 hay đã ngầm định)? (d) Muốn gửi email loại nào trước (đã tiếp nhận / có kết quả)? Danh sách gốc bên dưới giữ để tham khảo.


1. Khách hàng LAB là cá nhân hay công ty/cơ sở đóng gói? Một công ty có nhiều người liên hệ khác SĐT không? (Quyết định có cần thêm cấp "Tổ chức" không.)
2. Ngưỡng phân loại: bao nhiêu phiếu/năm là "thân thiết"; bao nhiêu ngày không gửi là "ngủ đông"?
3. LAB có thu phí/báo giá/công nợ qua hệ thống không? Nếu có, đây là module riêng, lớn.
4. SLA cho từng chặng (nhận → chuyển KCS, KCS xử lý, thử nghiệm, phát hành) là bao nhiêu ngày?
5. Ưu tiên của bạn: báo cáo cho lãnh đạo, hay quản lý chất lượng ISO 17025 (thiết bị/hiệu chuẩn/audit), hay chăm sóc khách?
6. Có chấp nhận thêm thư viện chart (ví dụ Recharts/Chart.js) nếu SVG tự viết quá tốn công không?
7. "Connect tới được" — bạn muốn kết nối tới đâu: Zalo OA, email, Google Sheets/Excel, phần mềm kế toán, hay API cho hệ thống khác? (Hiện mình hiểu là liên kết dữ liệu khách ↔ phiếu ↔ kết quả ↔ thông báo trong chính hệ thống.)

## 9. Tiêu chí nghiệm thu (chung)

- Mọi số trên dashboard khớp truy vấn SQL độc lập cùng khoảng ngày/filter; có test so khớp.
- Dashboard không còn số "toàn thời gian" bị trình bày như số trong kỳ.
- Click KPI/biểu đồ dẫn tới danh sách lọc đúng.
- Danh sách khách phân trang server-side; phân loại khách đúng theo dữ liệu mẫu test; merge có dry-run và log, không mất phiếu nào (tổng phiếu trước = sau).
- Role `RECEPTIONIST`/`TECHNICIAN` gọi API admin mới nhận 403.
- Không rò SĐT/email trong log; export có audit.
- Responsive 320px–2560px; có loading/lỗi/rỗng.

## 10. Test cần yêu cầu Codex chạy

- Backend: `mvn test` (Java 21) trong `source/be` — thêm test repository/service cho aggregate (kỳ rỗng, ranh giới ngày theo giờ VN, so sánh kỳ trước), RBAC 403, merge khách (dry-run, giao dịch, trùng SĐT), migration Flyway mới trên DB trống và trên bản sao DB hiện có. Có thể chạy cô lập bằng `bash scripts/run-be-tests-docker.sh`.
- Frontend: build + test model (`customerModel.test.js`, hàm tính delta/định dạng kỳ); kiểm Chrome thật ở 320/768/1280/2560 px với dữ liệu seed (không dùng dữ liệu khách thật).
- Hồi quy: trang Admin hiện có (users, dossiers, test-methods, customer-notifications) vẫn chạy.

---

## 11. Giao việc chi tiết cho Codex — A1 và A2 (cập nhật 28/11/2026)

> Trạng thái: **đề xuất, chưa code, chưa kiểm thử**. Đối chiếu lại source trước khi sửa; tên file dưới đây lấy từ hiện trạng đã đọc.

### 11.1 Lỗi/giới hạn hiện có cần sửa kèm (có bằng chứng trong code)

`AdminDashboardService.summarize` chỉ lọc theo kỳ cho `receiptsInPeriod` và `samplesInPeriod`. Các số còn lại (`transfers*`, `testing*`, `result*`) là `countByStatus` **toàn thời gian** nhưng giao diện ghi chung "Trong 7 ngày gần nhất / Toàn hệ thống". Bản mới phải tách hai nhóm và ghi nhãn đúng:
- **Theo kỳ** (đếm theo mốc thời gian của sự kiện): số phiếu/mẫu tiếp nhận, phiếu chuyển tạo trong kỳ, KQKN phát hành trong kỳ.
- **Tồn hiện tại** (snapshot, không phụ thuộc kỳ): đang chờ KCS nhận, đang thử nghiệm, chờ phát hành.

### 11.2 A1 — Overview, kỳ, so sánh kỳ trước, xu hướng

**Backend** (package `com.example.app.admin`, giữ nguyên `GET /api/v1/admin/dashboard` để không phá FE cũ; thêm endpoint mới, FE chuyển dần):

| Endpoint | Trả về |
|---|---|
| `GET /api/v1/admin/analytics/overview?from&to` | `{period:{from,to}, previous:{from,to}, current:{receipts,samples,customers,newCustomers,returningCustomers,transfersCreated,resultsPublished}, previousValues:{...cùng khóa}, backlog:{awaitingTransfer,awaitingKcs,testingInProgress,awaitingRelease}}` |
| `GET /api/v1/admin/analytics/trend?from&to&granularity=day\|week\|month` | `{points:[{bucketStart,receipts,samples,resultsPublished}]}` — **lấp bucket rỗng bằng 0** |

Quy tắc:
- `previous` = khoảng liền trước có **cùng độ dài** (kỳ N ngày → N ngày trước đó). Chia cho 0 → trả `null` cho % thay đổi, FE hiện "—".
- Giới hạn khoảng tối đa (đề xuất 366 ngày) → 400 nếu vượt; `from > to` → 400 (giữ thông điệp hiện có).
- Ranh giới ngày theo `Asia/Ho_Chi_Minh` (xem `IntakeDashboardService.sampleRange`); bucket tuần bắt đầu thứ Hai.
- "Khách": đếm `DISTINCT customer_id` của phiếu trong kỳ. "Khách mới" = chưa có phiếu nào **trước** `from`; "khách quay lại" = đã có phiếu trước `from`. (Chưa tính cấp công ty — chờ §7B.)
- Dùng GROUP BY/JPQL hoặc native query; **không** tải receipt về đếm. Kiểm `EXPLAIN` với `idx_receipts_received_at`, `idx_receipts_customer`. Chỉ thêm index nếu thật sự cần (migration kiểm version thực tế, hiện cao nhất V28).
- `@PreAuthorize("hasRole('ADMIN')")`; DTO là `record`, đặt trong `admin/dto`.
- File dự kiến: `AdminAnalyticsController`, `AdminAnalyticsService`, `AdminAnalyticsRepository` (native/JPQL), `dto/AnalyticsOverviewResponse`, `dto/AnalyticsTrendResponse`.

**Frontend** (`source/fe/src/features/admin/`):
- `components/PeriodFilter.jsx` (Hôm nay / 7 ngày / 30 ngày / Tháng này / Năm nay / Tùy chọn; ngày theo giờ VN, không dùng `toISOString()` cắt chuỗi vì lệch múi giờ).
- `components/KpiCard.jsx` (giá trị, nhãn đơn vị **phiếu**/**mẫu**/**khách**, delta ▲▼ so kỳ trước, bấm → danh sách lọc cùng kỳ).
- `components/TrendChart.jsx` (đã được duyệt dùng thư viện chart: ưu tiên bản nhẹ, `React.lazy` cho trang Dashboard, ghi kích thước bundle trước/sau; nếu quá nặng → quay lại SVG thuần).
- `api/adminAnalyticsApi.js`, `adminAnalyticsModel.js` (+ `adminAnalyticsModel.test.js`: tính delta, chia 0, định dạng kỳ, mapping preset → from/to theo giờ VN).
- Sửa `pages/DashboardPage.jsx`: tách khối "Trong kỳ" và "Tồn hiện tại", sửa nhãn sai nêu ở 11.1. Giữ loading/lỗi/rỗng, responsive 320px+.

### 11.3 A2 — Phễu, cơ cấu, workload

| Endpoint | Trả về |
|---|---|
| `GET /api/v1/admin/analytics/funnel?from&to` | Các bước theo **phiếu** và **mẫu** riêng: tiếp nhận → đã chuyển → KCS đã nhận → đã nộp thử nghiệm → đã duyệt → đã phát hành. Cohort theo ngày tiếp nhận trong kỳ (không phải số sự kiện xảy ra trong kỳ) — ghi rõ trong UI |
| `GET /api/v1/admin/analytics/breakdown?from&to&by=productType\|staff` | `[{key,label,receipts,samples}]`, top N + "Khác" |

- `by=parameter` (chỉ tiêu) **hoãn**: `requested_parameters` là TEXT tự do, cần kiểm cách nhập thực tế/`test_methods` trước, **cần xác nhận**. Không đoán chuỗi.
- `by=staff`: người tiếp nhận (`received_by_user_id`); workload KCS/Phiếu 5 đối chiếu theo cột phân công ở V28 (kiểm schema thực tế trước).
- FE: `FunnelChart.jsx`, `BreakdownBars.jsx`; click vào cột → danh sách lọc tương ứng nếu bộ lọc danh sách đã hỗ trợ (kiểm `ReceiptSearchCriteria`); chưa hỗ trợ thì ẩn click, **không** làm link giả.

### 11.4 Test yêu cầu Codex chạy cho A1/A2

- Backend (`mvn test`; hoặc `bash scripts/run-be-tests-docker.sh`): kỳ rỗng; phiếu đúng 23:59:59/00:00:00 giờ VN rơi đúng bucket; so sánh kỳ trước (có/không dữ liệu); `from > to`; vượt giới hạn; 403 cho RECEPTIONIST/TECHNICIAN; số liệu khớp truy vấn SQL độc lập trên cùng seed; khách mới vs quay lại.
- Frontend: `npm run build`; test model; Chrome thật 390×844, 1024×768, 1440×900, 2560×952, không tràn ngang; loading/lỗi/rỗng; dữ liệu seed, **không dùng dữ liệu khách thật** (dump `cfc_lab_2026-11-27.sql` chỉ có 3 khách/6 phiếu, ít để xem biểu đồ — cần script seed dữ liệu mô phỏng cho môi trường test).
- Hồi quy: `/admin/users`, `/admin/dossiers`, `/admin/customer-notifications`, dashboard Receptionist/KCS không đổi hành vi.
- Sau khi xong Codex cập nhật `docs/ai/CONTEXT.md`, `docs/ai/MODULE_MAP.md`, `docs/API_CONVENTIONS.md`.

### 11.5 Tiêu chí nghiệm thu A1/A2

1. Mọi số khớp SQL độc lập cùng khoảng ngày; nhãn "trong kỳ" và "tồn hiện tại" đúng bản chất.
2. Delta kỳ trước đúng, kể cả kỳ trước = 0.
3. Đổi preset không giữ dữ liệu cũ (không race: hủy/bỏ qua phản hồi cũ khi đổi kỳ nhanh).
4. Role khác ADMIN không gọi được; không còn hard-code số liệu.
5. Bundle trang khác không tăng do thư viện chart (lazy).
