# Role plan — Người nhận mẫu KCS

**Cập nhật:** 16/11/2026

**Trạng thái:** Đã khảo sát source và biểu mẫu; chờ khóa quyết định role trước khi code

**Role runtime hiện tại:** `TECHNICIAN` · **Đích nhận mẫu duy nhất:** `KCS`

> Tên nghiệp vụ trong plan này là **Người nhận mẫu KCS**. Source hiện dùng `TECHNICIAN` cho cả nhận mẫu và thử nghiệm. Không đổi enum hoặc migration user cho đến khi xác nhận hai trách nhiệm này là một người hay hai role riêng.

## 1. Ranh giới nghiệp vụ từ biểu mẫu

Chuỗi trách nhiệm được chốt theo tài liệu gốc:

1. `RECEPTIONIST` lập Phiếu 3, ghi hồ sơ nguồn, tình trạng, ảnh và kết luận tiếp nhận.
2. `RECEPTIONIST` lập/gửi Phiếu 4 với danh sách mẫu, khối lượng, số lượng, tình trạng, chỉ tiêu và phương pháp thử.
3. Người nhận mẫu KCS đối chiếu mẫu vật, nhãn, Phiếu 4 và bằng chứng ảnh; sau đó nhận hoặc từ chối.
4. Chỉ mẫu đã nhận mới đi tiếp sang Phiếu 5 cho người phân tích/kiểm nghiệm.
5. Phiếu kết quả được tạo ở giai đoạn phát hành sau Phiếu 5, không thuộc workspace nhận mẫu.

Role nhận mẫu không được sửa Phiếu 3, không lập Phiếu 4, không xem thông tin liên hệ khách hàng và không mặc định được thao tác thử nghiệm.

## 2. Hiện trạng source và khoảng trống

- `/admin/transfers` đã cho `TECHNICIAN` xem Phiếu 4 từ `PENDING` trở đi và nhận/từ chối mẫu.
- Navbar hiện vẫn cho `TECHNICIAN` thấy cả `Chuyển mẫu`, `Thử nghiệm` và `Thông báo`; chưa đúng yêu cầu “chỉ thấy nhận mẫu”.
- Landing hiện chuyển thẳng tới `/admin/transfers`; chưa có dashboard ngày/tuần/tháng.
- `GET /api/v1/transfers` chỉ lấy tối đa 50 bản ghi rồi lọc role trong service; chưa phân trang, search hoặc lọc thời gian ở database.
- `TransferResponse` hiện trả `customerName`, trong khi người nhận mẫu không cần danh tính khách hàng.
- Phiếu chuyển chưa trả danh sách ảnh/hồ sơ. `TECHNICIAN` cũng bị chặn khỏi `/api/v1/receipts/**`, nên hiện không thể xem ảnh Phiếu 3 một cách đúng quyền.
- Quyết định nhận mẫu mới kiểm tra trạng thái trước khi cập nhật, chưa có optimistic/pessimistic locking để bảo đảm hai người không thể quyết định đồng thời.
- Index hiện chỉ có `sample_transfers(status)`; dashboard theo `submittedAt/receivedAt` cần index phù hợp sau khi đo query thật.

## 3. Quyết định role cần khóa

### Phương án khuyến nghị

Tạo role riêng `SAMPLE_RECEIVER` (tên hiển thị: **Người nhận mẫu KCS**) và giữ `TECHNICIAN` cho người phân tích/Phiếu 5.

- `SAMPLE_RECEIVER`: dashboard nhận mẫu, hàng đợi Phiếu 4, xem bằng chứng, nhận/từ chối, thông báo, hồ sơ cá nhân.
- `TECHNICIAN`: mẫu đã nhận, Phiếu 5, thiết bị/hóa chất/raw data; không quyết định bàn giao nếu không đồng thời được cấp quyền nhận mẫu.
- Nếu một nhân sự làm cả hai việc, model một role/user hiện tại không đủ; cần xác nhận dùng role gộp tạm thời hay chuyển sang user có nhiều role.

### Phương án ít thay đổi hơn

