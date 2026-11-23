# Thử thông báo tiếp nhận qua Zalo

Cập nhật 23/11/2026. Đã có mã nguồn P1–P4: outbox, provider giả/development, theo dõi giao tin, token refresh và vận hành Admin. Mặc định `OFF`; adapter `ZALO_LIVE` có các điều kiện bật riêng và chưa được bật/gửi thật. UI có lựa chọn nhận tin và lịch sử theo từng phiếu; Admin theo dõi toàn bộ tại `/admin/customer-notifications`. Xem [runbook P3–P4](ZALO_OPERATIONS_RUNBOOK.md).

## Trạng thái chuẩn bị tài khoản

Ảnh người dùng cung cấp mới xác nhận App DEV, đang hiển thị chưa kích hoạt. Chưa xác nhận OA, quyền gửi, Template ID, tên biến template, token hoặc SĐT quản trị nhận test. Vì vậy chưa nghiệm thu được gửi vào tài khoản Zalo thật; có thể kiểm thử toàn bộ luồng nội bộ bằng `FAKE` mà không mua dịch vụ Zalo.

App ID là mã ứng dụng. Template ID là mã từng mẫu tin đã tạo ở tài khoản ZBS, hoặc giá trị `template_id` trong request gửi thành công. Hai giá trị không thay thế cho nhau.

## Chạy kiểm thử tự động

Từ gốc repo:

```bash
bash scripts/run-be-tests-docker.sh -Dtest=CustomerNotificationIntegrationTest,ZaloTemplateProviderTest,ZaloLiveTemplateProviderTest,ZaloWebhookPersistenceTest,ReceiptServiceTest
```

Script dựng MySQL/Redis tạm, chạy migration V20–V21 và test, rồi dọn container thử nghiệm. Không dùng database làm việc. Những test này dùng fake provider và mock HTTP; không có tin gửi Zalo, không cần token thật và không phát sinh phí Zalo.

Kiểm tra P1–P2 đã có: form/payload, upload lỗi sau lưu phiếu, lịch sử/mode mô phỏng và retry Admin bằng API giả. P3–P4 bổ sung test MySQL + mock HTTP cho callback, đối soát, refresh token, phân quyền và CSRF; giao diện vận hành thử bằng Playwright tại 390/1024/1440/2560 px. Kết quả suite cuối ghi tại [Project Memory](ai/CONTEXT.md). Đây chưa phải nghiệm thu gửi Zalo thật hoặc UI dùng dữ liệu vận hành.

## Thử bằng giao diện với provider giả

Trên một backend đã được cấu hình **database/Redis thử riêng**, thêm tham số khi khởi chạy Java:

```text
--app.customer-notifications.mode=FAKE
```

Tên biến môi trường tương đương là `CUSTOMER_NOTIFICATION_MODE=FAKE`. Không cần khai báo OA/App/template/token trong mode này. Không đổi mode trên backend dùng chung chỉ để tạo case test; không tự thay cấu hình kết nối hay chỉnh `.env` vận hành.

Sau khi backend mới được build và khởi chạy với cấu hình trên:

1. Đăng nhập tài khoản CFC Admin hoặc Tiếp nhận của môi trường test.
2. Tạo phiếu với dữ liệu giả và Số/No. mới; chọn **Thử thông báo cho phiếu này**.
3. Xác nhận số nhận và ô đồng ý. Lưu phiếu.
4. Ban đầu thấy **Chờ xử lý**, sau lượt worker khoảng 5 giây thấy **Đã mô phỏng · chưa gửi Zalo**. Có thể bấm **Làm mới**; UI tự cập nhật trong tối đa khoảng hai phút khi còn chờ.
5. Tạo phiếu thứ hai có Số/No. khác, cùng khách/SĐT. Mỗi phiếu có một yêu cầu và một lịch sử riêng. Một phiếu nhiều mẫu vẫn chỉ một tin.
6. Bổ sung ảnh không tạo thêm tin. Nếu ảnh tải lỗi, phiếu và yêu cầu thông báo đã lưu vẫn còn; tin chỉ xác nhận đã ghi nhận tiếp nhận.

