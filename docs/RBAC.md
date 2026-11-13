# RBAC

**Phiên bản:** 0.3 · **Cập nhật:** 13/11/2026

## Enforcement hiện tại

- Backend `/api/v1/users/**`: `ADMIN`.
- Backend `/api/v1/receipts/**`: `ADMIN` hoặc `RECEPTIONIST`.
- Frontend `/admin/*`: hiện chỉ cho `ADMIN`; đây là khoảng trống cần sửa khi mở giao diện cho Receptionist/Technician.
- Các quyền transfer/testing/review/release bên dưới vẫn là target và cần LAB xác nhận.

Ký hiệu: `✓` được đề xuất, `—` không được phép mặc định, `?` cần xác nhận theo phạm vi/ủy quyền.

| Hành động | ADMIN | RECEPTIONIST | TECHNICIAN |
|---|:---:|:---:|:---:|
| Xem phiếu/mẫu trong phạm vi | ✓ | ✓ | ✓ |
| Tạo khách hàng/phiếu/mẫu | ✓ | ✓ | — |
| Sửa dữ liệu tiếp nhận trước khóa | ✓ | ✓ | — |
| Lưu file phiếu 3 và hồ sơ tiếp nhận | ✓ | ✓ | — |
| Sinh/gán mã mẫu CFC-TN-14 | ✓ | ✓ | — |
| Lập phiếu chuyển mẫu 4 | ✓ | ✓ | ? |
| Xác nhận bàn giao mẫu | ✓ | ✓ | ✓ theo phân công |
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

## Quyền tra cứu khách hàng

Khách không nằm trong RBAC nội bộ. Một access grant giới hạn theo phiếu/mẫu, mục đích, thời hạn và loại dữ liệu được xem. Tiến độ cơ bản và tài liệu phát hành phải là hai mức quyền riêng.

## Cần xác nhận

- Ai có thẩm quyền xác nhận nhận mẫu và duyệt/phát hành?
- Có cần role `REVIEWER` hoặc `LAB_MANAGER` ngoài ba role hiện tại không?
- Phạm vi “được giao” là theo người, nhóm, loại mẫu hay đơn vị?
- Admin có bị tách khỏi quyền phê duyệt chuyên môn không?
