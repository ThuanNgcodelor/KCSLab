# Role plan — Nhân viên tiếp nhận mẫu

**Role:** `RECEPTIONIST` · **Landing:** `/admin/intake`

## Mục tiêu công việc

Giúp nhân viên hoàn tất một Phiếu 3 nhanh, đủ bằng chứng và biết ngay hồ sơ nào cần xử lý tiếp mà không phải đi qua dashboard quản trị.

## Dashboard cá nhân

- Bộ lọc mặc định `Hôm nay`; cho chọn khoảng ngày.
- KPI từ API: đã ghi nhận, chờ hoàn tất, có điều kiện, bị từ chối, chờ lập/gửi Phiếu 4.
- Hàng đợi ưu tiên: nháp gần nhất, hồ sơ thiếu ảnh/file, hạn dự kiến hôm nay, Phiếu 4 bị KCS từ chối.
- Hoạt động chỉ trong phạm vi user/phân công; không hiện thống kê nhân sự toàn hệ thống.
- Quick action: `Tạo phiếu tiếp nhận` là action chính; `Mở phiếu nháp gần nhất` là action phụ.

## Luồng thao tác chuẩn

1. Tìm khách cũ bằng số điện thoại/tên; nếu không có thì tạo thông tin khách mới.
2. Ghi người giao, thời điểm, nguồn/lô và bằng chứng ban đầu.
3. Thêm một hoặc nhiều mẫu; nhập tên tự do, chọn/gợi ý loại sản phẩm.
4. Chọn bao bì, niêm phong, tình trạng và điều kiện vận chuyển theo Phiếu 3.
5. Chọn chỉ tiêu, hạn dự kiến và yêu cầu đặc biệt.
6. Chụp Phiếu 3, mẫu và lô hàng; xem preview và retry file lỗi.
7. Lưu nháp hoặc hoàn tất; có điều kiện/từ chối bắt buộc lý do.
8. Sau hoàn tất, xem mã phiếu và trạng thái gửi thông báo khách hàng.
9. Lập Phiếu 4 và gửi KCS khi gate mã hóa cho phép.

## Notification nhận được

- Customer notification gửi thất bại/cần bổ sung số điện thoại.
- Admin yêu cầu bổ sung hồ sơ.
- Phiếu 4 được KCS nhận hoặc từ chối.
- Mẫu/hồ sơ gần quá hạn xử lý.

Không tạo notification “bạn vừa tạo phiếu” cho chính thao tác đã có success screen/toast; tránh gây nhiễu.

## Quyền

- Tạo/sửa nháp/hoàn tất Phiếu 3 trong phạm vi.
- Xem notification và trạng thái delivery liên quan phiếu của mình.
- Đề xuất loại sản phẩm mới nhưng không duyệt/cấp product code.
- Không quản lý provider/template, resend customer notification, user hoặc số liệu toàn hệ thống.
- Không tự phê duyệt bước độc lập do chính mình thực hiện khi Phase 2 được bật.

## Acceptance criteria

- Từ dashboard mở form mới trong một thao tác.
- Filter/search không xử lý mảng receipt ở frontend.
- Form mobile chụp ảnh trực tiếp, không tràn ngang và giữ draft khi mạng gián đoạn ngắn.
- Nhân viên biết rõ file nào upload lỗi và retry riêng, không phải tạo lại phiếu.
- Mọi thao tác hoàn tất ghi actor/thời điểm; không gửi khách khi phiếu còn draft.
