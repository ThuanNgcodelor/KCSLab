# RBAC sơ bộ

**Phiên bản:** 0.1 · **Trạng thái:** Đề xuất, cần LAB xác nhận

Ký hiệu: `✓` được đề xuất, `—` không được phép mặc định, `?` cần xác nhận theo phạm vi/ủy quyền.

| Hành động | ADMIN | RECEPTIONIST | TECHNICIAN |
|---|:---:|:---:|:---:|
| Xem phiếu/mẫu trong phạm vi | ✓ | ✓ | ✓ |
| Tạo khách hàng/phiếu/mẫu | ✓ | ✓ | — |
| Sửa dữ liệu tiếp nhận trước khóa | ✓ | ✓ | — |
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
