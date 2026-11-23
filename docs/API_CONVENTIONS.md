# Quy ước REST API

Base path `/api/v1`. Danh sách dùng `page` bắt đầu từ 0, `size` mặc định 20 và `sort=field,direction` theo danh sách cho phép của từng endpoint. Phản hồi danh sách có `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` và `sort`. Các danh sách ứng viên có thể trả mảng. Ngày nghiệp vụ dùng múi giờ `Asia/Ho_Chi_Minh`, ngày kết thúc bộ lọc được tính trọn ngày.

Cookie access JWT và refresh session phía server dùng cùng origin. Khởi tạo CSRF từ `GET /auth/csrf`. Mutation nội bộ gửi `X-XSRF-TOKEN`; login, refresh và API khách thuộc nhóm cấu hình riêng. Các lỗi nghiệp vụ có status HTTP phù hợp. Xem [RBAC](RBAC.md) để đối chiếu quyền.

## Điểm vào theo module

| Module | Endpoint chính |
|---|---|
| Phiên và tài khoản | `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/me`, `/users`, `/users/me`, `/users/{id}/avatar` |
| Yêu cầu | `/test-requests`, `/test-requests/by-number/{number}`, `/test-requests/next-number` |
| Lấy mẫu | `PUT /test-requests/{id}/sampling-plan`, `PUT /test-requests/{id}/field-sampling` |
| Tiếp nhận | `/receipts`, `/receipts/{id}/attachments`, `/receipts/{id}/form-3` |
| Chuyển mẫu | `/transfers`, `POST /transfers/{id}/submit`. File bằng chứng lưu qua endpoint đính kèm của hồ sơ tiếp nhận |
| KCS | `/receiving/dashboard`, `/receiving/transfers`, `/receiving/transfers/{id}/evidence`, `POST /receiving/transfers/{id}/decision` |
| Thử nghiệm | `/testing-records`, `/testing-records/candidates`, `POST /testing-records/from-result-file`, `/test-methods` |
| Kết quả | `/result-releases`, `GET /result-releases/report-header?transferItemId=`, `POST /result-releases/from-entry`, `PUT /result-releases/{id}/report`, `POST /result-releases/{id}/report-file`, `GET /result-releases/{id}/report-preview`, `GET /result-releases/{id}/report-docx`, `GET /result-releases/{id}/sample-photos/{attachmentId}`, `POST /result-releases/{id}/submit`, `POST /result-releases/{id}/return`, `POST /result-releases/{id}/publish`, `POST /result-releases/{id}/revoke`, `POST /result-releases/{id}/regenerate-report`, `GET /result-releases/lookup-settings`, `POST /result-releases/regenerate-reports` |
| Tra cứu | `POST /public/results/lookup`, `POST /public/results/download` |

- `GET /test-requests/next-number` gợi ý số dạng `CFC26.0001`. Để trống khi tạo sẽ cấp số nguyên tử. Số yêu cầu đã tạo là bất biến, trùng trả 409. Hai biểu mẫu lấy mẫu dùng chung số yêu cầu.
- KCS đọc projection của phiếu đã gửi. File bằng chứng phải thuộc hồ sơ đã bàn giao. Quyết định được khóa theo phiếu, trả 409 nếu phiếu hết trạng thái đang chờ.
- Biên bản từ file dùng `transferItemId` của mẫu đã được nhận. Tải KQKN bằng multipart `file` chứa DOCX hoặc PDF. DOCX cần LibreOffice. Admin xem trước PDF rồi phát hành. Mã mẫu nhập tay hợp lệ được ghi nhận ở trạng thái sẵn sàng khi tiếp nhận.
- Nhập kết quả: `GET /result-releases/report-header` trả 10 mục thông tin mẫu và chuỗi chỉ tiêu/phương pháp của mẫu. `POST /result-releases/from-entry` nhận `{transferItemId, testingDate, rows[{parameter, unit, method, result, note}], submit}` (1–40 dòng, mỗi dòng cần chỉ tiêu và kết quả). API tạo biên bản, hồ sơ và các dòng trong một giao dịch; mỗi mẫu chỉ một biên bản (trùng thì 409).
- Xem trước PDF và `report-docx` đều tạo từ cùng mẫu Word; PDF cần LibreOffice. Khi phát hành, số phiếu = Số/No. của phiếu tiếp nhận. `regenerate-report` và `regenerate-reports` chỉ dành cho Admin, chỉ áp dụng cho phiếu do hệ thống tạo, giữ nguyên số và mã tra cứu.
- Tra cứu và tải dùng `code`, `phoneLast4`; thêm `fileId` khi tải. Chỉ trả hồ sơ đã phát hành. Có giới hạn truy cập và `Cache-Control: no-store`. Xem [triển khai trang tra cứu](PUBLIC_LOOKUP_SEO.md).

