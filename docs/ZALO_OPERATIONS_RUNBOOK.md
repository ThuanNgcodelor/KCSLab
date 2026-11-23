# Vận hành thông báo Zalo tiếp nhận — P3/P4

Cập nhật 23/11/2026. Mã nguồn và kiểm thử dùng dữ liệu giả đã có; chưa triển khai lại hệ thống vận hành, chưa nhận/gửi tin Zalo thật. Các bước sau dành cho buổi triển khai riêng sau khi có App/OA/template và quyền hợp lệ. Không cần gửi secret vào chat.

## 1. Phạm vi và trạng thái

- Một phiếu tiếp nhận sinh một sự kiện, kể cả khách có nhiều phiếu; snapshot số nhận, Số/No. và template giữ nguyên.
- `ACCEPTED` chỉ xác nhận API nhận yêu cầu. `DELIVERED` cần bằng chứng giao tới thiết bị; chưa có trạng thái đã đọc.
- Mạng timeout/5xx/mất phản hồi là `UNKNOWN`, không tự gửi lại. Nếu không có `msg_id`, không thể polling bằng API trạng thái hiện tại; cần callback có `tracking_id` hoặc đối chiếu trên ZBS. Không tự đổi `UNKNOWN` thành trạng thái có thể retry.
- Hàng đợi test không được phát lại sang LIVE. Đổi mode/App/OA/đợt gửi chặn tin cũ; không sửa SQL để đổi mode của job.
- `/admin/customer-notifications`: chỉ Admin, số nhận che bớt, lọc Số/No./trạng thái, cảnh báo và tạm dừng/tiếp tục có lý do + audit. Cảnh báo lỗi provider được tổng hợp từ lịch sử, có thể còn sau khi cấu hình đã sửa; mở phiếu xem từng attempt.

## 2. Cấu hình máy chủ

Cấp secrets qua cơ chế cấu hình riêng của môi trường. Không đưa vào frontend, Git, access log, lệnh có token trong history hoặc báo cáo test. Lần triển khai code này không đọc/ghi `.env`.

Giữ các biến P2 trong [hướng dẫn test](ZALO_INTAKE_TESTING.md), thêm:

| Biến | Mặc định | Công dụng |
|---|---|---|
| `ZALO_WEBHOOK_ENABLED` | `false` | Nhận callback tại đúng route webhook |
| `ZALO_WEBHOOK_SECRET` | rỗng | Khóa xác minh webhook tương ứng OA theo tài liệu Zalo; xác nhận riêng với tài khoản, không tự coi là App Secret |
| `ZALO_WEBHOOK_MAX_AGE_SECONDS` | `86400` | Tuổi timestamp callback, clamp 60 giây–7 ngày; sai lệch tương lai tối đa 5 phút |
| `ZALO_STATUS_POLLING_ENABLED` | `false` | Đối soát các attempt ACCEPTED/UNKNOWN có `msg_id` |
| `ZALO_MAX_STATUS_CHECKS` | `6` | Tối đa 1–20 lần/attempt; cách nhau ít nhất 60 giây, trong 24 giờ sau attempt |
| `ZALO_TOKEN_REFRESH_ENABLED` | `false` | Worker refresh tự động |
| `ZALO_TOKEN_ENCRYPTION_KEY` | rỗng | Khóa AES 256 bit, 32 byte ngẫu nhiên mã hóa Base64, giữ ngoài DB và sao lưu riêng |
| `ZALO_APP_SECRET` | rỗng | Secret ứng dụng dùng refresh OAuth |
| `CUSTOMER_NOTIFICATION_LIVE_ENABLED` | `false` | Cổng bật LIVE độc lập với mode |
| `CUSTOMER_NOTIFICATION_LIVE_ALLOW_ALL_RECIPIENTS` | `false` | Khi false, LIVE vẫn chỉ gửi nhóm số được phép/pilot |

LIVE yêu cầu đồng thời: mode `ZALO_LIVE`, live gate, token quản lý READY, bật refresh + App Secret, bật polling, App/OA/template/mapping hợp lệ, session và cap hợp lệ. Allowlist bắt buộc trừ khi bật rõ `LIVE_ALLOW_ALL_RECIPIENTS`. Consent LIVE dùng version `intake-zalo-v1`; test dùng `intake-zalo-test-v1`.

`CUSTOMER_NOTIFICATION_TEST_SESSION` tiếp tục dùng làm mã đợt gửi; chọn đợt LIVE mới, không tái sử dụng đợt test. `CUSTOMER_NOTIFICATION_MAX_TEST_CALLS` là tổng cap bền vững của đợt: development 1–20, LIVE 1–100000; đặt thấp khi pilot. Đây là hạn mức lượt gửi nội bộ, không phải số dư hay quota Zalo; các truy vấn trạng thái/refresh không tính vào cap này. Lượt đã được claim vẫn tính khi request bị chặn sau claim hoặc gián đoạn.

