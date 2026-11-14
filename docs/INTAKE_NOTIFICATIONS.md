# Kế hoạch tiếp nhận và thông báo khách hàng

**Phiên bản:** 1.0 · **Cập nhật:** 14/11/2026 · **Trạng thái:** Đã chốt thiết kế, chưa triển khai runtime

## 1. Mục tiêu và ranh giới

Sau khi nhân viên hoàn tất ghi nhận Phiếu tiếp nhận số 3, hệ thống gửi thông báo xác nhận cho khách hàng qua Zalo hoặc SMS mà không làm chậm hay làm hỏng giao dịch tiếp nhận.

Phạm vi này gồm:

- Lưu số điện thoại nhận tin, lựa chọn kênh và bằng chứng đồng ý nhận thông báo.
- Gửi xác nhận tiếp nhận, theo dõi trạng thái nhà cung cấp, retry có kiểm soát và lưu audit.
- Hỗ trợ nhiều nhà cung cấp qua adapter; ưu tiên ZBS Template Message, dùng SMS Brandname/MultiSMS làm dự phòng.
- Hiển thị lịch sử gửi trên chi tiết Phiếu 3 và cảnh báo nội bộ thời gian thực.

Phạm vi này chưa gồm marketing/broadcast, OTP, gửi kết quả thử nghiệm qua tin nhắn, public lookup hoặc đính kèm dữ liệu nhạy cảm. WebSocket chỉ phục vụ nhân viên đang đăng nhập; không thay thế Zalo/SMS gửi cho khách hàng.

## 2. Điểm kích hoạt nghiệp vụ

MVP dùng sự kiện `INTAKE_RECORDED`: phát sinh sau khi Phiếu 3 và các mẫu được lưu thành công. Nội dung phải dùng từ “đã ghi nhận tiếp nhận”, không khẳng định mẫu đã đạt kiểm tra hoặc đã được mã hóa.

Khi Phase 2 tách rõ các bước tạo hồ sơ → chấp nhận mẫu → gán mã → xác minh mã, bổ sung hai sự kiện riêng:

- `INTAKE_ACCEPTED`: mẫu được chấp nhận và có thể đi tiếp trong quy trình.
- `INTAKE_REJECTED`: mẫu/hồ sơ bị từ chối, chỉ gửi lý do đã được phép công khai.

Không gửi khi người dùng mới mở form, lưu dữ liệu chưa hoàn chỉnh hoặc giao dịch database bị rollback. Việc lập/gửi Phiếu 4 là sự kiện nội bộ khác, không được dùng lại template xác nhận tiếp nhận.

## 3. Chiến lược kênh

### Kênh ưu tiên

1. `ZALO_ZBS`: ZBS Template Message theo số điện thoại, dùng template đã được duyệt.
2. `SMS_BRANDNAME`: SMS chăm sóc khách hàng qua nhà cung cấp đã ký hợp đồng; MultiSMS/eSMS/nhà cung cấp khác được cài qua cùng một interface.
3. `NONE`: khách không đồng ý hoặc không có số điện thoại hợp lệ; chỉ lưu trạng thái, không gửi.

Tên dịch vụ hiện hành trong tài liệu Zalo là **ZBS Template Message**. Hệ thống không dựa vào chat OA thông thường vì loại tin này phụ thuộc quan hệ/tương tác OA và chính sách gửi tin khác.

### Quy tắc fallback

- `AUTO`: thử ZBS trước, chỉ chuyển SMS khi ZBS trả lỗi vĩnh viễn hoặc xác định không thể giao đến số nhận.
- Không fallback khi request đang timeout/không rõ kết quả; phải đối soát trước để tránh khách nhận hai tin.
- Mỗi sự kiện chỉ có một `idempotency_key`; mọi retry giữ nguyên khóa đối soát.
- Cho phép cấu hình `ZALO_ONLY`, `SMS_ONLY`, `AUTO`, `NONE` theo đồng ý của khách.
- Không tự động đổi kênh nếu điều khoản đồng ý chỉ cho phép một kênh.

## 4. Kiến trúc mục tiêu

```text
Hoàn tất Phiếu 3
       |
       v
Transaction: receipt/samples + notification_outbox
       |
       v
Worker nền trong modular monolith
       |
       v
Channel router ----> ZBS adapter ----> Zalo API
       |                                  |
       +---------> SMS adapter ----> SMS provider
                                          |
       v                                  v
notification_deliveries <--------- webhook/callback
       |
       +----> lịch sử trên Phiếu 3
       +----> WebSocket cảnh báo nội bộ
```

MVP dùng scheduled worker trong Spring Boot và database outbox; chưa thêm Kafka/RabbitMQ. Outbox và Phiếu 3 phải được ghi trong cùng transaction. Gửi mạng luôn diễn ra sau commit.

