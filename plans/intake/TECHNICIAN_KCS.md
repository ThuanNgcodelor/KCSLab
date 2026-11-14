# Role plan — Kỹ thuật viên/phòng KCS

**Role:** `TECHNICIAN` · **Đích nhận mẫu duy nhất:** `KCS`

## Mục tiêu công việc

Nhận biết ngay Phiếu 4 mới được gửi, kiểm tra bàn giao và quyết định nhận/từ chối mà không truy cập dữ liệu khách hàng không cần thiết.

## Không gian làm việc

- Landing `/admin/transfers` với tab `Chờ nhận`, `Đã nhận`, `Đã từ chối`.
- Không có menu tạo Phiếu 3 hoặc dashboard tiếp nhận khách hàng.
- Không có dropdown phòng nhận; backend cố định/cấu hình `KCS`.
- Danh sách transfer phân trang server-side, search theo mã phiếu/mã mẫu và khoảng ngày.

## Notification nhận được

- `TRANSFER_SUBMITTED`: Phiếu 4 mới gửi đến KCS.
- `TRANSFER_UPDATED` chỉ khi thay đổi hợp lệ trước khi nhận và có ảnh hưởng đến mẫu.
- Cảnh báo transfer chờ nhận vượt SLA.

Click notification mở trực tiếp Phiếu 4. Notification chỉ chứa mã phiếu, số mẫu, người giao và thời điểm; không chứa phone/email/địa chỉ khách.

## Luồng nhận mẫu

1. WebSocket báo transfer mới; header cập nhật badge.
2. REST tải chi tiết Phiếu 4 và snapshot mẫu từ database.
3. Kỹ thuật viên đối chiếu mã/nhãn, số lượng, tình trạng và bằng chứng cần thiết.
4. Chọn nhận hoặc từ chối; từ chối bắt buộc lý do.
5. Backend cập nhật trạng thái transactionally và tạo notification cho Receptionist/Admin sau commit.

## Quyền

- Chỉ thấy Phiếu 4 từ `PENDING` trở đi; không thấy draft.
- Không xem mapping mã mù, customer contact hoặc hồ sơ nguồn đầy đủ nếu không cần kỹ thuật.
- Không sửa Phiếu 3, product catalog hoặc customer notification.
- Không tự chọn/chuyển sang phòng khác trong MVP.

## Acceptance criteria

- Transfer mới xuất hiện realtime nhưng refresh trang vẫn lấy đủ dữ liệu qua REST.
- Mất WebSocket không làm mất Phiếu 4; reconnect đồng bộ unread count và danh sách.
- Hai kỹ thuật viên cùng thao tác không thể quyết định một transfer hai lần; conflict trả HTTP 409.
- Receptionist nhận notification đúng một lần khi KCS nhận/từ chối.