Giữ `TECHNICIAN` là người nhận mẫu và ẩn toàn bộ menu/thao tác thử nghiệm. Phương án này nhanh hơn nhưng về sau phải thêm role mới khi triển khai người phân tích và separation of duties.

## 4. Navbar và route của người nhận mẫu

Navbar chỉ chứa:

```text
Không gian nhận mẫu
├── Tổng quan                 /admin/receiving
├── Nhận mẫu                  /admin/receiving/transfers
├── Thông báo                 /admin/notifications
└── Thông tin cá nhân         /admin/profile
```

- Không hiện `Nhân viên`, `Tiếp nhận mẫu`, `Tạo Phiếu 3`, `Lập Phiếu 4`, `Thử nghiệm` hoặc `Cài đặt chung`.
- `/admin/transfers` được giữ redirect tương thích trong ít nhất một release.
- Landing sau login là `/admin/receiving`.
- Header hiển thị trạng thái realtime, chuông thông báo và tên role **Người nhận mẫu KCS**.

## 5. Dashboard ngày/tuần/tháng

### Bộ lọc thời gian

- Preset: `Hôm nay`, `Tuần này`, `Tháng này`, `Tùy chọn`.
- Tính theo `Asia/Ho_Chi_Minh`; backend dùng khoảng nửa mở `[from, to + 1 ngày)`.
- Hai mốc được tính riêng: **đã chuyển lên** theo `submittedAt`, **đã nhận** theo `receivedAt`.

### Chỉ số chính

- Phiếu mới được chuyển lên trong kỳ.
- Tổng số mẫu trong các phiếu được chuyển lên.
- Đang chờ xác nhận tại thời điểm xem.
- Đã nhận trong kỳ.
- Đã từ chối trong kỳ.
- Thời gian xử lý trung bình từ `submittedAt` đến `receivedAt`.
- Phiếu chờ quá SLA; SLA phải là cấu hình, không hard-code trong UI.

### Bố cục

```text
┌─ Tổng quan nhận mẫu ───────── Hôm nay | Tuần | Tháng | Tùy chọn ─┐
├─ Đã chuyển lên 12 ─ Tổng mẫu 18 ─ Chờ nhận 3 ─ Đã nhận 8 ──────┤
├──────────────────────────────────────┬───────────────────────────┤
│ Hàng đợi cần nhận                    │ Nhịp nhận mẫu theo ngày   │
│ ưu tiên phiếu chờ lâu nhất           │ submitted / accepted      │
│ mã phiếu · số mẫu · thời gian · ảnh  │ không dùng dữ liệu giả     │
├──────────────────────────────────────┴───────────────────────────┤
│ Lịch sử gần đây: đã nhận / từ chối, actor và thời điểm           │
└──────────────────────────────────────────────────────────────────┘
```

Điểm nhấn là **hàng đợi bàn giao đang chờ**, không phải một cụm card SaaS giống nhau. Dashboard phải trả lời ngay: phiếu nào đến trước, có bao nhiêu mẫu, đã có ảnh chưa và cần xử lý trong bao lâu.

### Hướng thị giác

- Giữ hệ CFC hiện có: Forest `#09261B`, Leaf `#2F7D4D`, Paper `#F4F8F3`, Mint `#E6F6EA`, Amber `#B9781D`, Reject `#A44848`.
- `Space Grotesk` dùng cho số liệu/mã phiếu; `DM Sans` dùng cho nội dung và control.
- Trạng thái dùng màu kèm chữ/icon, không truyền đạt chỉ bằng màu.
- Ảnh là bằng chứng nghiệp vụ: thumbnail rõ, tỷ lệ ổn định, không dùng ảnh làm trang trí.

## 6. Hàng đợi nhận mẫu

