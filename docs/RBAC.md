# RBAC

## Workspace KCS đã triển khai, chờ nghiệm thu E2E — 16/11/2026

- Backend `/api/v1/receiving/**` cho `ADMIN|TECHNICIAN`; `RECEPTIONIST` bị từ chối. `TECHNICIAN` vẫn không có quyền đọc `/api/v1/receipts/**`.
- DTO receiving không có trường `customerName`, phone/email/address; file gốc và tên/mô tả file vẫn có thể chứa thông tin khách hàng. Nội dung file chỉ được tải sau khi xác minh attachment thuộc receipt của transfer đã gửi.
- API `/api/v1/transfers/**` cũ được giữ để không phá luồng Admin/Receptionist; frontend điều hướng Technician sang workspace KCS. Với Technician, field `customerName` trong DTO cũ được trả `null`; Admin/Receptionist vẫn nhận giá trị hiện có. File gốc và các trường ghi chú tự do vẫn có thể chứa thông tin khách hàng.
- Chưa nghiệm thu end-to-end trên Chrome với backend đang chạy; chưa kiểm tra tải thực tế và backup file.

## Quyết định role Người nhận mẫu KCS — 16/11/2026

- Người dùng xác nhận giữ `TECHNICIAN` cho cả nhận/từ chối Phiếu 4 và lập Phiếu 5; `ADMIN` cũng được lập Phiếu 5. Cả hai cần upload nhiều file PDF và tài liệu khác vào Phiếu 5; đây là yêu cầu chưa triển khai.
- `RECEPTIONIST`, `TECHNICIAN` và `ADMIN` có dashboard riêng theo phạm vi công việc; Admin bao quát toàn hệ thống.
- Người nhận mẫu được xem đầy đủ file gốc của hồ sơ được chuyển giao thông qua quyền theo transfer; file gốc có thể chứa thông tin khách hàng. API projection vẫn hạn chế trả riêng các trường liên hệ khi không cần thiết và không mở toàn bộ `/api/v1/receipts/**`.
- Quy tắc thiếu ảnh tạm đề xuất cảnh báo, chưa có quy định bắt buộc từ LAB; chưa áp đặt SLA. Plan chi tiết: `plans/intake/TECHNICIAN_KCS.md`.

## Ghi chú hiển thị người tiếp nhận — 15/11/2026

- `receivedBy` là nhân viên đăng nhập tạo Phiếu 3, khác với `deliveredBy` là người giao mẫu được nhập trong phiếu.
- `ADMIN` và `RECEPTIONIST` được xem đầy đủ Phiếu 3 và người tiếp nhận.
- `TECHNICIAN/KCS` không được gọi API `/api/v1/receipts/**` theo security hiện tại. Notification `INTAKE_RECORDED` có thể hiển thị tên người ghi nhận để giữ chuỗi trách nhiệm; đây là projection tối thiểu, không phải quyền xem toàn bộ hồ sơ.
- Trang tra cứu public tương lai không được trả `receivedBy`, `deliveredBy`, email, phone đầy đủ hoặc mapping thông tin nội bộ.

**Phiên bản:** 0.5 · **Cập nhật:** 14/11/2026

## Enforcement hiện tại

- Backend `/api/v1/users/**`: `ADMIN`.
- Backend `/api/v1/receipts/**`: `ADMIN` hoặc `RECEPTIONIST`.
- Backend transfer: `ADMIN|RECEPTIONIST` tạo/sửa/gửi; `ADMIN|TECHNICIAN` quyết định; cả ba role được đọc nhưng service ẩn `DRAFT` khỏi `TECHNICIAN`.
- Backend testing đọc/lập/cập nhật/submit: `ADMIN` hoặc `TECHNICIAN`; review: `ADMIN`.
- Frontend route intake mở cho `ADMIN|RECEPTIONIST`, testing cho `ADMIN|TECHNICIAN`, user management chỉ `ADMIN`.
- Quyền phát hành kết quả vẫn chưa triển khai và cần LAB xác nhận.
- Notification chưa có runtime: đề xuất Receptionist nhập/xác nhận consent và xem delivery của phiếu trong phạm vi; chỉ Admin cấu hình provider/template hoặc resend lỗi cuối cùng.

Ký hiệu: `✓` được đề xuất, `—` không được phép mặc định, `?` cần xác nhận theo phạm vi/ủy quyền.

