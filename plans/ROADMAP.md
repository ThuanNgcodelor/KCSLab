# Roadmap CFC Lab Management

**Cập nhật:** 13/11/2026

| Giai đoạn | Phạm vi | Trạng thái |
|---|---|---|
| 0 — Phân tích | PRD, biểu mẫu 3–5, RBAC, kiến trúc | Hoàn thành nền tảng; câu hỏi nghiệp vụ vẫn cập nhật dần |
| 1 — Nền tảng | Spring/React/MySQL, auth, Admin Portal, user management | Hoàn thành |
| 2 — Tiếp nhận | Phiếu 3, customer, receipt, sample, mã CFC-TN-14, attachment | Có vertical slice; hardening mã hóa được đưa vào gate 5A |
| 3 — Chuyển mẫu | Phiếu 4, custody, giao/nhận, trạng thái | Hoàn thành vertical slice |
| 4 — Thử nghiệm | Phiếu 5, phân công, thiết bị, hóa chất, QC, raw data | Hoàn thành vertical slice |
| 5 — Mã hóa hoàn chỉnh & phát hành | Xác minh mã, `-V/-R`, QC/mã mù, review chuyên môn, version PDF/checksum | 5A tiếp theo; release/PDF chưa có runtime |
| 6 — Tra cứu khách | Mã + đuôi điện thoại, tiến độ, vùng trồng, PDF | Chưa triển khai |
| 7 — Vận hành nâng cao | Audit, backup, import Excel, thông báo, tích hợp thiết bị | Chưa triển khai |

Ưu tiên kỹ thuật trước production: Flyway/Liquibase, integration tests, object storage + backup, audit log, rate limiting và quản lý secrets.

Ranh giới hiện tại: luồng nội bộ chạy đến `TEST_REVIEWED`, nhưng mã hóa mới có mã gốc và chưa đủ quy trình CFC-TN-14. Hoàn thành `docs/SAMPLE_CODING.md` là prerequisite trước PDF; dashboard còn dữ liệu mẫu; chưa có kết quả phát hành hoặc cổng tra cứu khách hàng.
