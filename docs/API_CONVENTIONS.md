# REST API conventions sơ bộ

**Phiên bản:** 0.1 · **Trạng thái:** Đề xuất; chưa có runtime API

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
| POST | `/api/v1/auth/login` | Tạo phiên nhân viên bằng email | Đề xuất |
| POST | `/api/v1/auth/refresh` | Xoay refresh session | Đề xuất |
| POST | `/api/v1/auth/logout` | Thu hồi phiên | Đề xuất |
| GET | `/api/v1/auth/me` | Lấy user hiện tại | Đề xuất |
| GET/POST | `/api/v1/customers` | Tìm/tạo khách hàng | Đề xuất |
| GET/POST | `/api/v1/receipts` | Danh sách/tạo phiếu | Đề xuất |
| GET/PATCH | `/api/v1/receipts/{id}` | Xem/sửa phiếu theo trạng thái | Đề xuất |
| GET/POST | `/api/v1/samples` | Tìm/tạo mẫu | Đề xuất |
| GET | `/api/v1/samples/{id}` | Chi tiết mẫu | Đề xuất |
| POST | `/api/v1/public/lookups` | Tra cứu tiến độ có kiểm soát | Đề xuất bảo mật, chưa chốt |

Không có `POST /api/v1/auth/register`. Tài khoản chỉ được Admin tạo qua endpoint quản trị `/api/v1/users` sau khi đăng nhập.

## Response và lỗi

Response thành công đơn lẻ nên có `data`; danh sách nên có `items`, `page`, `size`, `totalItems`, `totalPages`. Tên field chính thức phải chốt cùng frontend.

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

Access JWT ngắn hạn; refresh session lưu phía server, refresh token rotation và thu hồi khi logout/khóa tài khoản. Cookie refresh `HttpOnly`, `Secure`, `SameSite` theo deployment; CSRF protection phù hợp với cookie authentication. Phiên có hạn tuyệt đối 30 ngày, không gia hạn vô hạn. Cần chốt domain, cross-origin, CSRF strategy và OTP trước implementation.
