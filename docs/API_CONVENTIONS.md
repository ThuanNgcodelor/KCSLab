# REST API conventions

**Phiên bản:** 0.6 · **Cập nhật:** 14/11/2026 · **Trạng thái:** Auth/user/intake/transfer/testing đã có runtime API; notification mới ở mức đề xuất

## Quy ước chung

- Base path: `/api/v1`.
- Resource dùng danh từ số nhiều: `/users`, `/customers`, `/receipts`, `/samples`.
- `GET` đọc, `POST` tạo, `PATCH` cập nhật một phần, `DELETE` chỉ dùng khi nghiệp vụ cho phép; không dùng động từ trong URL.
- ID trong path; query dùng cho search/filter/sort/pagination.
- Danh sách tăng trưởng phải có `page` bắt đầu từ 0, `size` mặc định 20, `sort=field,direction`, giới hạn `size` tối đa cần chốt.
- Chỉ cho phép sort/filter fields đã lập danh sách; không nhận tên cột tùy ý từ client.
- Runtime receipt đã chốt `size` tối đa 100; sort whitelist: `receivedAt`, `createdAt`, `receiptCode`, `customerName`, `sampleName`, `status`, `expectedDate` với chiều `asc|desc`.
- Mọi search/filter receipt mới phải chạy trong truy vấn database qua specification/custom query; không gọi `findAll()` không giới hạn rồi search/sort/paginate trong Java hoặc frontend.
- Query mục tiêu cho receipt: `q`, `from`, `to`, `status`, `decision`, `productTypeId`, `receivedBy`, `page`, `size`, `sort`. Khoảng ngày dùng business date `Asia/Ho_Chi_Minh`; `to` là ngày bao gồm và được chuyển thành cận trên loại trừ ở backend.

## Endpoint dự kiến