`FAKE` là mô phỏng, không phải xác nhận Zalo đã nhận/giao tin. Không tự chuyển các job fake sang Zalo khi đổi mode.

## Cấu hình adapter Zalo để dùng sau khi có tài khoản đủ điều kiện

Chỉ dùng trên môi trường test với dữ liệu giả. Credential cấp riêng cho tiến trình backend bằng cơ chế secret/config của môi trường; không đưa token vào frontend, chat, Git hoặc tham số shell có thể lưu vào history.

| Biến | Giá trị/ý nghĩa |
|---|---|
| `CUSTOMER_NOTIFICATION_MODE` | `ZALO_DEVELOPMENT` |
| `CUSTOMER_NOTIFICATION_ALLOWED_PHONES` | Số quản trị test, phân cách bằng dấu phẩy; bắt buộc không rỗng |
| `CUSTOMER_NOTIFICATION_TEST_SESSION` | Tên đợt test, 1–80 chữ/số/`_`/`-`; giữ nguyên khi restart cùng đợt |
| `CUSTOMER_NOTIFICATION_MAX_TEST_CALLS` | Mặc định 5, cho phép 1–20; số lượt giữ bền trong DB theo đợt test |
| `ZALO_APP_ID` / `ZALO_OA_ID` | Cặp App/OA đã cấp quyền |
| `ZALO_INTAKE_TEMPLATE_ID` | Template dùng thử |
| `ZALO_INTAKE_TEMPLATE_VERSION` | Version snapshot, mặc định `1` |
| `ZALO_INTAKE_BINDINGS_JSON` | Mapping tên biến template với nguồn dữ liệu CFC |
| `ZALO_ACCESS_TOKEN` | Access token của OA cấp riêng phía backend |
| `CUSTOMER_NOTIFICATION_WORKER_ENABLED` | `true` mặc định; `false` dừng lấy job mới |

Nếu thiếu cấu hình, API settings trả `ready=false`; form không cho bật gửi. Request từ client bật gửi khi cấu hình chưa đủ vẫn lưu phiếu với thông báo `BLOCKED`; mode `OFF` lưu `CANCELLED`. Thiếu đồng ý hoặc SĐT không hợp lệ thì trả 400 trước khi lưu phiếu. Số hợp lệ ngoài allowlist bị chặn gửi, không đổi đích sang số test khác.

Mapping sau chỉ là **ví dụ**, không phải tên biến đã xác nhận từ template của người dùng:

```json
{
  "order_code": { "source": "requestNumber", "maxLength": 80 },
  "received_time": { "source": "receivedAt", "maxLength": 30 },
  "quantity": { "source": "sampleCount", "maxLength": 10 }
}
```

Nguồn được hỗ trợ: `requestNumber`, `receiptCode`, `customerName`, `receivedAt` (`dd/MM/yyyy HH:mm`), `receivedDate` (`dd/MM/yyyy`), `sampleCount`. Ngày giờ theo `Asia/Ho_Chi_Minh`. Giá trị được gửi dưới dạng chuỗi. Phải có ít nhất một biến dùng `requestNumber` để phân biệt nhiều đơn. Template có biến khác, kiểu dữ liệu khác hoặc giá trị cố định cần chốt mapping trước buổi gửi thật; không bỏ qua biến bắt buộc và không tự cắt dữ liệu quá dài.

Payload luôn có `mode=development`. Allowlist kiểm tra lại tại worker và trước lệnh HTTP. Tên đợt, App/OA và mode được chụp theo phiếu; đổi sang đợt/App/OA khác chặn queue cũ. Template/data của phiếu đã tạo giữ nguyên khi cấu hình template thay đổi.

