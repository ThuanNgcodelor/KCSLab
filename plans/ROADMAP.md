# Roadmap đề xuất

**Trạng thái:** Chưa phê duyệt; các mốc chỉ là đề xuất

| Giai đoạn | Phạm vi | Phụ thuộc |
|---|---|---|
| 0 — Phân tích/thiết kế | PRD, quy trình, RBAC, ERD, API, security, open questions | Phê duyệt tài liệu và đầu mối LAB |
| 1 — Nền tảng | Bootstrap Java/Spring + React/Vite, DB, auth, layout, API client | Stack/version/hạ tầng được chốt |
| 2 — Tiếp nhận | Customer, receipt, sample, mã, trạng thái nhận | RBAC, model dữ liệu, API |
| 3 — Kiểm nghiệm/tracking | Phân công sơ bộ, tiến độ, tra cứu khách | Quy tắc trạng thái và bảo mật tra cứu |
| 4 — Kết quả | Nhập, review, approve, release, PDF | Người duyệt và mẫu báo cáo LAB |
| 5 — Mở rộng | Excel, QR nâng cao, thông báo, dashboard, lịch sử vùng trồng, tích hợp thiết bị | Dữ liệu thật và khảo sát tích hợp |

## Backlog ưu tiên

- **P0:** authentication/session; RBAC; customer; receipt/sample; code generation; status history; basic tracking; result draft/review/release boundary; audit log; backup design.
- **P1:** growing areas; dashboard cơ bản; import Excel có kiểm tra; lịch sử nhiều kỳ; thông báo được phê duyệt.
- **P2:** device integration; advanced analytics; QR/PDF nâng cao; tự động hóa và tích hợp ngoài.

Không bắt đầu Giai đoạn 1 trước khi các câu hỏi nghiệp vụ và quyết định công nghệ tối thiểu được xác nhận.
