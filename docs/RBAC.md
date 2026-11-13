# RBAC

**Phiên bản:** 0.3 · **Cập nhật:** 13/11/2026

## Enforcement hiện tại

- Backend `/api/v1/users/**`: `ADMIN`.
- Backend `/api/v1/receipts/**`: `ADMIN` hoặc `RECEPTIONIST`.
- Backend transfer đọc/tạo/quyết định: `ADMIN`, `RECEPTIONIST` hoặc `TECHNICIAN` theo method security hiện tại.
- Backend testing đọc/lập/cập nhật/submit: `ADMIN` hoặc `TECHNICIAN`; review: `ADMIN`.
- Frontend route intake mở cho `ADMIN|RECEPTIONIST`, testing cho `ADMIN|TECHNICIAN`, user management chỉ `ADMIN`.
- Quyền phát hành kết quả vẫn chưa triển khai và cần LAB xác nhận.

Ký hiệu: `✓` được đề xuất, `—` không được phép mặc định, `?` cần xác nhận theo phạm vi/ủy quyền.

| Hành động | ADMIN | RECEPTIONIST | TECHNICIAN |
|---|:---:|:---:|:---:|
| Xem phiếu/mẫu trong phạm vi | ✓ | ✓ | ✓ |
| Tạo khách hàng/phiếu/mẫu | ✓ | ✓ | — |
| Sửa dữ liệu tiếp nhận trước khóa | ✓ | ✓ | — |
| Lưu file phiếu 3 và hồ sơ tiếp nhận | ✓ | ✓ | — |
| Sinh/gán mã mẫu CFC-TN-14 | ✓ | ✓ | — |
| Xác minh mã mẫu của người khác | ✓ | ✓ theo phân công | — |
| Xem mapping mã mẫu–khách hàng | ✓ | ✓ | — mặc định |
| Xem bảng đối chiếu mã mù | ? | ? theo ủy quyền | — trước mở mù |
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

Người gán mã không được tự xác minh mã của chính mình. Mẫu chưa xác minh không được chuyển phòng thử nghiệm. Kỹ thuật viên chỉ dùng mã mẫu trên hồ sơ trung gian và dữ liệu thiết bị; thông tin khách/mapping mã gốc chỉ mở khi có quyền nghiệp vụ. Trưởng/Phó phòng QLCLSP kiểm soát, phê duyệt thay đổi và lưu hồ sơ mã hóa theo CFC-TN-14.

## Quyền tra cứu khách hàng

Khách không nằm trong RBAC nội bộ. Một access grant giới hạn theo phiếu/mẫu, mục đích, thời hạn và loại dữ liệu được xem. Tiến độ cơ bản và tài liệu phát hành phải là hai mức quyền riêng.

## Cần xác nhận

- Ai có thẩm quyền xác nhận nhận mẫu và duyệt/phát hành?
- Có cần role `REVIEWER` hoặc `LAB_MANAGER` ngoài ba role hiện tại không?
- Phạm vi “được giao” là theo người, nhóm, loại mẫu hay đơn vị?
- Admin có bị tách khỏi quyền phê duyệt chuyên môn không?
