# Role plan — Người nhận mẫu KCS

**Cập nhật:** 16/11/2026

**Trạng thái:** Đã khảo sát source và biểu mẫu; người dùng xác nhận vai trò và dashboard, chưa triển khai các bổ sung

**Role runtime hiện tại:** `TECHNICIAN` · **Đích nhận mẫu duy nhất:** `KCS`

> Người dùng xác nhận người nhận mẫu KCS cũng được lập Phiếu 5 cùng Admin. Giữ `TECHNICIAN` cho cả nhận mẫu và thử nghiệm; không tạo `SAMPLE_RECEIVER` ở giai đoạn này.

## 1. Ranh giới nghiệp vụ từ biểu mẫu

Chuỗi trách nhiệm được chốt theo tài liệu gốc:

1. `RECEPTIONIST` lập Phiếu 3, ghi hồ sơ nguồn, tình trạng, ảnh và kết luận tiếp nhận.
2. `RECEPTIONIST` lập/gửi Phiếu 4 với danh sách mẫu, khối lượng, số lượng, tình trạng, chỉ tiêu và phương pháp thử.
3. Người nhận mẫu KCS đối chiếu mẫu vật, nhãn, Phiếu 4 và bằng chứng ảnh; sau đó nhận hoặc từ chối.
4. Chỉ mẫu đã nhận mới đi tiếp sang Phiếu 5 cho người phân tích/kiểm nghiệm.
5. Phiếu kết quả được tạo ở giai đoạn phát hành sau Phiếu 5, không thuộc workspace nhận mẫu.

Role nhận mẫu không sửa Phiếu 3 hoặc lập Phiếu 4. `TECHNICIAN` và `ADMIN` được lập Phiếu 5, tải nhiều file PDF và định dạng phù hợp lên hồ sơ thử nghiệm; bước kiểm tra/phát hành vẫn theo quyền riêng hiện có và quy trình đã chốt.

## 2. Hiện trạng source và khoảng trống

- `/admin/transfers` đã cho `TECHNICIAN` xem Phiếu 4 từ `PENDING` trở đi và nhận/từ chối mẫu.
- Navbar `TECHNICIAN` đã có `Chuyển mẫu`, `Thử nghiệm` và `Thông báo`; cần bổ sung Tổng quan riêng cho công việc nhận mẫu và thử nghiệm.
- Landing hiện chuyển thẳng tới `/admin/transfers`; chưa có dashboard ngày/tuần/tháng.
- `GET /api/v1/transfers` chỉ lấy tối đa 50 bản ghi rồi lọc role trong service; chưa phân trang, search hoặc lọc thời gian ở database.
- `TransferResponse` hiện trả `customerName`, trong khi người nhận mẫu không cần danh tính khách hàng.
- Phiếu chuyển chưa trả danh sách ảnh/hồ sơ. `TECHNICIAN` cũng bị chặn khỏi `/api/v1/receipts/**`, nên hiện không thể xem ảnh Phiếu 3 một cách đúng quyền.
- Chưa thấy chức năng đính kèm file riêng cho Phiếu 5 trong module thử nghiệm; cần triển khai upload nhiều file và xem/tải lại.
- Quyết định nhận mẫu mới kiểm tra trạng thái trước khi cập nhật, chưa có optimistic/pessimistic locking để bảo đảm hai người không thể quyết định đồng thời.
- Index hiện chỉ có `sample_transfers(status)`; dashboard theo `submittedAt/receivedAt` cần index phù hợp sau khi đo query thật.

## 3. Phân công đã xác nhận

- `RECEPTIONIST`: tạo Phiếu 3, gửi Phiếu 4 và có dashboard tiếp nhận riêng.
- `TECHNICIAN`: nhận/từ chối Phiếu 4, xem toàn bộ file liên quan được chuyển giao, lập Phiếu 5 và có dashboard KCS riêng.
- `ADMIN`: dashboard bao quát toàn bộ, được lập Phiếu 5 và có các quyền quản trị hiện tại.
- `TECHNICIAN` và `ADMIN` được tải nhiều file lên Phiếu 5; chưa tự suy rộng quyền phát hành kết quả hay bỏ yêu cầu kiểm tra độc lập.