| Hành động | ADMIN | RECEPTIONIST | TECHNICIAN |
|---|:---:|:---:|:---:|
| Xem phiếu/mẫu trong phạm vi | ✓ | ✓ | ✓ |
| Tạo khách hàng/phiếu/mẫu | ✓ | ✓ | — |
| Sửa dữ liệu tiếp nhận trước khóa | ✓ | ✓ | — |
| Lưu file phiếu 3 và hồ sơ tiếp nhận | ✓ | ✓ | — |
| Xác nhận số nhận tin/kênh/consent trước hoàn tất Phiếu 3 | ✓ | ✓ | — |
| Xem trạng thái ZBS/SMS của Phiếu 3 trong phạm vi | ✓ | ✓ | — |
| Gửi lại notification đã lỗi cuối cùng, có lý do | ✓ | — | — |
| Quản lý provider/template/credential notification | ✓ cấu hình; secret không hiển thị | — | — |
| Dashboard tiếp nhận cá nhân | ✓ | ✓ | — |
| Dashboard tiếp nhận tổng hợp/workload | ✓ | — | — |
| Xem/đánh dấu notification nội bộ của chính mình | ✓ | ✓ | ✓ |
| Đề xuất loại sản phẩm mới | ✓ | ✓ | — |
| Duyệt/chuẩn hóa loại sản phẩm | ✓ | — | — |
| Sinh/gán mã mẫu CFC-TN-14 | ✓ | ✓ | — |
| Xác minh mã mẫu của người khác | ✓ | ✓ theo phân công | — |
| Xem mapping mã mẫu–khách hàng | ✓ | ✓ | — mặc định |
| Xem bảng đối chiếu mã mù | ? | ? theo ủy quyền | — trước mở mù |
| Lập/sửa/gửi phiếu chuyển mẫu 4 | ✓ | ✓ | — |
| Xem phiếu chuyển đã gửi | ✓ | ✓ | ✓ |
| Xem phiếu chuyển còn nháp | ✓ | ✓ | — |
| Xác nhận nhận/từ chối bàn giao | ✓ | — | ✓ theo phân công |
| Lập biên bản thử nghiệm 5 | ✓ | — | ✓ |
| Quản lý thiết bị/hóa chất/raw data thử nghiệm | ✓ | — | ✓ |
| Xóa dữ liệu nghiệp vụ | ? | — | — |
| Xác nhận đã nhận mẫu | ? | ? | — |
| Chuyển sang đang thử nghiệm | ✓ | ? | ? |
| Nhập/cập nhật kết quả nháp | ? | — | ✓ |
| Kiểm tra kết quả | ? | — | ? |
| Duyệt kết quả | ? | — | ? |
| Phát hành kết quả | ? | — | — |
| Quản lý khách hàng | ✓ | ✓ theo phạm vi | — |
| Quản lý tài khoản nhân viên (tạo bằng email, khóa/mở khóa) | ✓ | — | — |
| Gán role/khóa tài khoản | ✓ | — | — |
| Quản lý danh mục/cấu hình | ✓ | — | — |
| Xem audit log | ✓ | ? | ? |

Nguyên tắc: người nhập không mặc định được duyệt kết quả của chính mình; quyền chuyên môn của Admin không tự động được giả định; không xóa vật lý dữ liệu đã tạo lịch sử, ưu tiên vô hiệu hóa/đính chính có audit.

Người gán mã không được tự xác minh mã của chính mình. Mẫu chưa xác minh không được chuyển phòng thử nghiệm. Kỹ thuật viên chỉ dùng mã mẫu trên hồ sơ trung gian và dữ liệu thiết bị; thông tin khách/mapping mã gốc chỉ mở khi có quyền nghiệp vụ. Trưởng/Phó phòng QLCLSP kiểm soát, phê duyệt thay đổi và lưu hồ sơ mã hóa theo CFC-TN-14.

Notification nội bộ thuộc về từng recipient; API list/read không cho Admin tùy ý đánh dấu tin của user khác. KCS hiện là một phòng duy nhất, không có quyền chọn hoặc đổi `receivingDepartment`; KCS chỉ nhận event Phiếu 4 đã submit và payload không chứa phone/email/địa chỉ khách.

## Quyền tra cứu khách hàng

Khách không nằm trong RBAC nội bộ. Một access grant giới hạn theo phiếu/mẫu, mục đích, thời hạn và loại dữ liệu được xem. Tiến độ cơ bản và tài liệu phát hành phải là hai mức quyền riêng.

## Cần xác nhận

- Ai có thẩm quyền xác nhận nhận mẫu và duyệt/phát hành?
- Có cần role `REVIEWER` hoặc `LAB_MANAGER` ngoài ba role hiện tại không?
- Phạm vi “được giao” là theo người, nhóm, loại mẫu hay đơn vị?
- Admin có bị tách khỏi quyền phê duyệt chuyên môn không?