Theo tài liệu Zalo, development mode chỉ gửi tới quản trị viên App/OA; quyền và quota vẫn phải đáp ứng. Mode test không đảm bảo miễn phí. [API test](https://docs.zaloplatforms.com/docs/ZBS/gui-tin-template-qua-sdt/api-gui-tin-qua-sdt/api-gui-tin-su-dung-development-mode), [bảng mã lỗi](https://docs.zaloplatforms.com/docs/ZBS/bang-ma-loi).

## Trạng thái và xử lý lỗi

| Trạng thái | Ý nghĩa |
|---|---|
| `QUEUED` / `SENDING` | Chờ hoặc đang xử lý |
| `SIMULATED` | Provider giả đã xử lý, không gọi Zalo |
| `ACCEPTED` | Zalo nhận request, có `msg_id`; chưa xác nhận giao/đọc |
| `DELIVERED` | Có bằng chứng thiết bị nhận từ webhook/API trạng thái; chưa biết đã đọc |
| `RETRY_WAIT` | Fake backoff 30/60/120/240 giây; LIVE chỉ retry lỗi từ chối số dư/quota rõ ràng, chờ 1 giờ/24 giờ, tối đa 5 attempt |
| `UNKNOWN` | Timeout, lỗi phản hồi hoặc lease hết hạn; không tự POST lại |
| `BLOCKED` | Thiếu cấu hình, sai phạm vi test, hết giới hạn gọi hoặc ngoài allowlist |
| `FAILED_FINAL` | Provider trả lỗi xác định; cần sửa điều kiện trước khi Admin thử lại |
| `CANCELLED` | Tính năng đang tắt tại lúc tạo phiếu |

Adapter Zalo development không tự retry. LIVE chỉ tự retry các lỗi provider xác định `-115` (1 giờ), `-144`/`-147` (24 giờ), tối đa 5 attempt và vẫn tính cap. HTTP 200 cần `error=0` và `msg_id`; lỗi không xác định, HTTP không thành công và mất kết nối chuyển `UNKNOWN`. Không lưu/hiển thị nguyên văn response có thể chứa thông tin riêng. HTTP thành công không được báo thành “Khách đã nhận/đã đọc”.

Chỉ Admin thử lại `FAILED_FINAL`, bắt buộc lý do và `requestId` chống bấm lặp, tối đa tổng 5 lần/phiếu. Không thử lại tin đã nhận API, đã giao, đang xử lý, mô phỏng, bị chặn, hủy hoặc `UNKNOWN`; lỗi người nhận từ chối `-139`/`-141` không được retry. Khi sửa allowlist/cấu hình bị thiếu, tạo phiếu test mới thay vì phát lại queue đã `BLOCKED`.

Outbox và consent cùng transaction với phiếu. Worker claim/ghi attempt trong transaction ngắn, gọi mạng ngoài transaction. Unique theo `INTAKE_RECORDED:receiptId`, lease và cap theo session giúp tránh gửi trùng nội bộ; không đảm bảo exactly-once với Zalo. Worker chết giữa lượt gửi → `UNKNOWN` và cần đối soát.

Tắt `CUSTOMER_NOTIFICATION_MODE=OFF` hoặc worker trước khi khởi động lại để ngừng lấy việc mới. Lệnh đã phát đi có thể hoàn tất; tính năng tắt không thu hồi tin đã gửi. Không tự backfill phiếu cũ hoặc chuyển dữ liệu test sang gửi production.

## Phần chưa nghiệm thu thực tế

- Webhook/polling, token refresh và adapter LIVE đã có code + test giả; chưa có callback, credential hoặc lượt gửi thực tế từ OA của người dùng. Không có bằng chứng “đã đọc”.
- Chưa dựng tunnel/HTTPS, mở pilot khách thật hoặc chốt thời gian lưu dữ liệu với LAB. SMS và thông báo phát hành kết quả là phạm vi tiếp theo, chưa triển khai.
- Chưa kết nối tài khoản Zalo thật, chưa đổi/mua gói, chưa bật App và chưa tạo template hộ người dùng.
- Chưa build/restart backend vận hành hoặc chạy migration trên database thật trong lần triển khai code này.

Xem [plan và ma trận ca kiểm thử](../plans/ZALO_INTAKE_TEST_PLAN.md).
