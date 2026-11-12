# Roadmap đề xuất

**Trạng thái:** Chưa phê duyệt; các mốc chỉ là đề xuất

| Giai đoạn | Phạm vi | Phụ thuộc |
|---|---|---|
| 0 — Phân tích/thiết kế | PRD, quy trình, RBAC, ERD, API, security, open questions | Phê duyệt tài liệu và đầu mối LAB |
| 1 — Nền tảng | Bootstrap Java/Spring + React/Vite, DB, auth, layout, API client | Stack/version/hạ tầng được chốt |
| 2 — Tiếp nhận & mã hóa | Phiếu 3, hồ sơ giấy số hóa, customer, receipt, sample, sinh mã CFC-TN-14, QR | Chốt hai kênh tiếp nhận và mẫu bắt buộc |
| 3 — Chuyển mẫu KCS/PTN | Phiếu chuyển mẫu 4, bàn giao, chỉ tiêu, phương pháp, hạn trả, lưu/hoàn mẫu | Người giao/nhận và quy tắc custody |
| 4 — Thử nghiệm | Biên bản 5, phân công, thiết bị, hóa chất, QC, raw data, kết quả chỉ tiêu | Danh mục phương pháp và quyền kỹ thuật |
| 5 — Duyệt & phát hành | Kiểm tra, duyệt, version PDF, phát hành và audit | Người duyệt chuyên môn, mẫu PDF chính thức |
| 6 — Tra cứu khách | Mã + đuôi điện thoại, tiến độ, vùng trồng, lịch sử được phép, tải PDF | Threat model, rate limit, chính sách dữ liệu |
| 7 — Mở rộng | Excel, QR nâng cao, thông báo, dashboard, lịch sử vùng trồng, tích hợp thiết bị | Dữ liệu thật và khảo sát tích hợp |

## Backlog ưu tiên

- **P0:** authentication/session; RBAC; phiếu 3 và hồ sơ scan; customer/receipt/sample; sinh mã CFC-TN-14; phiếu chuyển mẫu 4; biên bản thử nghiệm 5; status/custody history; result draft/review/release; PDF; tra cứu có xác thực; audit log; backup design.
- **P1:** growing areas; dashboard cơ bản; import Excel có kiểm tra; lịch sử nhiều kỳ; thông báo được phê duyệt.
- **P2:** device integration; advanced analytics; QR/PDF nâng cao; tự động hóa và tích hợp ngoài.

Không bắt đầu Giai đoạn 1 trước khi các câu hỏi nghiệp vụ và quyết định công nghệ tối thiểu được xác nhận.