## Thông báo tiếp nhận cho khách qua Zalo

- `POST /receipts` thêm object tùy chọn `customerNotification: {enabled, recipientPhone, consentAccepted}`. Không có object hoặc tắt thì giữ luồng cũ, không gửi tin.
- `GET /receipts/notification-settings`: ADMIN/RECEPTIONIST đọc mode, readiness, mã thiếu cấu hình và nội dung đồng ý; không trả token, allowlist hoặc đích nhận đầy đủ.
- `GET /receipts/{id}/notification-deliveries?page=0&size=20`: ADMIN/RECEPTIONIST đọc lịch sử theo phiếu, số nhận đã mask; phân trang 0-based, size 1–100, sort cố định `createdAt,desc`.
- `POST /receipts/{id}/notification-deliveries/{deliveryId}/retry`: chỉ ADMIN, body `{reason, requestId}`. Kiểm tra đúng phiếu, giới hạn attempt và trạng thái; trả 202 khi nhận yêu cầu, 409 khi chưa được retry.
- Settings, lịch sử và retry dùng `Cache-Control: no-store`. Mutation giữ CSRF. Chưa có endpoint webhook công khai trong P0–P2.
- Mặc định `OFF`; `FAKE` chỉ mô phỏng; `ZALO_DEVELOPMENT` bắt buộc allowlist và giới hạn số lần gọi theo đợt. `ACCEPTED` chưa phải `DELIVERED`. Chi tiết tại [hướng dẫn test](ZALO_INTAKE_TESTING.md).

## API thông báo nội bộ

- `GET /notifications?page=0&size=20&unread=false` hỗ trợ tìm kiếm và lọc. Kết quả có `unreadCount` cho toàn bộ thông báo của người nhận.
- `PATCH /notifications/{id}` đánh dấu đã đọc và trả bản ghi cập nhật. `POST /notifications/read-all` đánh dấu tất cả của người đang đăng nhập.
- WebSocket `/ws/notifications` xác thực bằng cookie. Cùng cấu trúc bản ghi như REST. Các trường gồm `id`, `type`, `title`, `message`, `targetUrl`, `entityType`, `entityId`, `createdAt`, `readAt` và `actor`.
- `actor` có `displayName` và `avatarUrl`. Với tin lịch sử hoặc người thực hiện đã bị xóa, `actor` có thể là null. Tên và ảnh lấy từ hồ sơ hiện tại của người thực hiện.
- Bản ghi được ghi trong transaction nghiệp vụ. WebSocket gửi sau commit đến các tab của người nhận. REST dùng để đồng bộ khi kết nối lại hoặc mất kênh trực tiếp.

## Đối soát và vận hành Zalo P3/P4

- `POST /webhooks/zbs`: public đúng route/method, không cần cookie/CSRF; bắt buộc chữ ký trên raw JSON, App/OA/header/timestamp hợp lệ, body <=16 KiB. Lưu inbox chống trùng trước 200; tắt/chưa cấu hình 503, chữ ký/scope sai 403, payload sai 400, quá lớn 413. DB lỗi không trả 2xx.
- `GET /admin/customer-notifications`: ADMIN, tổng hợp mode/pause/count/warnings/expiry; không trả token/secret.
- `GET /admin/customer-notifications/deliveries?status=&q=&page=0&size=20`: ADMIN, lọc trạng thái/Số/No.; q tối đa 80, size 1–100, số nhận mask, sort `createdAt,desc`.
- `POST /admin/customer-notifications/pause`: ADMIN, body `{paused, requestId, reason}`, 204; ghi audit và chống lặp requestId, khác nội dung 409.
- `POST /admin/customer-notifications/token`: ADMIN, body `{accessToken, refreshToken, expiresIn, requestId, reason}`, 204; không trả credentials. Mã hóa cặp token với khóa ngoài DB, 409 nếu cấu hình chưa đủ hoặc đang refresh. Không log body này.
- API lịch sử theo phiếu bổ sung `deliveredAt`, `deliverySource`, `statusCheckCount`, `lastStatusCheckAt`, `lastStatusCode` trên attempt. HTTP nhận lệnh gửi chỉ là ACCEPTED; DELIVERED có bằng chứng từ callback/status, không phải đã đọc.
- API có dữ liệu vận hành/token dùng `Cache-Control: no-store`. Request ghi bằng cookie JWT yêu cầu CSRF; SPA gửi `XSRF-TOKEN` cookie và header `X-XSRF-TOKEN`. Explicit Authorization Bearer không yêu cầu CSRF, webhook có chữ ký dùng ngoại lệ riêng.

Xem [runbook Linux/Windows và cấu hình](ZALO_OPERATIONS_RUNBOOK.md).