Hệ thống có ngữ nghĩa at-least-once với khóa idempotency; không tuyên bố exactly-once với dịch vụ bên ngoài. Adapter chuẩn hóa kết quả nhà cung cấp thành `QUEUED`, `SENT`, `DELIVERED`, `FAILED_RETRYABLE`, `FAILED_FINAL` hoặc `CANCELLED`.

## 5. Dữ liệu dự kiến

Mọi bảng MySQL mới dùng `VARCHAR`/`TEXT` với `utf8mb4`; không dùng `NVARCHAR`/`NCHAR`.

| Bảng | Dữ liệu chính |
|---|---|
| `customer_notification_preferences` | Số điện thoại chuẩn hóa, kênh ưu tiên, trạng thái đồng ý/từ chối, nguồn và thời điểm cập nhật |
| `receipt_notification_consents` | Snapshot đồng ý theo từng Phiếu 3: số nhận, kênh được phép, nội dung/phiên bản consent, actor và thời điểm |
| `notification_templates` | Mã nghiệp vụ, kênh, provider template ID, version, trạng thái active và schema tham số |
| `notification_outbox` | Event ID, aggregate ID, loại sự kiện, payload tối thiểu, idempotency key, số lần thử và thời điểm thử tiếp |
| `notification_deliveries` | Kênh/provider, số nhận đã mask/hash, provider message ID, trạng thái chuẩn hóa, mã lỗi và các mốc thời gian |

Ràng buộc chính:

- Unique `notification_outbox.idempotency_key`.
- Một delivery active cho mỗi `outbox + channel + provider`.
- Không ghi API key, access token hoặc số điện thoại đầy đủ vào log.
- Payload outbox chỉ chứa dữ liệu cần cho template; không chứa ảnh, file Phiếu 3, mapping mã mù hoặc kết quả thử nghiệm.

## 6. API và giao diện dự kiến

- `PATCH /api/v1/customers/{id}/notification-preferences`: cập nhật kênh/đồng ý dài hạn.
- `GET /api/v1/receipts/{id}/notification-deliveries`: xem lịch sử gửi của phiếu.
- `POST /api/v1/receipts/{id}/notification-deliveries`: Admin yêu cầu gửi lại delivery đã lỗi cuối cùng; bắt buộc lý do và idempotency.
- `POST /api/v1/webhooks/zbs`: nhận trạng thái giao ZBS, xác minh request theo cơ chế nhà cung cấp.
- `POST /api/v1/webhooks/sms/{provider}`: callback SMS cho provider được whitelist.

Form tiếp nhận bổ sung:

- Số điện thoại nhận thông báo, có thể lấy từ khách hàng nhưng phải xác nhận lại.
- Kênh `Tự động/Zalo/SMS/Không nhận`.
- Checkbox đồng ý và nội dung consent có version.
- Preview nội dung ở mức nghiệp vụ, không cho nhân viên sửa template tùy ý.

Chi tiết Phiếu 3 hiển thị timeline `Chờ gửi → Đã gửi → Đã nhận` hoặc lỗi, kênh thực tế và lần thử gần nhất. Chỉ Admin được gửi lại thủ công; Receptionist được xem trạng thái và sửa lựa chọn trước khi hoàn tất phiếu.

## 7. Nội dung template MVP

Template xác nhận chỉ nên có:

- Tên CFC/LAB.
- Mã phiếu tiếp nhận, thời điểm tiếp nhận và số lượng mẫu.
- Tên khách hàng rút gọn hoặc cách xưng hô đã xác nhận.
- Câu xác nhận “đã ghi nhận”, không khẳng định kết quả chuyên môn.
- Số liên hệ hỗ trợ.

Không gửi tên chỉ tiêu nhạy cảm, kết quả thử nghiệm, ảnh lô hàng, biển số/seal, mã mù hoặc link nội bộ. Link tra cứu chỉ thêm sau khi public lookup, rate limit và access grant đã hoàn thành.

## 8. Retry, đối soát và vận hành

- Lỗi mạng/429/5xx: retry theo exponential backoff có jitter; đề xuất tối đa 5 lần trong 24 giờ.
- Lỗi template, credential, số điện thoại hoặc consent: `FAILED_FINAL`, không retry tự động.
- Webhook được xử lý idempotent; sự kiện đến trễ không được làm lùi trạng thái `DELIVERED`.
- Job đối soát xử lý delivery ở trạng thái không rõ; không fallback khi chưa biết kênh trước đã giao hay chưa.
- Admin có màn hình hàng đợi/lỗi, bộ lọc theo kênh, trạng thái, thời gian và provider; list dùng pagination 20/trang.
- Theo dõi tỷ lệ queue, send, delivery, lỗi, fallback, thời gian giao và chi phí theo kênh; cảnh báo khi credential gần hết hạn, quota thấp hoặc tỷ lệ lỗi tăng.

## 9. Bảo mật và tuân thủ

