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

- V23 chuẩn hóa nội dung `TRANSFER_SUBMITTED` cũ theo phiếu chuyển/Số CFC; bổ sung bản thông báo lịch sử cho từng Admin đang hoạt động nếu thiếu theo dedupe key. Không xóa tin sai khâu hoặc đổi readAt đã có. Quyền xem theo loại được khóa ở service; không phụ thuộc việc xóa bản ghi cũ. Đã kiểm tra trên MySQL cô lập và triển khai DB vận hành ngày 25/11/2026.

- V22 bổ sung `samples.weight VARCHAR(80)` nullable (utf8mb4), độc lập với `samples.quantity`. Không backfill từ quantity vì số lượng/đơn vị cũ có thể không phải khối lượng. API nhận mẫu cũ bỏ weight vẫn hợp lệ; snapshot phiếu chuyển dùng weight riêng. Migration đã kiểm tra trên MySQL cô lập; chưa chạy trên DB vận hành.

- V1–V5 thiết lập nền, dữ liệu tiếp nhận, chuyển mẫu, thông báo và các chỉ mục truy vấn.
- V6–V10 bổ sung hồ sơ kết quả, lịch sử mã, phiên bản phát hành, file và nội dung phiếu.
- V11–V15 bổ sung yêu cầu, kế hoạch, phiếu lấy mẫu và điều chỉnh cấu trúc tiếp nhận. V14 đồng bộ số kế hoạch và phiếu lấy mẫu theo số yêu cầu.
- V16–V18 bảo vệ số tham chiếu tiếp nhận, thêm danh mục phương pháp và chuẩn hóa số yêu cầu.
- Không có migration mới cho luồng nhập kết quả: dùng lại `testing_records`, `result_releases`, `result_release_items`. `result_releases.result_number` (unique) giờ lấy Số/No. của phiếu tiếp nhận; ảnh mẫu đọc từ `receipt_attachments`.
- V19 thêm `staff_notifications.actor_id` có thể null, tham chiếu `users.id` và `ON DELETE SET NULL`. Tin cũ giữ nguyên. Query danh sách tải actor cùng bản ghi để phục vụ avatar qua API.

Thiết kế tổng thể thông báo khách và các phần mở rộng Zalo/SMS nằm ở [INTAKE_NOTIFICATIONS.md](INTAKE_NOTIFICATIONS.md).

V20 bổ sung phần thử nghiệm Zalo: `receipt_notification_consents` giữ đích nhận/đồng ý theo phiếu; `customer_notification_outbox` giữ snapshot và unique sự kiện theo phiếu; `notification_delivery_attempts` giữ từng lần xử lý, tracking/provider ID và audit retry; `customer_notification_test_budgets` giữ cap bền vững theo session. Worker dùng JDBC tham gia transaction tạo phiếu hiện có, không gọi mạng trong transaction đó. Không có token trong các bảng này. Thời điểm metadata mới lưu UTC trong `DATETIME(6)`, đọc UTC rồi hiển thị giờ Việt Nam. Mặc định tắt gửi; xem [hướng dẫn test](ZALO_INTAKE_TESTING.md).

V21 thêm bằng chứng giao/đối soát vào `notification_delivery_attempts`; `zalo_delivery_inbox` chống trùng callback và chờ khớp, chỉ lưu recipient hash; `zalo_managed_tokens` giữ cặp token AES-GCM/expiry/lease theo App/OA (khóa ngoài DB); `customer_notification_controls` giữ pause bền vững; `customer_notification_operation_audit` ghi thao tác pause/resume/cấp token bằng fingerprint, không chứa token. Metadata mới dùng UTC `DATETIME(6)`. Chưa có tự xóa theo retention; xem [runbook](ZALO_OPERATIONS_RUNBOOK.md).