- Tab `Chờ nhận`, `Đã nhận`, `Đã từ chối`; mặc định `Chờ nhận`.
- Search theo mã Phiếu 4, mã Phiếu 3 và mã mẫu; không search phone/email/customer.
- Filter ngày chuyển, hạn trả kết quả, người giao nội bộ và trạng thái có/thiếu ảnh.
- Sort mặc định `PENDING`: `submittedAt,asc` để phiếu chờ lâu nhất lên đầu.
- Phân trang server-side 20/50; frontend không tải 50 bản ghi rồi tự lọc.
- Mỗi dòng/card hiển thị mã Phiếu 4, số mẫu, mã mẫu rút gọn, người bàn giao nội bộ, giờ gửi, hạn trả, số ảnh và trạng thái SLA.
- WebSocket chỉ báo có thay đổi; REST/database vẫn là nguồn sự thật.

## 7. Chi tiết Phiếu 4 và bằng chứng ảnh

### Dữ liệu được xem

- Mã Phiếu 4, mã Phiếu 3, ngày chuyển, hạn trả, người giao nội bộ.
- Danh sách mẫu: mã, tên, khối lượng/số lượng, tình trạng, chỉ tiêu, phương pháp thử.
- Ghi chú thay đổi, yêu cầu lưu mẫu và chuỗi bàn giao.
- Toàn bộ file/ảnh liên quan đến việc đối chiếu: Phiếu 3, ảnh mẫu, ảnh lô hàng, bao bì/niêm phong và ảnh bất thường.

### Dữ liệu không trả cho role nhận mẫu

- Tên khách hàng, phone, email, địa chỉ.
- Mapping mã mù, secret/provider notification, lịch sử quản trị user.
- Hồ sơ nguồn đầy đủ nếu không cần cho việc đối chiếu bàn giao.

### Trải nghiệm ảnh

- Gallery theo nhóm `Phiếu 3`, `Ảnh mẫu`, `Ảnh lô hàng`, `Khác`.
- Thumbnail có số lượng, thời điểm chụp/tải và người tải.
- Click mở preview lớn; ảnh trước/sau, zoom và tải file nếu được phép.
- File PDF/DOCX hiện icon, tên file, dung lượng và nút mở/tải; không giả preview ảnh.
- Empty state ghi rõ “Phiếu chưa có ảnh đối chiếu”; có thể chặn nhận hoặc chỉ cảnh báo tùy quyết định nghiệp vụ.

## 8. API/projection đề xuất

Không mở quyền `/api/v1/receipts/**` đầy đủ cho người nhận mẫu. Tạo projection riêng thuộc feature nhận/chuyển mẫu:

| Method | Endpoint | Trách nhiệm |
|---|---|---|
| GET | `/api/v1/receiving/dashboard?from&to` | KPI, trend và hàng đợi gần nhất |
| GET | `/api/v1/receiving/transfers?page&size&status&q&from&to&sort` | Danh sách phân trang/filter tại DB |
| GET | `/api/v1/receiving/transfers/{id}` | Chi tiết kỹ thuật đã loại PII khách hàng |
| GET | `/api/v1/receiving/transfers/{id}/evidence` | Metadata ảnh/file thuộc đúng receipt của transfer |
| GET | `/api/v1/receiving/transfers/{id}/evidence/{attachmentId}/content` | Stream file sau khi kiểm tra quan hệ và quyền |
| POST | `/api/v1/receiving/transfers/{id}/decision` | Nhận/từ chối idempotent, từ chối bắt buộc lý do |

- Dùng DTO riêng, không tái sử dụng `ReceiptResponse` hoặc trả entity JPA.
- Dashboard dùng aggregate query riêng; không tính KPI từ page 20 dòng.
- Content endpoint phải kiểm tra attachment thuộc receipt của transfer mà viewer được xem; không nhận `attachmentId` rồi tải trực tiếp.
- Response list không trả `customerName`; response detail chỉ trả dữ liệu kỹ thuật cần thiết.
- Quyết định đồng thời phải trả `409 Conflict` cho request thua bằng `@Version` hoặc row lock trong transaction.

## 9. Dữ liệu và index dự kiến

- Chưa cần bảng dashboard; số liệu lấy từ `sample_transfers` và `sample_transfer_items`.
- Migration Flyway kế tiếp phải bắt đầu từ version chưa dùng sau V4.
- Dự kiến index `sample_transfers(status, submitted_at)` và `sample_transfers(received_at)`; chỉ chốt sau `EXPLAIN` với query thật.
- Nếu dùng optimistic locking, thêm cột `version` và migration/backfill phù hợp.
- Không sao chép attachment sang bảng transfer; dùng quan hệ transfer → receipt → receipt_attachment và kiểm tra quyền qua service.