- Credential nhà cung cấp chỉ nằm trong secret/environment của deployment; không commit và không trả ra frontend.
- Lưu bằng chứng đồng ý nhận tin và hỗ trợ opt-out; thay đổi consent không sửa lịch sử cũ.
- Mask số điện thoại trên UI/log theo role; audit mọi thao tác gửi lại và thay đổi cấu hình.
- Webhook bỏ qua CSRF nhưng bắt buộc xác minh chữ ký/token, timestamp chống replay, rate limit và allowlist provider nếu khả thi.
- Template phải được nhà cung cấp duyệt; nội dung quảng cáo không được dùng trong luồng giao dịch.
- Chốt thời hạn lưu số điện thoại, payload và delivery log trước production; rà soát pháp lý về dữ liệu cá nhân và tin chăm sóc khách hàng.

## 10. Kế hoạch triển khai

### N0 — Chuẩn bị nghiệp vụ và nhà cung cấp

- [ ] Chốt tên sự kiện và nội dung template `INTAKE_RECORDED`.
- [ ] Chốt nội dung consent, kênh mặc định và trường hợp gửi tin từ chối.
- [ ] Xác thực OA, tạo/liên kết ZBS Account + AppID + OA; tạo template và gửi duyệt.
- [ ] Chọn nhà cung cấp SMS Brandname/MultiSMS, lấy tài liệu API/callback/sandbox và đăng ký brandname/template.
- [ ] Chốt ngân sách, hạn mức và người sở hữu tài khoản nhà cung cấp.

### N1 — Nền tảng notification không phụ thuộc provider

- [ ] Tạo schema preferences/consent/template/outbox/delivery bằng migration versioned.
- [ ] Phát `INTAKE_RECORDED` cùng transaction khi Phiếu 3 được lưu hoàn tất.
- [ ] Viết worker, router, fake provider, idempotency, retry và state machine.
- [ ] Unit/integration test rollback, gửi trùng, retry, concurrency và số điện thoại Việt Nam.

### N2 — UI tiếp nhận và vận hành

- [ ] Bổ sung consent/channel vào form Phiếu 3 và timeline delivery ở trang chi tiết.
- [ ] Thêm hàng đợi lỗi cho Admin; phân trang 20/trang, sort/filter whitelist.
- [ ] Cho Admin gửi lại lỗi cuối cùng với lý do; không cho resend delivery đã `DELIVERED`.

### N3 — Tích hợp ZBS Template Message

- [ ] Quản lý OAuth access/refresh token an toàn và cảnh báo hết hạn.
- [ ] Map template/version, `tracking_id`, response/quota và webhook `user_received_message`.
- [ ] Test sandbox/UAT bằng số điện thoại nội bộ trước khi bật cho khách thật.

### N4 — SMS Brandname/MultiSMS và fallback

- [ ] Cài adapter provider đã chọn, RequestId/idempotency và callback trạng thái.
- [ ] Cấu hình fallback theo lỗi xác định; đối soát để không gửi kép.
- [ ] Test tiếng Việt/Unicode, độ dài nội dung, template đã đăng ký và quota.

### N5 — WebSocket nội bộ và production rollout

- [ ] Phát WebSocket cho nhân viên khi receipt được ghi nhận, delivery lỗi, Phiếu 4 được gửi/nhận/từ chối.
- [ ] Authorize topic theo role; client reconnect và tải lại REST để lấy nguồn sự thật.
- [ ] Bật theo feature flag: nội bộ → nhóm khách thử nghiệm → toàn bộ khách đủ consent.
- [ ] Dashboard SLA/chi phí, runbook sự cố, retention và cảnh báo provider.

## 11. Điều kiện hoàn thành

- Phiếu tiếp nhận vẫn thành công khi Zalo/SMS ngừng hoạt động; outbox giữ sự kiện để xử lý sau.
- Không có consent hoặc số điện thoại không hợp lệ thì không gọi provider.
- Một sự kiện không tạo tin trùng khi retry, worker chạy đồng thời hoặc webhook gửi lặp.
- Có thể truy vết receipt → event → delivery → provider message ID → trạng thái cuối.
- ZBS/SMS dùng template đã duyệt; không lộ secret hoặc dữ liệu thử nghiệm nhạy cảm.
- Fallback chỉ diễn ra theo ma trận lỗi đã test và không phát sinh hai tin khi kết quả kênh đầu chưa rõ.
- Backend test, MySQL integration, frontend build và Chrome E2E desktop/mobile đều pass.

## 12. Tài liệu nhà cung cấp tham chiếu

- Zalo OA — tổng quan ZBS Template Message: <https://oa.zalo.me/home/documents/vie/guides/tong-quan-cac-loai-tin-nhan-tren-zalo-official-account-_3651713298729094511>
- Zalo Business Solutions — gửi template qua API và webhook delivery: <https://zalo.solutions/business-message/guidelines/en/gui-api>
- Zalo Business Solutions — tài khoản, template và quy trình bắt đầu: <https://zalo.solutions/business-message/guidelines/intro>
- Tài liệu SMS chỉ được khóa sau khi LAB chọn đúng nhà cung cấp và cung cấp contract/sandbox hiện hành.
