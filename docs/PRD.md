# Phạm vi sản phẩm CFC Lab

CFC Lab quản lý lộ trình mẫu từ yêu cầu kiểm nghiệm đến file kết quả được phát hành. Hệ thống lưu mối liên kết giữa các chứng từ, người thực hiện, thời gian, bằng chứng tiếp nhận và bản PDF gửi khách.

- **Tiếp nhận** tạo yêu cầu, ghi thông tin lấy mẫu khi có, nhập mã mẫu và bàn giao.
- **KCS** đối chiếu hồ sơ, ghi quyết định nhận/từ chối, tải kết quả hoàn chỉnh.
- **Admin** quản lý nhân sự và danh mục, phát hành hoặc thu hồi phiếu.
- **Khách hàng** tra cứu kết quả đã phát hành bằng mã tra cứu và bốn số cuối số điện thoại.
- **Nhân viên** theo dõi thông báo và tự quản lý hồ sơ cá nhân. Có thể cài app web lên màn hình chính từ bản HTTPS.

Biểu mẫu gốc nằm trong [template](template/). Xem [hướng dẫn vận hành](DOC_HIEU_HE_THONG.md), [phân quyền](RBAC.md) và [trạng thái triển khai](SYSTEM_STATUS.md).

## Mở rộng đang xem xét

- Gửi [thông báo Zalo/SMS](INTAKE_NOTIFICATIONS.md) sau khi LAB chốt nhà cung cấp, biểu mẫu tin nhắn và nội dung đồng ý nhận tin.
- Chốt hạ tầng lưu trữ, sao lưu và [triển khai trang tra cứu](PUBLIC_LOOKUP_SEO.md).
