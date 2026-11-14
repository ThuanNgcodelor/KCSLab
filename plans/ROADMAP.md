# Roadmap CFC Lab Management

**Cập nhật:** 14/11/2026

| Giai đoạn | Phạm vi | Trạng thái |
|---|---|---|
| 0 — Phân tích | PRD, biểu mẫu 3–5, RBAC, kiến trúc | Hoàn thành nền tảng; câu hỏi nghiệp vụ vẫn cập nhật dần |
| 1 — Nền tảng | Spring/React/MySQL, auth, Admin Portal, user management | Hoàn thành |
| 2 — Tiếp nhận | Phiếu 3, customer, receipt, sample, mã CFC-TN-14, attachment | Có vertical slice; hardening mã hóa được đưa vào gate 5A |
| 2A — Workspace Tiếp nhận | Dashboard thật, query server-side, header/notification, WebSocket và form Phiếu 3 mới | Plan I1–I6 đã chốt; ưu tiên triển khai |
| 2B — Thông báo tiếp nhận | Consent, ZBS Template Message, SMS Brandname/MultiSMS, outbox/delivery và UI trạng thái | Ưu tiên mới; đã chốt plan, chưa có runtime |
| 3 — Chuyển mẫu | Phiếu 4, custody, giao/nhận, trạng thái | Hoàn thành vertical slice |
| 4 — Thử nghiệm | Phiếu 5, phân công, thiết bị, hóa chất, QC, raw data | Hoàn thành vertical slice |
| 5 — Mã hóa hoàn chỉnh & phát hành | Xác minh mã, `-V/-R`, QC/mã mù, review chuyên môn, version PDF/checksum | 5A tiếp theo; release/PDF chưa có runtime |
| 6 — Tra cứu khách | Mã + đuôi điện thoại, tiến độ, vùng trồng, PDF | Chưa triển khai |
| 7 — Vận hành nâng cao | Audit, backup, import Excel, dashboard/notification nâng cao, tích hợp thiết bị | Chưa triển khai |

Ưu tiên kỹ thuật trước production: Flyway/Liquibase, integration tests, object storage + backup, audit log, rate limiting và quản lý secrets.

Ranh giới hiện tại: luồng nội bộ chạy đến `TEST_REVIEWED`, nhưng dashboard còn dữ liệu mẫu, chưa có notification nội bộ/WebSocket, chưa gửi Zalo/SMS và mã hóa mới có mã gốc. Ưu tiên triển khai `plans/intake/OVERVIEW.md` I1–I6, sau đó notification khách hàng và `docs/SAMPLE_CODING.md` trước PDF; chưa có kết quả phát hành hoặc cổng tra cứu khách hàng.
