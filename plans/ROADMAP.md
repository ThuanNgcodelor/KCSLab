# Roadmap CFC Lab Management

**Cập nhật:** 13/11/2026

| Giai đoạn | Phạm vi | Trạng thái |
|---|---|---|
| 0 — Phân tích | PRD, biểu mẫu 3–5, RBAC, kiến trúc | Hoàn thành nền tảng; câu hỏi nghiệp vụ vẫn cập nhật dần |
| 1 — Nền tảng | Spring/React/MySQL, auth, Admin Portal, user management | Hoàn thành |
| 2 — Tiếp nhận | Phiếu 3, customer, receipt, sample, mã CFC-TN-14, attachment | Hoàn thành vertical slice |
| 3 — Chuyển mẫu | Phiếu 4, custody, giao/nhận, trạng thái | Hoàn thành vertical slice |
| 4 — Thử nghiệm | Phiếu 5, phân công, thiết bị, hóa chất, QC, raw data | Hoàn thành vertical slice |
| 5 — Duyệt/phát hành | Review chuyên môn, version PDF, checksum, thu hồi/đính chính | Đang lập kế hoạch; bước kiểm tra biên bản đã có, release/PDF chưa có runtime |
| 6 — Tra cứu khách | Mã + đuôi điện thoại, tiến độ, vùng trồng, PDF | Chưa triển khai |
| 7 — Vận hành nâng cao | Audit, backup, import Excel, thông báo, tích hợp thiết bị | Chưa triển khai |

Ưu tiên kỹ thuật trước production: Flyway/Liquibase, integration tests, object storage + backup, audit log, rate limiting và quản lý secrets.

Ranh giới hiện tại: luồng nội bộ chạy đến `TEST_REVIEWED`; dashboard còn dữ liệu mẫu; chưa có kết quả PDF được phát hành hoặc cổng tra cứu khách hàng. Responsive Admin UI đã có contract chung trong `source/fe/AGENTS.md`.