## 4. Navbar và route của người nhận mẫu

Navbar chỉ chứa:

```text
Không gian nhận mẫu
├── Tổng quan                 /admin/receiving
├── Nhận mẫu                  /admin/receiving/transfers
├── Thử nghiệm               /admin/testing
├── Thông báo                 /admin/notifications
└── Thông tin cá nhân         /admin/profile
```

- Không hiện `Nhân viên`, `Tiếp nhận mẫu`, `Tạo Phiếu 3`, `Lập Phiếu 4` hoặc `Cài đặt chung`.
- `/admin/transfers` vẫn phục vụ Admin/Receptionist; nếu chuyển Technician sang route mới, giữ link chi tiết cũ hoạt động hoặc redirect theo role, không redirect chung làm hỏng luồng các role khác.
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
- Hiển thị thời gian đã chờ từ lúc gửi. Chưa đặt ngưỡng quá hạn/SLA cho đến khi LAB có quy định; SLA không liên quan đến việc mỗi role có dashboard riêng.

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
- Mỗi dòng/card hiển thị mã Phiếu 4, số mẫu, mã mẫu rút gọn, người bàn giao nội bộ, giờ gửi, hạn trả, số ảnh và thời gian chờ.
- WebSocket chỉ báo có thay đổi; REST/database vẫn là nguồn sự thật.

## 7. Chi tiết Phiếu 4 và bằng chứng ảnh

### Dữ liệu được xem

- Mã Phiếu 4, mã Phiếu 3, ngày chuyển, hạn trả, người giao nội bộ.
- Danh sách mẫu: mã, tên, khối lượng/số lượng, tình trạng, chỉ tiêu, phương pháp thử.
- Ghi chú thay đổi, yêu cầu lưu mẫu và chuỗi bàn giao.
- Toàn bộ file/ảnh đã gắn với hồ sơ bàn giao: Phiếu 3, ảnh mẫu, ảnh lô hàng, bao bì/niêm phong và các file liên quan khác; xem/tải bản gốc theo yêu cầu người dùng.

### Dữ liệu không trả cho role nhận mẫu

- API projection không tự đưa tên khách hàng, phone, email, địa chỉ thành các trường riêng khi không cần nghiệp vụ. File gốc được xem đầy đủ có thể chứa các thông tin này; không khẳng định nội dung file đã được ẩn hoặc loại PII.
- Mapping mã mù, secret/provider notification, lịch sử quản trị user.
- Hồ sơ nguồn đầy đủ nếu không cần cho việc đối chiếu bàn giao.

### Trải nghiệm ảnh

- Gallery theo nhóm `Phiếu 3`, `Ảnh mẫu`, `Ảnh lô hàng`, `Khác`.
- Thumbnail có số lượng, thời điểm chụp/tải và người tải.
- Click mở preview lớn; ảnh trước/sau, zoom và tải file nếu được phép.
- File PDF/DOCX hiện icon, tên file, dung lượng và nút mở/tải; không giả preview ảnh.
- Empty state ghi rõ “Phiếu chưa có ảnh đối chiếu”. Đề xuất trước mắt cảnh báo và cho phép nhận, vì chưa có quy tắc bắt buộc ảnh được xác nhận; không xem đây là kết luận từ biểu mẫu.

### Phiếu 5 — đính kèm nhiều file

- `TECHNICIAN` và `ADMIN` được chọn nhiều file PDF, ảnh và tài liệu hỗ trợ; từng file có tên, loại, dung lượng, người tải, thời điểm và liên kết với đúng Phiếu 5.
- Xem trước định dạng trình duyệt hỗ trợ; các định dạng khác cho tải về. Chỉ chốt danh sách định dạng và giới hạn sau khi kiểm tra nhu cầu LAB/cấu hình hiện hữu; không mặc định cho phép mọi loại file.
- Kiểm tra nội dung/định dạng, giới hạn dung lượng, tên lưu ngẫu nhiên và quyền đọc/tải tại backend; xử lý lỗi từng file và không làm mất Phiếu 5 đã lưu.
- Cần thiết kế migration versioned và API riêng cho attachment Phiếu 5; tính năng hiện chưa có runtime.

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

