# Plan riêng — Module Tiếp nhận mẫu

**Cập nhật:** 14/11/2026 · **Trạng thái:** Đã khảo sát source/Phiếu 3, chưa triển khai runtime mới

## 1. Kết luận hiện trạng

- Menu hiện chỉ có một mục `Tiếp nhận mẫu`; route `/admin/intake` đang là danh sách, chưa có dashboard riêng theo nghiệp vụ.
- App shell có sidebar nhưng chưa có header toàn cục chứa tiêu đề trang, chuông thông báo, trạng thái kết nối và tài khoản.
- `GET /api/v1/receipts` mới nhận `page`, `size`, `sort`; chưa có search, khoảng ngày, trạng thái, loại mẫu hoặc người tiếp nhận.
- Backend đang dùng `receiptRepository.findAll(Pageable)`: đã phân trang nhưng chưa có truy vấn filter/search server-side.
- Dashboard chung đang dùng `stats` và `activity` hard-code. Dữ liệu này phải bị loại bỏ khi API dashboard thật được nối vào; không thay bằng dữ liệu giả khác.
- Form tạo Phiếu 3 đã có nhiều mẫu và ảnh/file, nhưng một số control chưa bám cấu trúc biểu mẫu: loại sản phẩm bị khóa ở `SR/MIT/OTHER`, bao bì/tình trạng/chỉ tiêu lại là input text đơn.
- Chưa có notification persistence, REST API, WebSocket hoặc trang thông báo.
- Hệ thống có đúng một phòng nhận mẫu là KCS; UI không cần bộ chọn phòng. Backend dùng giá trị cấu hình/canonical `KCS` thay vì nhận chuỗi phòng tùy ý từ frontend.

## 2. Mục tiêu

Biến `Tiếp nhận mẫu` thành một không gian làm việc riêng, tối ưu cho thao tác tại quầy và trên điện thoại:

1. Dashboard dùng dữ liệu thật, khác nhau theo role.
2. Danh sách tìm kiếm/lọc/sort/phân trang hoàn toàn ở backend.
3. Form Phiếu 3 bám đúng biểu mẫu giấy nhưng nhập nhanh hơn, hỗ trợ giá trị “Khác” có kiểm soát.
4. Header và notification center có dữ liệu lưu bền; WebSocket chỉ đẩy thay đổi realtime.
5. Khi Phiếu 4 được gửi, KCS nhận thông báo realtime; không quản lý nhiều phòng.
6. Không làm lộ dữ liệu khách hàng cho KCS và không dùng WebSocket làm nguồn sự thật.

## 3. Bài học từ biểu mẫu và hệ thống Việt Nam

### Phiếu 3 của CFC

Phiếu thật có sáu nhóm:

1. Thông tin tiếp nhận: mã mẫu, ngày giờ, người giao, người nhận.
2. Thông tin mẫu: tên mẫu, số lượng/khối lượng, bao bì, niêm phong.
3. Tình trạng khi nhận: nguyên vẹn, rò rỉ, dập nát, thiếu lượng, bất thường và mô tả.
4. Vận chuyển/bảo quản: phương tiện, thời gian, nhiệt độ, thường/lạnh/khác, điều kiện lưu.
5. Yêu cầu thử nghiệm: Cadimi, Auramin O, khác, hạn dự kiến và yêu cầu đặc biệt.
6. Kết luận: chấp nhận, có điều kiện, từ chối, lý do, người phê duyệt.

Phiếu không quy định danh sách đóng “Sầu riêng/Mít/Khác”. Vì vậy:

- `Tên mẫu` luôn là text tự do bắt buộc.
- `Loại sản phẩm` là combobox tìm kiếm danh mục và cho nhập tên mới.
- `product code` là dữ liệu nội bộ riêng, không bắt người tiếp nhận tự nghĩ mã.
- Bao bì, tình trạng và chỉ tiêu dùng multi-select theo lựa chọn của phiếu, kèm ô “Khác”.

### Tham khảo hệ thống Việt Nam