## 3. Cấp và refresh token

1. Người quản trị OA cấp quyền App và lấy cặp access/refresh token ban đầu theo OAuth của Zalo bằng kênh riêng.
2. Cấu hình App/OA, khóa mã hóa, App Secret và bật refresh trên môi trường test. Mode có thể là DEVELOPMENT, chưa cần bật gửi LIVE.
3. Đăng nhập Admin CFC. Gọi `POST /api/v1/admin/customer-notifications/token` từ công cụ nội bộ qua HTTPS hoặc kết nối loopback tin cậy. Body:

   ```json
   {
     "accessToken": "<cấp riêng>",
     "refreshToken": "<cấp riêng>",
     "expiresIn": 90000,
     "requestId": "<mã duy nhất cho lần cấp>",
     "reason": "Cấp cặp token OA cho đợt thử"
   }
   ```

   `expiresIn` phải lấy từ phản hồi OAuth thực tế, không mặc định dùng số trong ví dụ. API chỉ trả 204, không trả token; nếu dùng cookie đăng nhập cần cookie `XSRF-TOKEN` cùng header `X-XSRF-TOKEN`. Không ghi body vào log. Gửi lại cùng requestId/nội dung là idempotent; đổi nội dung với cùng mã trả 409.
4. Kiểm tra `GET /api/v1/admin/customer-notifications` hoặc màn hình Admin: token READY và hạn dùng. Token trong DB được mã hóa AES-GCM; khóa gắn scope App/OA, dùng chung khi đổi development/LIVE.
5. Worker kiểm tra mỗi phút, refresh khi access token còn tối đa 10 phút. Lease trong DB đảm bảo một lượt refresh cho App/OA giữa nhiều instance. Cặp mới lưu nguyên tử; access token tạm không dùng trong lúc REFRESHING.
6. Timeout, phản hồi refresh không hợp lệ hoặc lease refresh hết hạn chuyển NEEDS_AUTH. Refresh token dùng một lần nên không tự gọi lại token cũ khi chưa rõ kết quả. Cấp lại cặp hợp lệ qua bước 3. Không dùng `ZALO_ACCESS_TOKEN` làm phương án dự phòng nếu kho token đã tồn tại nhưng lỗi.

Khóa mã hóa mất/đổi sai → không giải mã được, ngừng gửi và hiện cảnh báo. Khôi phục khóa đúng hoặc cấp lại cặp token bằng khóa mới trong buổi bảo trì; chưa có tự động xoay khóa. Khôi phục DB cũ có thể mang refresh token đã dùng: cần đối chiếu/cấp lại, không mở gửi ngay sau restore.