### K0 — Khóa contract

- Giữ `TECHNICIAN` cho cả nhận mẫu và Phiếu 5 theo xác nhận của người dùng.
- Áp dụng cảnh báo thiếu ảnh như giả định MVP; chưa áp đặt SLA hoặc cấm nhận thiếu ảnh.
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
- Phiếu 5 cho `TECHNICIAN`/`ADMIN`: tải nhiều file và xem/tải lại theo quyền.

### K3 — Dashboard Receptionist

- Tách `/admin/intake` thành dashboard cá nhân và `/admin/intake/receipts` thành danh sách.
- Navbar Receptionist có `Tổng quan`, `Tiếp nhận mẫu`, `Chuyển mẫu`, `Thông báo`, `Thông tin cá nhân`.
- Tái sử dụng aggregate endpoint hiện có nhưng bổ sung hàng đợi hành động thay vì chỉ dải KPI.

### K4 — Nghiệm thu

- Backend unit/integration test với MySQL cho dashboard, list, evidence và race condition.
- Frontend build; test loading/error/empty/401/403/409.
- Chrome E2E theo hai tài khoản `RECEPTIONIST` và role nhận mẫu ở 390×844, 1024×768, 1440×900, 2560×952.
- E2E bắt buộc: Receptionist gửi Phiếu 4 có ảnh → người nhận thấy realtime → mở ảnh → nhận/từ chối → Receptionist nhận notification.
- Thêm E2E: KCS và Admin lập Phiếu 5, đính kèm nhiều file, xem/tải lại; Receptionist không truy cập file Phiếu 5 nếu không có quyền.

### K5 — Dashboard Admin bao quát

- Tổng hợp toàn hệ thống theo ngày/tuần/tháng: tiếp nhận, mẫu đã chuyển/chờ nhận/đã nhận/từ chối, thử nghiệm và các trạng thái thực sự tồn tại.
- Hiển thị hàng đợi, bất thường và liên kết sang dashboard/chi tiết từng bộ phận; dữ liệu từ aggregate server-side, không cộng từ trang hiện tại.
- Phân biệt số phiếu và số mẫu; chỉ hiển thị trạng thái phát hành kết quả khi có runtime tương ứng.

## 11. Acceptance criteria

- Người nhận mẫu đăng nhập thấy dashboard riêng, nhận mẫu, thử nghiệm, thông báo và hồ sơ cá nhân.
- Dashboard đổi Hôm nay/Tuần/Tháng trả số liệu đúng theo giờ Việt Nam.
- Phiếu mới xuất hiện realtime nhưng reload vẫn lấy đủ qua REST.
- Người nhận xem được toàn bộ ảnh/file gốc liên quan qua quyền transfer; API projection hạn chế trường thông tin khách hàng nhưng file gốc có thể chứa dữ liệu đó.
- Search/filter/sort/page chạy tại database.
- Hai người không thể quyết định cùng một Phiếu 4; request thua nhận 409 và UI tải lại trạng thái mới.
- Từ chối bắt buộc lý do; nhận/từ chối ghi actor và timestamp, gửi notification đúng một lần.
- Không có horizontal overflow tại các breakpoint chuẩn; gallery dùng được bằng bàn phím và trên mobile.

## 12. Giả định cần kiểm chứng khi triển khai

- Thiếu ảnh chỉ cảnh báo; chốt quy định bắt buộc sau khi LAB xác nhận, không mặc định chặn.
- Chưa có SLA; chỉ hiển thị thời gian chờ.
- Giới hạn file và danh sách định dạng Phiếu 5 cần chọn theo nhu cầu thực tế và kiểm tra bảo mật.