## 10. Thứ tự triển khai

### K0 — Khóa role và contract

- Chốt `SAMPLE_RECEIVER` riêng hay tái dùng `TECHNICIAN`.
- Chốt role nhận mẫu có được thao tác Phiếu 5 không.
- Chốt ảnh bắt buộc trước khi nhận và SLA chờ nhận.
- Khóa endpoint/DTO trong `docs/API_CONVENTIONS.md` và ma trận quyền trong `docs/RBAC.md`.

### K1 — Backend nhận mẫu

- Tạo package/projection nhận mẫu trong feature `transfer` hoặc sub-feature `receiving`, không mở receipt API đầy đủ.
- Thêm query phân trang/search/filter và dashboard aggregate.
- Thêm evidence metadata/content có kiểm tra ownership theo transfer.
- Hardening quyết định đồng thời và notification sau commit.
- Thêm migration/index và test authorization, PII, attachment traversal, pagination, timezone, concurrency.

### K2 — Frontend workspace KCS

- Role-aware landing/navbar.
- Dashboard ngày/tuần/tháng và hàng đợi chờ nhận.
- Danh sách phân trang + filter URL query.
- Chi tiết Phiếu 4, gallery ảnh/file và modal preview accessible.
- Nhận/từ chối với confirm, loading, lỗi 409 và refresh dữ liệu.

### K3 — Dashboard Receptionist

- Tách `/admin/intake` thành dashboard cá nhân và `/admin/intake/receipts` thành danh sách.
- Navbar Receptionist có `Tổng quan`, `Tiếp nhận mẫu`, `Chuyển mẫu`, `Thông báo`, `Thông tin cá nhân`.
- Tái sử dụng aggregate endpoint hiện có nhưng bổ sung hàng đợi hành động thay vì chỉ dải KPI.

### K4 — Nghiệm thu

- Backend unit/integration test với MySQL cho dashboard, list, evidence và race condition.
- Frontend build; test loading/error/empty/401/403/409.
- Chrome E2E theo hai tài khoản `RECEPTIONIST` và role nhận mẫu ở 390×844, 1024×768, 1440×900, 2560×952.
- E2E bắt buộc: Receptionist gửi Phiếu 4 có ảnh → người nhận thấy realtime → mở ảnh → nhận/từ chối → Receptionist nhận notification.

## 11. Acceptance criteria

- Người nhận mẫu đăng nhập thấy đúng bốn mục navbar của workspace nhận mẫu, không thấy Phiếu 3 hoặc thử nghiệm nếu role được tách.
- Dashboard đổi Hôm nay/Tuần/Tháng trả số liệu đúng theo giờ Việt Nam.
- Phiếu mới xuất hiện realtime nhưng reload vẫn lấy đủ qua REST.
- Người nhận xem được mọi ảnh/file cần đối chiếu mà không gọi API Phiếu 3 đầy đủ và không thấy PII khách hàng.
- Search/filter/sort/page chạy tại database.
- Hai người không thể quyết định cùng một Phiếu 4; request thua nhận 409 và UI tải lại trạng thái mới.
- Từ chối bắt buộc lý do; nhận/từ chối ghi actor và timestamp, gửi notification đúng một lần.
- Không có horizontal overflow tại các breakpoint chuẩn; gallery dùng được bằng bàn phím và trên mobile.

## 12. Câu hỏi cần người dùng chốt trước khi code

1. Người nhận mẫu KCS có phải cũng là người lập Phiếu 5/thử nghiệm không, hay phải là role riêng?
2. Khi Phiếu 4 chưa có ảnh mẫu/ảnh lô hàng, hệ thống **chặn nhận** hay chỉ **cảnh báo**?
3. SLA chờ nhận là bao lâu (ví dụ 30 phút, 2 giờ hay hết ca) để dashboard đánh dấu quá hạn?
