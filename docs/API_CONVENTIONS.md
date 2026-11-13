# REST API conventions

**Phiên bản:** 0.2 · **Cập nhật:** 13/11/2026 · **Trạng thái:** Auth/user/intake đã có runtime API

## Quy ước chung

- Base path: `/api/v1`.
- Resource dùng danh từ số nhiều: `/users`, `/customers`, `/receipts`, `/samples`.
- `GET` đọc, `POST` tạo, `PATCH` cập nhật một phần, `DELETE` chỉ dùng khi nghiệp vụ cho phép; không dùng động từ trong URL.
- ID trong path; query dùng cho search/filter/sort/pagination.
- Danh sách tăng trưởng phải có `page` bắt đầu từ 0, `size` mặc định 20, `sort=field,direction`, giới hạn `size` tối đa cần chốt.
- Chỉ cho phép sort/filter fields đã lập danh sách; không nhận tên cột tùy ý từ client.

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
| GET/POST | `/api/v1/receipts` | Tối đa 50 phiếu mới nhất/tạo phiếu theo biểu mẫu 3 | Đã triển khai |
| GET | `/api/v1/receipts/{id}` | Xem phiếu theo ID | Đã triển khai |
| POST | `/api/v1/receipts/{id}/attachments` | Lưu file và metadata/checksum phiếu giấy | Đã triển khai |
| POST | `/api/v1/receipts/{id}/transfer` | Lập phiếu chuyển mẫu theo biểu mẫu 4 | Đề xuất |
| GET/POST | `/api/v1/samples` | Tìm/tạo mẫu | Đề xuất |
| GET | `/api/v1/samples/{id}` | Chi tiết mẫu | Đề xuất |
| POST | `/api/v1/samples/{id}/custody-events` | Ghi nhận bàn giao, tình trạng và người giao/nhận | Đề xuất |
| GET/POST | `/api/v1/testing-records` | Lập/cập nhật biên bản thử nghiệm biểu mẫu 5 | Đề xuất |
| POST | `/api/v1/testing-records/{id}/submit-review` | Gửi biên bản/kết quả chờ kiểm tra | Đề xuất |
| POST | `/api/v1/result-releases` | Duyệt và phát hành PDF kết quả | Đề xuất |
| POST | `/api/v1/public/lookups` | Tra cứu tiến độ có kiểm soát | Đề xuất bảo mật, chưa chốt |

Không có `POST /api/v1/auth/register`. Tài khoản chỉ được Admin tạo qua endpoint quản trị `/api/v1/users` sau khi đăng nhập.

## Response và lỗi

Runtime hiện trả object hoặc array trực tiếp. Chuẩn envelope/pagination bên dưới là mục tiêu cho các danh sách tăng trưởng, chưa áp dụng cho user/receipt hiện tại.

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
