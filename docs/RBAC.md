# Phân quyền

| Thao tác | Admin | Tiếp nhận | KCS |
|---|---|---|---|
| Quản lý tài khoản và danh mục phương pháp thử | Có | — | — |
| Tự sửa hồ sơ, avatar, mật khẩu | Có | Có | Có |
| Lập yêu cầu, kế hoạch lấy mẫu, lấy mẫu hiện trường | Có | Có | — |
| Tiếp nhận và nhập mã mẫu | Có | Có | — |
| Hiệu chỉnh mã mẫu đã ghi nhận | Có | — | — |
| Lập/sửa/gửi phiếu chuyển mẫu | Có | Có | — |
| Xem phiếu chuyển đã gửi và ảnh hồ sơ bàn giao | Có | Theo phạm vi | Có |
| Nhận/từ chối bàn giao | Có | — | Có |
| Tải file kết quả, lập biên bản và hồ sơ kết quả | Có | — | Có |
| Phát hành, thu hồi kết quả | Có | — | — |
| Xem và đánh dấu thông báo của mình | Có | Có | Có |

Spring Security kiểm tra role theo route. Service kiểm tra các điều kiện nghiệp vụ, quyền chỉnh sửa hồ sơ đã hoàn tất và phạm vi dữ liệu. Menu frontend phản ánh quyền để dễ thao tác.

KCS xem dữ liệu qua projection và bằng chứng gắn với phiếu đã gửi. File gốc có thể chứa thông tin do Tiếp nhận ghi vào hồ sơ. Bản ghi thông báo của người khác bị giới hạn phía server. Avatar nhân viên chỉ trả qua API cần đăng nhập.

Khách truy cập hai thao tác public để tra và tải PDF đã phát hành với mã tra cứu và bốn số cuối điện thoại.