- QUATEST 3 dùng mã yêu cầu/hồ sơ làm điểm neo xuyên suốt, trả lại mã và ngày dự kiến cho khách, cho tra cứu tiến độ và gửi tin khi dịch vụ hoàn tất. Luồng CFC nên luôn hiển thị mã phiếu, người tiếp nhận và hạn dự kiến ngay sau khi lưu. [Hướng dẫn QUATEST 3](https://quatest3.com.vn/huong-dan-dang-ky-dich-vu-thu-nghiem)
- QUATEST 3 tách thông tin khách cung cấp và phần đơn vị thử nghiệm chấp nhận; đây là cơ sở để CFC tách “ghi nhận hồ sơ” khỏi “phê duyệt/chấp nhận mẫu” ở Phase 2. [Phiếu yêu cầu QUATEST 3](https://quatest3.com.vn/Content/UserImages/Files/TN/21.06.M01-TTTN%2001%20-%20PYC%20TN%20%28SD0-BH9%29.pdf)
- QUATEST 1 cho tra cứu bằng số đăng ký, giữ giao diện tra cứu ngắn gọn. CFC nên dùng search theo mã làm đường nhanh nhất thay vì buộc mở nhiều bộ lọc. [Tra cứu QUATEST 1](https://www.quatest1.com.vn/tra-cuu-tien-do-ho-so)
- MEDLATEC gửi thông tin tra cứu qua SMS nhưng yêu cầu mã bảo mật riêng. CFC chỉ gửi xác nhận tiếp nhận trong phase này; link kết quả phải chờ public lookup và access grant an toàn. [Hướng dẫn MEDLATEC](https://medlatec.vn/tin-tuc/huong-dan-khach-hang-tra-cuu-ket-qua-kham-xet-nghiem-s5-n22394)

Không sao chép giao diện các website trên. Chỉ áp dụng các pattern nghiệp vụ: mã hồ sơ rõ ràng, trạng thái ngắn gọn, thông báo theo mốc và tra cứu có bảo mật.

## 4. Cấu trúc menu và route

Mục `Tiếp nhận mẫu` trở thành nhóm điều hướng:

```text
Tiếp nhận mẫu
├── Tổng quan                 /admin/intake
├── Danh sách phiếu          /admin/intake/receipts
├── Tạo phiếu                /admin/intake/new
└── Thông báo                /admin/notifications
```

Route chi tiết chuyển thành `/admin/intake/receipts/:id`. Giữ redirect tương thích từ `/admin/intake/:id` trong ít nhất một release.

Role landing:

- `ADMIN` vào `/admin`: dashboard quản trị toàn hệ thống; có link nhanh đến dashboard tiếp nhận.
- `RECEPTIONIST` đăng nhập xong vào `/admin/intake`.
- `TECHNICIAN` vào `/admin/transfers`; vẫn dùng notification center chung nhưng không nhìn thấy menu tạo Phiếu 3.

## 5. Header và notification center

### Header toàn cục

Desktop:

```text
┌────────────────────────────────────────────────────────────────────┐
│ Tiếp nhận mẫu / Tổng quan       Kết nối realtime ●   🔔 4   Thuận ▾ │
└────────────────────────────────────────────────────────────────────┘
```

Mobile:

```text
┌────────────────────────────────────┐
│ ☰  Tiếp nhận mẫu          ●  🔔 4 │
└────────────────────────────────────┘
```

Header sticky trong app shell, không lặp một hero lớn ở mọi page. Page vẫn có heading ngắn và primary action riêng.

### Chuông thông báo

- Badge hiển thị số chưa đọc, tối đa `99+`.
- Popover chỉ tải/hiển thị ba tin mới nhất.
- Mỗi tin có icon loại sự kiện, tiêu đề, mô tả một dòng, thời gian tương đối và trạng thái chưa đọc.
- Nút `Xem tất cả thông báo` mở `/admin/notifications`.
- Khi có trên ba tin, không render toàn bộ trong popover; backend vẫn trả `totalElements` và `unreadCount`.
- Click notification đánh dấu đã đọc rồi điều hướng theo `targetUrl`.
- Nếu WebSocket mất kết nối, hiện trạng thái “Đang kết nối lại”; REST vẫn hoạt động bình thường.

### Trang thông báo

- Tab `Tất cả` và `Chưa đọc`.
- Filter loại sự kiện và khoảng ngày.
- Phân trang server-side 20 tin/trang, sort whitelist theo `createdAt` và `readAt`.
- Có `Đánh dấu tất cả đã đọc`; không có xóa vật lý trong MVP.
- Empty state hướng dẫn rõ “Chưa có thông báo trong khoảng thời gian này”.

## 6. Dashboard Tiếp nhận

### Bộ lọc chung

- Từ ngày/đến ngày theo `Asia/Ho_Chi_Minh`; backend chuyển thành khoảng Instant nửa mở `[from, to + 1 ngày)`.
- Search một ô: ưu tiên exact/prefix cho mã phiếu, mã mẫu, số điện thoại; sau đó customer/sample name.
- Trạng thái tiếp nhận, kết luận, loại sản phẩm, người tiếp nhận (Admin), hạn dự kiến.
- Kích thước trang `20/50/100`, mặc định 20.
- Sort theo whitelist; mỗi thay đổi filter reset về trang 0.
- Filter được phản ánh lên URL query để reload/chia sẻ nội bộ không mất trạng thái.

Frontend debounce search 300–400 ms rồi gọi API; không dùng `Array.filter`, `Array.sort` hoặc tải tất cả bản ghi để phân trang.

### Bố cục

```text
┌─ Header ────────────────────────────────────────────────────────────┐
├─ Hôm nay: 12 nhận │ 3 chờ chuyển │ 1 có điều kiện │ 0 từ chối ────┤
├─ Search mã/khách/mẫu ── Từ ngày ─ Đến ngày ─ Bộ lọc ──────────────┤
├──────────────────────────────────────────┬─────────────────────────┤
│ Hàng đợi tiếp nhận / danh sách gần đây   │ Việc cần xử lý          │
│ dữ liệu thật, 20 dòng/trang              │ hạn hôm nay, lỗi hồ sơ  │
├──────────────────────────────────────────┴─────────────────────────┤
│ Hoạt động tiếp nhận gần đây từ API, không hard-code                │
└────────────────────────────────────────────────────────────────────┘
```

Dashboard không dùng bốn card giống nhau làm điểm nhấn. Điểm nhận diện là “bảng ca tiếp nhận hôm nay”: một dải số liệu ngang gọn như sổ trực, nối trực tiếp với hàng đợi bên dưới.

## 7. Danh sách Phiếu 3

- Dùng cùng query contract với dashboard nhưng là page chuyên sâu.
- Cột desktop: mã phiếu, khách hàng/người giao, mẫu, thời điểm nhận, người nhận, kết luận, trạng thái, hạn dự kiến.
- Mobile chuyển từng dòng thành card có nhãn; action chính là `Mở phiếu`.
- Có skeleton/loading, lỗi có nút thử lại, empty state theo filter.
- Không gọi endpoint không phân trang và không gọi repository `findAll()` không có `Pageable`/specification.
- Export CSV là phase sau; nếu làm phải export bằng job/server query, không export từ mảng dữ liệu hiện trên frontend.

## 8. Thiết kế lại form tạo Phiếu 3

### Hướng thiết kế

Form là một “biên bản số hóa”, không phải wizard nhiều trang. Người nhận mẫu thường cần nhìn lại toàn bộ thông tin và đối chiếu giấy/mẫu vật cùng lúc.

Desktop dùng nội dung chính + rail kiểm tra sticky; mobile xếp một cột và thanh hành động sticky phía dưới.

```text
┌─ Tạo phiếu tiếp nhận ─────────────────────────── Lưu nháp | Hoàn tất ┐
├─ 01 Tiếp nhận ────────────────────────────────┬─ Kiểm tra hồ sơ ────┤
├─ 02 Mẫu và bao bì                             │ ✓ Người giao         │
├─ 03 Tình trạng khi nhận                       │ ! Ảnh lô hàng        │
├─ 04 Vận chuyển và bảo quản                    │ ✓ Số điện thoại      │
├─ 05 Yêu cầu thử nghiệm                        │ Trạng thái: Nháp     │
├─ 06 Kết luận                                  │                      │
└───────────────────────────────────────────────┴──────────────────────┘
```

### Mapping control theo Phiếu 3

| Nhóm | Control đề xuất |
|---|---|
| Người giao/khách hàng | Search khách cũ theo phone/name từ backend; cho tạo mới tại chỗ |
| Người nhận | Tự lấy user đăng nhập, read-only |
| Tên mẫu | Input tự do bắt buộc |
| Loại sản phẩm | Searchable combobox; chọn catalog hoặc nhập tên mới |
| Mã sản phẩm | Backend/catalog quản lý; không cho Receptionist tự sửa mã đã duyệt |
| Số lượng/khối lượng | Giá trị + đơn vị tách riêng để có dữ liệu cấu trúc |
| Bao bì | Multi-select: Túi PE, Hộp/thùng, Chai/lọ, Khác + mô tả |
| Niêm phong | Số niêm phong + `Còn nguyên/Không có/Đã mở` |
| Tình trạng | Multi-select: Nguyên vẹn, Rò rỉ, Dập nát, Thiếu lượng, Bất thường + mô tả |
| Vận chuyển | Phương tiện, thời lượng, nhiệt độ và `Thường/Lạnh/Khác` |
| Chỉ tiêu | Searchable multi-select từ catalog: Cd, Auramin O, Khác |
| Kết luận | Radio ba trạng thái; có điều kiện/từ chối bắt buộc lý do |
| Phê duyệt | Tách khỏi người nhập; triển khai cùng gate Phase 2 |
| Ảnh/file | Phiếu 3, mẫu, lô hàng, bao bì/niêm phong/hư hỏng; preview và retry từng file |

### Loại sản phẩm nhập tay

Không tiếp tục dùng select cứng `SR/MIT/OTHER`.

- `product_catalog`: `id`, `code`, `name`, `aliases`, `active`, `approval_status`.
- Receptionist gõ “Thanh long ruột đỏ”; backend trả gợi ý nếu đã có.
- Nếu không có, cho chọn `Tạo loại mới “Thanh long ruột đỏ”`; record ở trạng thái `PENDING`.
- Admin chuẩn hóa tên và cấp code trước khi mã mẫu được xác minh.
- Trong Phase 2, mã mẫu chỉ sinh sau khi product code đã được duyệt; không dùng `OTHER` cho mọi giá trị mới vì sẽ mất truy xuất.

### Lưu nháp và hoàn tất

- `Lưu nháp`: cho phép thiếu trường chưa bắt buộc, không gửi khách, không chuyển KCS.
- `Hoàn tất tiếp nhận`: validate Phiếu 3, ghi actor/time, tạo event `INTAKE_RECORDED` và enqueue notification khách hàng nếu có consent.
- Upload file lỗi không được làm mất phiếu; từng file có trạng thái và nút retry.
- Cảnh báo khi rời trang có thay đổi chưa lưu.

## 9. API backend đề xuất

### Dashboard và danh sách

| Method | Endpoint | Trách nhiệm |
|---|---|---|
| GET | `/api/v1/intake/dashboard/receptionist?from&to` | KPI và hàng đợi của user đang đăng nhập |
| GET | `/api/v1/intake/dashboard/admin?from&to&receivedBy` | KPI toàn bộ, workload và ngoại lệ |
| GET | `/api/v1/receipts` | Search/filter/sort/pagination server-side |
| GET | `/api/v1/intake/activities?page&size&from&to` | Hoạt động tiếp nhận thật, phân trang |
| GET | `/api/v1/intake/filter-options?query` | Gợi ý khách, loại sản phẩm, chỉ tiêu; response giới hạn |

Query list receipt:

```text
page=0&size=20
&q=CT001
&from=2026-11-01&to=2026-11-14
&status=RECEIVED,TRANSFER_PENDING
&decision=ACCEPTED
&productTypeId=12
&receivedBy=5
&sort=receivedAt,desc
```

Backend dùng `JpaSpecificationExecutor` hoặc custom query/projection; mọi điều kiện chạy trong SQL. Không dùng `repository.findAll()` rồi filter/map trong memory. Summary dùng aggregate query riêng, không tính từ 20 dòng của page.

### Notifications

| Method | Endpoint | Trách nhiệm |
|---|---|---|
| GET | `/api/v1/notifications?page=0&size=3&sort=createdAt,desc` | Ba tin header + `unreadCount` |
| GET | `/api/v1/notifications?page=0&size=20&unread=true&type=...` | Trang thông báo |
| PATCH | `/api/v1/notifications/{id}` | Đánh dấu đã đọc; chỉ owner |
| POST | `/api/v1/notifications/read-all` | Đánh dấu các tin hiện có đã đọc, idempotent |

### Catalog và form

| Method | Endpoint | Trách nhiệm |
|---|---|---|
| GET | `/api/v1/product-types?query=&page=0&size=20` | Tìm loại sản phẩm |
| POST | `/api/v1/product-types/suggestions` | Receptionist đề xuất giá trị mới |
| PATCH | `/api/v1/product-types/{id}` | Admin duyệt/chuẩn hóa/khóa catalog |
| GET | `/api/v1/test-parameters?query=&page=0&size=20` | Gợi ý chỉ tiêu |
| POST | `/api/v1/receipts/drafts` | Lưu nháp Phiếu 3 |
| PATCH | `/api/v1/receipts/{id}` | Sửa nháp trước khi hoàn tất |
| POST | `/api/v1/receipts/{id}/completion` | Hoàn tất tiếp nhận, phát event sau commit |

Tên/path cuối cùng phải được khóa trong `docs/API_CONVENTIONS.md` trước khi code. Không expose JPA `PageImpl`; dùng envelope ổn định hiện có.

## 10. Dữ liệu và index

### Notification nội bộ

`notifications`:

- `id`, `recipient_user_id`, `type`, `title`, `message`.
- `target_url`, `entity_type`, `entity_id`.
- `dedupe_key` unique, `read_at`, `created_at`.

Không dùng chung bảng này với delivery ZBS/SMS cho khách hàng. Notification nội bộ và customer notification là hai bounded contexts có thể dùng chung domain event nhưng khác recipient, trạng thái và bảo mật.

### Query tiếp nhận

- Index `receipts(received_at, id)`, `receipts(status, received_at)`, `receipts(received_by_id, received_at)`.
- Index `samples(product_type_id, received_date)`, `samples(sample_code)` và các khóa customer hiện dùng.
- Search mã dùng exact/prefix trước. `%keyword%` trên nhiều bảng chỉ là MVP nhỏ; khi dữ liệu lớn phải dùng search key/FULLTEXT phù hợp, không tải toàn bảng.
- Tất cả chuỗi MySQL dùng `VARCHAR/TEXT` + `utf8mb4`, không dùng `NVARCHAR`.

## 11. WebSocket nội bộ

Stack đề xuất: Spring WebSocket + STOMP và client STOMP cho React. Repository hiện chưa có dependency WebSocket nên implementation cần thêm dependency có chủ đích và test security.

- Handshake `/ws` dùng access JWT cookie hiện có; chỉ user đã xác thực được kết nối.
- Client chỉ subscribe `/user/queue/notifications`; server xác định recipient, không cho client tự subscribe topic role rộng.
- Khi domain event commit, backend ghi `notifications` trước, sau đó publish payload nhỏ: `id`, `type`, `title`, `message`, `targetUrl`, `createdAt`.
- Mở app hoặc reconnect luôn gọi REST lấy ba tin mới nhất/unread count. WebSocket chỉ giảm độ trễ, không thay REST/database.
- Reconnect exponential backoff; refresh session trước khi reconnect khi access token hết hạn.
- Dedupe theo notification ID; event lặp không tạo hai item UI.
- KCS nhận `TRANSFER_SUBMITTED`; Receptionist nhận `TRANSFER_ACCEPTED/REJECTED`; Admin nhận ngoại lệ tiếp nhận và notification delivery lỗi.

## 12. Visual direction

Skill `frontend-design` được áp dụng theo ngữ cảnh quầy tiếp nhận và biểu mẫu giấy CFC.

### Token đề xuất

- `CFC green #087A45`: action chính và trạng thái kết nối.
- `Ledger navy #17324D`: chữ/tiêu đề mang cảm giác hồ sơ kỹ thuật.
- `Paper #FCFDFB`: nền form.
- `Rule #D7E0DA`: đường kẻ chia mục như biên bản.
- `Conditional amber #B66A12`: có điều kiện/cảnh báo.
- `Reject red #B42318`: từ chối/lỗi cuối.

Typography giữ font sans hiện có nếu hỗ trợ tiếng Việt tốt; dùng weight/spacing thay vì thêm font trang trí. Dữ liệu mã dùng tabular numerals, không biến toàn bộ UI thành monospace.

Điểm khác biệt duy nhất là cấu trúc “sổ trực/biên bản số hóa”: đường chia mục rõ, số mục đúng thứ tự nghiệp vụ, sticky checklist. Không dùng gradient, animation trang trí hoặc một bộ card SaaS giống nhau.

## 13. Ba role và ranh giới

Chi tiết ở:

- `plans/intake/RECEPTIONIST.md`
- `plans/intake/ADMIN.md`
- `plans/intake/TECHNICIAN_KCS.md`

| Khả năng | Receptionist | Admin | Technician/KCS |
|---|:---:|:---:|:---:|
| Dashboard tiếp nhận cá nhân | ✓ | Xem được | — |
| Dashboard tổng hợp | — | ✓ | — |
| Tạo/sửa/hoàn tất Phiếu 3 | ✓ | ✓ | — |
| Duyệt loại sản phẩm mới | — | ✓ | — |
| Xem dữ liệu khách hàng đầy đủ | Theo phạm vi | ✓ | — |
| Nhận thông báo Phiếu 4 gửi KCS | — | Theo dõi | ✓ |
| Gửi lại customer notification lỗi | — | ✓ | — |

## 14. Thứ tự triển khai

### I1 — API search/filter/dashboard thật

- [ ] Thêm criteria DTO, specification/custom query và aggregate projection.
- [ ] Bổ sung `q/from/to/status/decision/productType/receivedBy/page/size/sort`.
- [ ] Tạo hai dashboard endpoint theo role và activity endpoint.
- [ ] Thêm index/migration và test query, timezone, quyền, page size/sort whitelist.
- [ ] Xóa `stats/activity` hard-code sau khi frontend dùng API thật.

### I2 — App header và notification persistence

- [ ] Tạo bảng/entity/service/API notification nội bộ.
- [ ] Thêm header responsive, popover ba tin, badge và trang thông báo phân trang.
- [ ] Test chỉ owner đọc/mark-read notification của mình.

### I3 — WebSocket realtime

- [ ] Thêm Spring WebSocket/STOMP và client cần thiết.
- [ ] Xác thực handshake bằng JWT cookie, user destination và reconnect.
- [ ] Phát notification sau commit cho receipt/transfer; REST reconcile khi reconnect.

### I4 — Dashboard và danh sách Tiếp nhận

- [ ] Tách route tổng quan/danh sách/tạo/chi tiết.
- [ ] Làm filter URL-driven, debounce search và responsive 390/1024/1440/2560.
- [ ] Chỉ dùng dữ liệu API; không client-side search/sort/pagination.

### I5 — Form Phiếu 3 và catalog

- [ ] Tạo product/test-parameter catalogs và luồng đề xuất/duyệt.
- [ ] Refactor form theo sáu mục Phiếu 3, multi-select + “Khác”, preview/retry ảnh.
- [ ] Thêm draft/completion, unsaved warning và gate phê duyệt.
- [ ] Gắn `INTAKE_RECORDED` với notification khách hàng theo plan riêng.

### I6 — Hardening và E2E

- [ ] Test MySQL thật với dữ liệu đủ nhiều để xác nhận pagination/filter không tải toàn bảng.
- [ ] Test hai user/role song song và WebSocket reconnect/dedupe.
- [ ] Chrome E2E desktop/mobile: dashboard → tạo nháp → hoàn tất → notification → Phiếu 4 → KCS nhận realtime.

## 15. Điều kiện hoàn thành

- Không còn số liệu/hoạt động hard-code trên dashboard.
- Mọi list tăng trưởng mặc định 20/trang, max 100; query chạy ở database.
- Header hiển thị ba tin mới nhất và dẫn tới trang notification đầy đủ.
- Refresh/reconnect không mất notification và không tạo bản ghi trùng.
- Receptionist nhập được loại sản phẩm mới nhưng không tự cấp product code chính thức.
- Form thể hiện đủ sáu phần của Phiếu 3 và bắt buộc lý do khi có điều kiện/từ chối.
- KCS là một đích cố định; KCS chỉ nhận Phiếu 4 đã submit và không thấy dữ liệu khách không cần thiết.
- Backend tests, frontend build và Chrome E2E bốn viewport đều pass.
