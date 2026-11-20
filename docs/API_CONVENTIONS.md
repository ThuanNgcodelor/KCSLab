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
| Kết quả | `/result-releases`, `POST /result-releases/{id}/report-file`, `GET /result-releases/{id}/report-preview`, `POST /result-releases/{id}/publish`, `POST /result-releases/{id}/revoke` |
| Tra cứu | `POST /public/results/lookup`, `POST /public/results/download` |

- `GET /test-requests/next-number` gợi ý số dạng `CFC26.0001`. Để trống khi tạo sẽ cấp số nguyên tử. Số yêu cầu đã tạo là bất biến, trùng trả 409. Hai biểu mẫu lấy mẫu dùng chung số yêu cầu.
- KCS đọc projection của phiếu đã gửi. File bằng chứng phải thuộc hồ sơ đã bàn giao. Quyết định được khóa theo phiếu, trả 409 nếu phiếu hết trạng thái đang chờ.
- Biên bản từ file dùng `transferItemId` của mẫu đã được nhận. Tải KQKN bằng multipart `file` chứa DOCX hoặc PDF. DOCX cần LibreOffice. Admin xem trước PDF rồi phát hành. Mã mẫu nhập tay hợp lệ được ghi nhận ở trạng thái sẵn sàng khi tiếp nhận.
- Tra cứu và tải dùng `code`, `phoneLast4`; thêm `fileId` khi tải. Chỉ trả hồ sơ đã phát hành. Có giới hạn truy cập và `Cache-Control: no-store`. Xem [triển khai trang tra cứu](PUBLIC_LOOKUP_SEO.md).

## Thông báo nội bộ

- `GET /notifications?page=0&size=20&unread=false` hỗ trợ tìm kiếm và lọc. Kết quả có `unreadCount` cho toàn bộ thông báo của người nhận.
- `PATCH /notifications/{id}` đánh dấu đã đọc và trả bản ghi cập nhật. `POST /notifications/read-all` đánh dấu tất cả của người đang đăng nhập.
- WebSocket `/ws/notifications` xác thực bằng cookie. Cùng cấu trúc bản ghi như REST. Các trường gồm `id`, `type`, `title`, `message`, `targetUrl`, `entityType`, `entityId`, `createdAt`, `readAt` và `actor`.
- `actor` có `displayName` và `avatarUrl`. Với tin lịch sử hoặc người thực hiện đã bị xóa, `actor` có thể là null. Tên và ảnh lấy từ hồ sơ hiện tại của người thực hiện.
- Bản ghi được ghi trong transaction nghiệp vụ. WebSocket gửi sau commit đến các tab của người nhận. REST dùng để đồng bộ khi kết nối lại hoặc mất kênh trực tiếp.