| Method | Path | Mục đích | Trạng thái |
|---|---|---|---|
| GET | `/api/v1/auth/csrf` | Lấy CSRF token | Đã triển khai |
| POST | `/api/v1/auth/login` | Tạo phiên nhân viên bằng email | Đã triển khai |
| POST | `/api/v1/auth/refresh` | Xoay refresh session | Đã triển khai |
| POST | `/api/v1/auth/logout` | Thu hồi phiên | Đã triển khai |
| GET | `/api/v1/auth/me` | Lấy user hiện tại | Đã triển khai |
| GET/POST | `/api/v1/users` | Danh sách/tạo nhân viên, Admin-only | Đã triển khai |
| PATCH | `/api/v1/users/{id}` | Sửa role/trạng thái/mật khẩu | Đã triển khai |
| GET/POST | `/api/v1/customers` | Tìm/tạo khách hàng | Đề xuất |
| PATCH | `/api/v1/customers/{id}/notification-preferences` | Cập nhật consent/kênh thông báo dài hạn; lưu actor và version nội dung đồng ý | Đề xuất Phase 1B |
| GET | `/api/v1/intake/dashboard/receptionist` | KPI và hàng đợi tiếp nhận của user hiện tại theo khoảng ngày | Đề xuất Intake I1 |
| GET | `/api/v1/intake/dashboard/admin` | KPI tổng hợp/workload/ngoại lệ; Admin-only | Đề xuất Intake I1 |
| GET | `/api/v1/intake/activities` | Hoạt động tiếp nhận thật, phân trang/filter server-side | Đề xuất Intake I1 |
| GET | `/api/v1/intake/filter-options` | Gợi ý có giới hạn cho filter/form; không trả toàn bộ catalog | Đề xuất Intake I1 |
| GET/POST | `/api/v1/receipts` | Danh sách phân trang mặc định 20 phiếu/lập phiếu theo biểu mẫu 3 | Đã triển khai |
| GET | `/api/v1/receipts/{id}` | Xem phiếu theo ID | Đã triển khai |
| POST | `/api/v1/receipts/{id}/attachments` | Lưu file/ảnh tối đa 10 MB cùng `category`, mô tả, thời điểm chụp, actor và checksum | Đã triển khai |
| GET | `/api/v1/receipts/{id}/attachments/{attachmentId}/content` | Xem/tải bằng chứng thuộc đúng phiếu tiếp nhận | Đã triển khai |
| GET | `/api/v1/receipts/{id}/notification-deliveries` | Lịch sử ZBS/SMS đã mask theo Phiếu 3 | Đề xuất Phase 1B |
| POST | `/api/v1/receipts/{id}/notification-deliveries` | Admin yêu cầu gửi lại lỗi cuối cùng, bắt buộc lý do/idempotency | Đề xuất Phase 1B |
| POST | `/api/v1/receipts/drafts` | Tạo nháp Phiếu 3, chưa gửi khách/chuyển KCS | Đề xuất Intake I5 |
| PATCH | `/api/v1/receipts/{id}` | Sửa Phiếu 3 khi còn nháp | Đề xuất Intake I5 |
| POST | `/api/v1/receipts/{id}/completion` | Hoàn tất Phiếu 3 và phát event sau commit | Đề xuất Intake I5 |
| POST | `/api/v1/receipts/{id}/coding` | Cấp mã mẫu nguyên tử sau khi hồ sơ/mẫu được chấp nhận | Đề xuất theo CFC-TN-14 |
| POST | `/api/v1/samples/{id}/coding-verification` | Người thứ hai xác minh nhãn, phiếu nguồn và bản ghi | Đề xuất theo CFC-TN-14 |
| POST | `/api/v1/samples/{id}/parts` | Sinh phần nền `-V/-R` từ mã gốc | Đề xuất theo CFC-TN-14 |
| GET | `/api/v1/samples/{id}/label` | Tạo/in nhãn mẫu; ghi nhận actor in nhãn | Đề xuất |
| GET/POST | `/api/v1/transfers` | Danh sách/lập phiếu chuyển mẫu theo biểu mẫu 4 | Đã triển khai |
| GET | `/api/v1/transfers/{id}` | Chi tiết phiếu chuyển mẫu | Đã triển khai |
| PUT | `/api/v1/transfers/{id}` | Sửa Phiếu 4 khi còn `DRAFT`; không được đổi phiếu tiếp nhận | Đã triển khai |
| POST | `/api/v1/transfers/{id}/submit` | Gửi bàn giao, khóa mẫu và chuyển `DRAFT → PENDING` | Đã triển khai |
| POST | `/api/v1/transfers/{id}/decision` | KCS/PTN chấp nhận hoặc từ chối bàn giao | Đã triển khai |
| GET/POST | `/api/v1/samples` | Tìm/tạo mẫu | Đề xuất |
| GET | `/api/v1/samples/{id}` | Chi tiết mẫu | Đề xuất |
| GET/POST | `/api/v1/testing-records` | Danh sách/lập biên bản thử nghiệm biểu mẫu 5 | Đã triển khai |
| GET/PUT | `/api/v1/testing-records/{id}` | Xem/cập nhật biên bản nháp hoặc được trả lại | Đã triển khai |
| POST | `/api/v1/testing-records/{id}/submit` | Gửi biên bản chờ kiểm tra | Đã triển khai |
| POST | `/api/v1/testing-records/{id}/review` | Admin kiểm tra hoàn tất hoặc trả lại | Đã triển khai |
| POST | `/api/v1/result-releases` | Duyệt và phát hành PDF kết quả | Đề xuất |
| POST | `/api/v1/public/lookups` | Tra cứu tiến độ có kiểm soát | Đề xuất bảo mật, chưa chốt |
| GET | `/api/v1/notifications` | Danh sách notification nội bộ phân trang; size=3 dùng cho header | Đề xuất Intake I2 |
| PATCH | `/api/v1/notifications/{id}` | Owner đánh dấu notification đã đọc | Đề xuất Intake I2 |
| POST | `/api/v1/notifications/read-all` | Đánh dấu các notification hiện có đã đọc, idempotent | Đề xuất Intake I2 |
| GET | `/api/v1/product-types` | Search catalog loại sản phẩm phân trang | Đề xuất Intake I5 |
| POST | `/api/v1/product-types/suggestions` | Receptionist đề xuất loại sản phẩm nhập tay | Đề xuất Intake I5 |
| PATCH | `/api/v1/product-types/{id}` | Admin duyệt/chuẩn hóa/vô hiệu hóa loại sản phẩm | Đề xuất Intake I5 |
| GET | `/api/v1/test-parameters` | Search catalog chỉ tiêu thử nghiệm phân trang | Đề xuất Intake I5 |
| POST | `/api/v1/webhooks/zbs` | Nhận delivery event ZBS; CSRF-exempt nhưng phải xác minh request, chống replay và idempotent | Đề xuất Phase 1B |
| POST | `/api/v1/webhooks/sms/{provider}` | Callback từ SMS provider thuộc whitelist | Đề xuất Phase 1B |