[OAuth chính thức](https://docs.zaloplatforms.com/docs/OA/bat-dau/xac-thuc-va-uy-quyen-cho-ung-dung-new) mô tả access token 25 giờ, refresh token 3 tháng và việc thay cặp token.

## 4. Callback và đối soát

- URL test: `https://<tên-miền-test>/api/v1/webhooks/zbs`. Chỉ public đúng POST route này qua tunnel/reverse proxy; không public API Admin hoặc cổng backend toàn bộ. Chưa dựng tunnel trong lần làm này. Có thể dùng polling trước khi có callback HTTPS.
- Proxy giữ nguyên raw JSON và hai header `X-ZEvent-Signature`, `X-ZEvent-Server`; giới hạn body 16 KiB và đặt rate limit ở proxy phù hợp lưu lượng pilot. Không log request body/headers chứa secret.
- Handler kiểm tra App/OA, server ZNS, timestamp và SHA-256 trên `appId + raw body + timestamp + webhook secret`, so sánh hằng thời gian. Chỉ nhận `user_received_message`; lưu metadata tối thiểu/recipient hash và chống trùng trong DB trước 2xx. DB lỗi không trả thành công.
- Inbox được xử lý nền, callback đến trước phản hồi gửi không bị ghi đè bởi ACCEPTED. Chưa có attempt thì chờ khớp tối đa khoảng một giờ. Sai App/OA/đích/msg_id bị loại; callback muộn chỉ cập nhật attempt tương ứng, không làm lùi attempt mới. Có attempt cũ đã giao sẽ chặn việc retry tiếp.
- Polling GET `/message/status` dùng `message_id` + phone snapshot. `status=1` và thời điểm giao hợp lệ mới cập nhật DELIVERED. `0`/`-1` hoặc lỗi GET không cho phép POST gửi lại và không hạ DELIVERED. Hết số lần kiểm tra thì giữ trạng thái hiện tại, Admin đối soát thủ công.

Nguồn: [webhook theo SĐT](https://docs.zaloplatforms.com/docs/ZBS/gui-tin-template-qua-sdt/webhook-gui-tin-qua-sdt/su-kien-nguoi-dung-nhan-tin-qua-sdt), [API trạng thái](https://docs.zaloplatforms.com/docs/ZBS/gui-tin-template-qua-sdt/api-truy-xuat-thong-tin-gui-tin-qua-sdt/api-lay-thong-tin-trang-thai-gui-tin-qua-sdt).

## 5. Lỗi, tạm dừng và dữ liệu

- LIVE chỉ tự retry lỗi từ chối xác định: `-115` sau 1 giờ, `-144`/`-147` sau 24 giờ, tối đa tổng 5 attempt. Mọi lần retry kiểm tra consent snapshot, scope, allowlist, pause và cap lại. Không tự SMS fallback.
- `-124`: kiểm tra quyền/token; `-139`/`-141`: người nhận từ chối, không retry. Các lỗi xác định khác cần sửa rồi Admin thử lại tại phiếu, có lý do và mã yêu cầu chống bấm lặp. [Bảng mã lỗi](https://docs.zaloplatforms.com/docs/ZBS/bang-ma-loi).
- Tạm dừng tại màn hình Admin; pause bền qua restart và có audit. Lệnh đang gửi có thể hoàn tất. Polling/webhook/refresh tiếp tục để hoàn thành đối soát. Tiếp tục sẽ xử lý queue còn đủ điều kiện. `OFF`/worker false ngừng nhận việc mới; không tự backfill phiếu không có event.
- Nếu có UNKNOWN: xem attempt, `msg_id`/tracking và thời điểm; đối chiếu ZBS trước. Chưa có API chuyển UNKNOWN thành quyền gửi lại. Không sửa SQL để ép resend, tránh thêm tin/phí khi tin đầu đã tới.
- Consent/outbox/attempt chứa dữ liệu khách và snapshot nội dung: chỉ tài khoản được phân quyền được truy cập; số đầy đủ không trả qua API lịch sử/Admin. DB và bản backup phải được bảo vệ như hồ sơ tiếp nhận.
- Đề xuất để LAB phê duyệt: inbox callback 90 ngày sau xử lý; audit/consent/attempt giữ theo thời hạn hồ sơ tiếp nhận và nghĩa vụ lưu trữ của LAB. Chưa có job tự xóa, chưa xóa dữ liệu nào. LAB cần chốt thời hạn, phạm vi backup và người có quyền truy xuất trước khi mở rộng pilot.

## 6. Triển khai Linux / Windows và nghiệm thu

Chuẩn bị bản build, backup DB + storage và khóa mã hóa riêng. Khi triển khai bản mới, Flyway V20/V21 tạo bảng và metadata mới; giữ mode OFF trong lần migrate đầu. Kiểm thử migration đã chạy trên MySQL cô lập, chưa chạy trên DB vận hành.

- Linux: theo [run.md](../run.md) và `docker-compose.yml`. Cấu hình được cấp cho container backend (Compose hiện dùng env_file); chỉ quản trị viên cập nhật cấu hình môi trường ở bước triển khai riêng. Build/recreate backend/frontend bằng quy trình sẵn có. Không bật/tắt MySQL/Redis chung để thử Zalo.
- Windows: theo [RUN_WINDOWS_SERVER.md](../RUN_WINDOWS_SERVER.md) và `scripts/windows/`. Cấp biến môi trường cho tiến trình Java; build `.jar`/frontend rồi restart tiến trình backend theo quy trình server. Đảm bảo máy chủ truy cập HTTPS outbound tới OA OAuth và business API, đồng hồ đúng; Apache/tunnel bảo toàn webhook như mục 4. Chưa thử trên Windows thật.
- Auth cookie hiện yêu cầu CSRF cho thao tác ghi; SPA dùng cookie XSRF cùng header như httpClient sẵn có. Explicit Authorization Bearer giữ cách gọi API cũ không phụ thuộc cookie; webhook có chữ ký không cần cookie/CSRF. Có test cookie thực cho trường hợp thiếu/đủ CSRF.

Nghiệm thu theo thứ tự: FAKE → một phiếu DEVELOPMENT nhận trên điện thoại → hai phiếu cùng số → callback/poll cập nhật DELIVERED → refresh token thật → pause/resume → pilot LIVE nhỏ có consent, allowlist và cap riêng. Kiểm số tin, thời gian giao, số dư/chi phí rồi LAB quyết định mở rộng. Bật LIVE là bước cấu hình riêng sau nghiệm thu; lần viết code này chưa thực hiện.

Chưa thuộc phạm vi này: mua/kích hoạt Zalo, phê duyệt template, cấu hình domain/tunnel thật, gửi thông báo kết quả `RESULT_PUBLISHED`, SMS và xóa dữ liệu theo lịch.
