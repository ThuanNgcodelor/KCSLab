# Role plan — Admin đối với khâu tiếp nhận

**Trạng thái 15/11/2026:** User/RBAC, dashboard dữ liệu thật, notification và profile đã có; catalog, approval/coding gate, resend ZBS/SMS và result release còn plan.

**Role:** `ADMIN` · **Landing:** `/admin`

## Yêu cầu dashboard tổng thể — 16/11/2026

- Dashboard Admin bao quát toàn bộ chuỗi: tiếp nhận Phiếu 3, chuyển/nhận Phiếu 4 và thử nghiệm Phiếu 5, có bộ lọc ngày/tuần/tháng và khoảng tùy chọn theo giờ Việt Nam.
- Tách số phiếu và số mẫu; hiển thị hàng đợi chờ chuyển, chờ KCS nhận, đã nhận/từ chối và tiến độ thử nghiệm theo trạng thái đã có trong hệ thống.
- Điều hướng từ số liệu tới danh sách được lọc tương ứng; có loading, lỗi và trạng thái rỗng. Tổng hợp tại backend, không đếm từ một trang dữ liệu.
- Phần kết quả phát hành, gửi khách, danh mục duyệt hoặc SLA chỉ hiển thị khi có dữ liệu và quy tắc tương ứng; không giả lập là đã triển khai.
- Admin và Technician được lập Phiếu 5; plan bổ sung upload nhiều file tại `plans/intake/TECHNICIAN_KCS.md`.
- Hiện dashboard Admin chủ yếu tổng hợp tiếp nhận, chưa phản ánh toàn chuỗi như yêu cầu mới.

## Mục tiêu công việc

Giám sát năng lực tiếp nhận, xử lý ngoại lệ, chuẩn hóa danh mục và kiểm soát notification mà không thay nhân viên nhập liệu thường ngày.

## Dashboard quản trị tiếp nhận

- Filter khoảng ngày, nhân viên tiếp nhận, kết luận, trạng thái và loại sản phẩm.
- KPI thật từ aggregate API: tổng phiếu/mẫu, tỷ lệ có điều kiện/từ chối, chờ chuyển KCS, quá hạn dự kiến, delivery ZBS/SMS lỗi.
- Workload theo nhân viên: số phiếu, số mẫu, nháp tồn, ngoại lệ; không dùng dữ liệu hard-code.
- Danh sách cần can thiệp: loại sản phẩm chờ duyệt, phiếu có điều kiện, lỗi ảnh/file, notification thất bại, transfer bị từ chối.
- Activity feed có phân trang và audit actor thật.

## Chức năng quản trị

- Xem mọi Phiếu 3 và filter theo `receivedBy`.
- Sửa/đính chính theo policy và lý do; không ghi đè lịch sử đã khóa.
- Duyệt, merge, đổi tên hoặc vô hiệu hóa loại sản phẩm do Receptionist đề xuất.
- Cấu hình catalog chỉ tiêu/bao bì nếu chuyển thành danh mục quản trị.
- Resend customer notification ở trạng thái lỗi cuối cùng; bắt buộc lý do.
- Xem health WebSocket, hàng đợi notification và lỗi provider ở mức vận hành; secret không hiển thị.
- Theo dõi KCS cố định; không có màn quản lý nhiều phòng trong phạm vi hiện tại.

## Notification nhận được

- Phiếu tiếp nhận có điều kiện hoặc bị từ chối.
- Loại sản phẩm mới chờ duyệt.
- Notification khách hàng lỗi cuối/quota/credential bất thường.
- Phiếu 4 bị KCS từ chối hoặc hồ sơ vượt SLA.
- WebSocket không tạo notification riêng cho mọi receipt bình thường nếu dashboard đã phản ánh; có thể cấu hình mức độ để tránh quá tải.

## Quyền và separation of duties

- Admin có quyền vận hành nhưng không mặc định được bỏ qua gate hai người.
- Nếu Admin là người tạo/gán mã, một user đủ quyền khác phải xác minh.
- Chỉ Admin quản lý catalog và resend; mọi action ghi audit.
- Dashboard không trả dữ liệu bằng cách tải toàn bộ receipt rồi đếm ở frontend.

## Acceptance criteria

- Số KPI khớp aggregate query MySQL theo cùng khoảng ngày/filter.
- Có thể truy từ KPI ngoại lệ đến danh sách đã filter tương ứng.
- Không còn constants `stats/activity` trong dashboard runtime.
- Admin không đọc/mark-read notification của user khác qua API, trừ API audit riêng được thiết kế rõ.
- Duyệt loại sản phẩm không làm thay đổi mã mẫu đã phát hành; correction phải có lịch sử.
