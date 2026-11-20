# Dữ liệu CFC Lab

MySQL 8.4 và `utf8mb4`. Flyway chạy SQL versioned ở `source/be/src/main/resources/db/migration/`, Hibernate dùng `validate`. Các migration đã phát hành được giữ nguyên. Để nâng môi trường có dữ liệu, sao lưu và kiểm tra `flyway_schema_history` trước khi khởi động phiên bản mới.

## Nhóm bảng

| Nhóm | Dữ liệu |
|---|---|
| Người dùng | `users`, phiên refresh phía server và metadata avatar. Ảnh hiện hành lưu trên hệ thống file |
| Yêu cầu và lấy mẫu | `test_requests`, dòng chỉ tiêu, `sampling_plans`, `field_sampling_forms`, `field_sampling_items`, bộ đếm số yêu cầu |
| Tiếp nhận | Khách, phiếu, mẫu, file bằng chứng, lịch sử mã và thông tin nguồn |
| Bàn giao | `sample_transfers`, `sample_transfer_items`, file bàn giao, người gửi và người nhận |
| Thử nghiệm | `testing_records`, thiết bị/hóa chất/số liệu cho hồ sơ cũ và danh mục phương pháp |
| Kết quả | `result_releases`, kết quả từng dòng cũ, file gốc/PDF, lịch sử, bộ đếm phát hành và audit tra cứu |
| Thông báo nội bộ | `staff_notifications`, người nhận, loại sự kiện, thời điểm đọc và người thực hiện |

File hồ sơ lưu ở thư mục cấu hình của backend. Database giữ bản ghi, metadata và checksum khi áp dụng. Kế hoạch sao lưu cần bao gồm cả database và file.

## Migration nghiệp vụ

- V1–V5 thiết lập nền, dữ liệu tiếp nhận, chuyển mẫu, thông báo và các chỉ mục truy vấn.
- V6–V10 bổ sung hồ sơ kết quả, lịch sử mã, phiên bản phát hành, file và nội dung phiếu.
- V11–V15 bổ sung yêu cầu, kế hoạch, phiếu lấy mẫu và điều chỉnh cấu trúc tiếp nhận. V14 đồng bộ số kế hoạch và phiếu lấy mẫu theo số yêu cầu.
- V16–V18 bảo vệ số tham chiếu tiếp nhận, thêm danh mục phương pháp và chuẩn hóa số yêu cầu.
- V19 thêm `staff_notifications.actor_id` có thể null, tham chiếu `users.id` và `ON DELETE SET NULL`. Tin cũ giữ nguyên. Query danh sách tải actor cùng bản ghi để phục vụ avatar qua API.

Thông báo Zalo/SMS và các bảng outbox dành cho khách đang được thiết kế ở [INTAKE_NOTIFICATIONS.md](INTAKE_NOTIFICATIONS.md).
