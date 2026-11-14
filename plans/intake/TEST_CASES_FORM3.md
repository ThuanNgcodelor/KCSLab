# Bộ case nghiệm thu Phiếu tiếp nhận và bàn giao mẫu số 3

## Chuẩn bị

- Dùng tài khoản `RECEPTIONIST` đã được Admin tạo.
- Bật MySQL, Redis Laragon, backend `:8080`, frontend `:5173`.
- Mỗi case tạo dữ liệu với mã phiếu yêu cầu riêng để tránh trùng.
- Không dùng lại số điện thoại khách hàng nếu muốn kiểm tra tạo khách hàng mới.

## A. Happy path

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| F3-001 | Mở `/admin/intake/new` | Form hiển thị đủ 5 nhóm: người giao, nguồn/lô, mẫu, điều kiện/kết luận, ảnh/hồ sơ |
| F3-002 | Nhập khách hàng, điện thoại, số phiếu yêu cầu, người giao | Lưu được, người nhận tự lấy từ tài khoản đăng nhập |
| F3-003 | Thêm 1 mẫu với tên, loại sản phẩm nhập tay, mã nguồn, số lượng, bao bì, tình trạng, chỉ tiêu | Mẫu được lưu và sinh mã duy nhất |
| F3-004 | Chọn niêm phong `Còn nguyên` | API lưu `sealStatus=INTACT`, tương thích `sealIntact=true` |
| F3-005 | Upload Phiếu 3, ảnh mẫu, ảnh lô hàng | File gắn đúng phiếu, có checksum và người upload |
| F3-006 | Chọn `Chấp nhận` và lưu | Phiếu có trạng thái `RECEIVED`, hiển thị mã phiếu và mã mẫu |
| F3-007 | Đăng nhập Admin/KCS khác | Nhận thông báo `INTAKE_RECORDED` trong header và trang thông báo; bấm vào mở đúng phiếu |

## B. Trạng thái niêm phong

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| F3-008 | Chọn `Không có` | API lưu `sealStatus=NOT_PRESENT` |
| F3-009 | Chọn `Đã mở` | API lưu `sealStatus=OPENED` |
| F3-010 | Không chọn trạng thái niêm phong rồi gọi API | HTTP 400, không tạo phiếu |

## C. Validation bắt buộc

| ID | Bỏ trống | Kết quả mong đợi |
|---|---|---|
| F3-011 | Số điện thoại khách hàng | HTTP 400 |
| F3-012 | Số Phiếu yêu cầu | HTTP 400 |
| F3-013 | Người giao | HTTP 400 |
| F3-014 | Tên mẫu | HTTP 400 |
| F3-015 | Loại sản phẩm | HTTP 400 |
| F3-016 | Mã nguồn | HTTP 400 |
| F3-017 | Chỉ tiêu yêu cầu | HTTP 400 |
| F3-018 | Danh sách mẫu rỗng | HTTP 400 |

## D. Kết luận tiếp nhận

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| F3-019 | Chọn `Chấp nhận có điều kiện`, bỏ lý do | HTTP 400, không tạo phiếu |
| F3-020 | Chọn `Từ chối`, bỏ lý do | HTTP 400, không tạo phiếu |
| F3-021 | Chọn `Chấp nhận có điều kiện` và nhập điều kiện | Tạo thành công, trạng thái `ACCEPTED_WITH_CONDITION` |
| F3-022 | Chọn `Từ chối` và nhập lý do | Tạo thành công, trạng thái `REJECTED`, không được chuyển mẫu |

## E. Nhiều mẫu và truy xuất

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| F3-023 | Thêm 2 mẫu trong cùng phiếu | Một phiếu, hai mã mẫu, cùng actor tiếp nhận |
| F3-024 | Tìm theo mã phiếu yêu cầu | Chỉ trả đúng phiếu |
| F3-025 | Tìm theo mã mẫu/khách hàng | Kết quả phân trang server-side, tối đa 20 phiếu/trang |
| F3-026 | Mở chi tiết phiếu | Hiện request number, seal status, người nhận, mẫu, file và lịch sử trạng thái |

## F. Phân quyền và thông báo dùng chung

| ID | Thao tác | Kết quả mong đợi |
|---|---|---|
| F3-027 | Receptionist tạo phiếu | Thành công; không thấy menu Admin-only |
| F3-028 | Technician đăng nhập | Nhận notification tiếp nhận; không được sửa/tạo phiếu nếu policy không cho phép |
| F3-029 | Người nhận phiếu tạo phiếu | Không nhận thông báo của chính mình; Admin và KCS nhận thông báo |
| F3-030 | Mở thông báo trên mobile | Header hiển thị 3 tin mới nhất, `Xem tất cả` mở trang thông báo |
| F3-031 | Đánh dấu đã đọc | Badge giảm; trạng thái `readAt` được lưu server-side |

## G. Responsive và ảnh

| ID | Kích thước | Kết quả mong đợi |
|---|---|---|
| F3-032 | PC 1440px | Không tràn ngang, form hai cột hợp lý |
| F3-033 | Tablet 1024px | Form chuyển bố cục phù hợp, thao tác được |
| F3-034 | Điện thoại 390px | Không tràn ngang, input/file picker dùng được |
| F3-035 | Upload ảnh lớn | Frontend thu nhỏ, backend từ chối file quá giới hạn, không lưu ảnh gốc |

## Tiêu chí pass

Tất cả F3-001 đến F3-035 đạt; không có HTTP 500, không tạo bản ghi một phần khi validation thất bại, notification không trùng và mọi truy vấn danh sách vẫn phân trang server-side.