Không có `POST /api/v1/auth/register`. Tài khoản chỉ được Admin tạo qua endpoint quản trị `/api/v1/users` sau khi đăng nhập.

Phiếu chuyển mẫu còn `DRAFT` chỉ hiển thị cho `ADMIN|RECEPTIONIST`; `TECHNICIAN` chỉ nhận danh sách/chi tiết sau khi phiếu đã gửi. Quyết định nhận/từ chối chỉ áp dụng cho `PENDING`.

API coding phải tách DTO nội bộ: Receptionist/Admin được xem mapping hồ sơ nguồn; Technician mặc định chỉ nhận mã mẫu, phần nền, chỉ tiêu và dữ liệu kỹ thuật cần thiết. Không trả bảng đối chiếu mã mù trong API thử nghiệm thông thường.

API notification không gọi ZBS/SMS đồng bộ trong `POST /receipts`. Sau khi receipt commit, worker lấy event từ outbox và gọi provider. `POST .../notification-deliveries` là lệnh resend có audit, không phải cách gửi mặc định; delivery đã `DELIVERED` không được resend. Webhook không tin trạng thái do client gửi, phải map từ event provider sang state machine nội bộ và không cho event đến trễ làm lùi trạng thái cuối.

Notification nội bộ dùng bảng/API riêng với customer notification delivery. Header gọi `GET /notifications?page=0&size=3&sort=createdAt,desc`; response phải kèm `unreadCount` để không cần tải toàn bộ tin. WebSocket chỉ phát ID/payload tối thiểu sau commit; khi mở app hoặc reconnect, frontend luôn reconcile lại bằng REST.

## Response và lỗi

`GET /api/v1/receipts` trả envelope ổn định:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "sort": "receivedAt,desc"
}
```

Các list API cũ khác hiện vẫn có thể trả array trực tiếp; khi chuyển sang phân trang phải dùng cùng contract này thay vì expose trực tiếp `PageImpl` của Spring.

Lỗi đề xuất:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "fieldErrors": [{"field": "phone", "message": "Số điện thoại không hợp lệ"}],
  "traceId": "..."
}
```

HTTP status: `200/201/204` thành công; `400` input sai; `401` chưa xác thực; `403` không đủ quyền; `404` không tìm thấy hoặc không tiết lộ tồn tại tùy ngữ cảnh; `409` xung đột nghiệp vụ; `429` vượt giới hạn; `500` lỗi không dự kiến. Không bọc mọi lỗi bằng `200`.

## Cookie/JWT đề xuất

Runtime dùng access JWT cookie `access_token`, refresh session phía server, rotation và thu hồi khi logout. CSRF dùng `CookieCsrfTokenRepository`; login/refresh được miễn CSRF. `Secure` được điều khiển bằng cấu hình deployment. Domain/cross-origin production và thời hạn tuyệt đối cần được kiểm tra lại trước phát hành.
