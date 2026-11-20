# CLAUDE.md — CFC Lab Management

## Vai trò

- **Claude phân tích và lập plan; Codex viết code.** Claude không sửa source, schema, migration, database hay secrets. Chỉ ghi file plan/tài liệu khi được giao rõ (thường trong `plans/`).
- Hai agent dùng chung plan, source và Project Memory. Bắt đầu từ [START_HERE.md](docs/ai/START_HERE.md).
- Một plan tốt phải giao thẳng được cho Codex: mục tiêu, hiện trạng có bằng chứng, quyết định đã chốt, câu hỏi mở, các bước + file dự kiến, contract API/schema, phụ thuộc, rủi ro, tiêu chí nghiệm thu, danh sách test.
- Luôn phân biệt **đã chạy / đang dở / đề xuất / chưa kiểm thử**. Không đánh dấu hoàn thành chỉ vì đã viết plan hoặc code.

## Dự án

Modular monolith quản lý phòng Lab (tiếp nhận mẫu → chuyển KCS → thử nghiệm).

- Backend `source/be`: Java 21, Spring Boot 4.1.2, Maven, package-by-feature trong `com.example.app` (`auth`, `user`, `intake`, `transfer`, `testing`, `config`, `common`).
- Frontend `source/fe`: React 19, Vite 6, JSX, feature-based trong `src/features/*` (`auth`, `users`, `intake`, `transfer`, `testing`, `notifications`, `profile`, `admin`), dùng chung ở `src/shared/`. Không TypeScript, không UI library.
- Dữ liệu: MySQL 8.4 + Flyway (Hibernate `validate`), Redis. WebSocket chỉ cho cập nhật nội bộ; REST + DB là nguồn chính.
- Thư mục khác: `docs/` (tài liệu), `plans/` (kế hoạch), `guideline-assets/` (logo/brand), `scripts/`, các file `.docx/.doc` ở gốc là biểu mẫu gốc (Phiếu 3, 4, 5, KQKN, quy trình CFC-TN-14).

## Nghiệp vụ và role

```text
RECEPTIONIST: tiếp nhận khách/mẫu → Phiếu 3 → lập/gửi Phiếu 4
TECHNICIAN (KCS): xem hồ sơ chuyển → đối chiếu → nhận/từ chối Phiếu 4
TECHNICIAN hoặc ADMIN: lập Phiếu 5 (thử nghiệm) → submit → review
ADMIN: giám sát toàn hệ thống
```

- Chỉ ba role `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`; không tự thêm role mới.
- Mã mẫu chuẩn CFC-TN-14: `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`. Hiện mới có mã gốc.
- **Chưa có**: `-V/-R`, QC/mã mù, xác minh độc lập, phát hành result release/PDF, QR/public lookup thật, gửi ZBS/SMS khách. V6 và API tạo/đọc nháp kết quả đã có trong thay đổi chưa commit; landing tra cứu chỉ là preview khóa form. Không mô tả các phần chưa có như đã hoạt động.
- Phân biệt KPI theo **phiếu** và **mẫu**; thời gian theo `Asia/Ho_Chi_Minh`; aggregate phải lấy từ DB, không tính từ trang đang hiển thị.

## Tài liệu — đọc theo tác vụ

| Cần gì | Đọc |
|---|---|
| Tổng quan nhanh | `AGENTS.md`, `docs/ai/CONTEXT.md` |
| Định vị module | `docs/ai/MODULE_MAP.md` |
| Quyết định kiến trúc / kế hoạch | `docs/ai/DECISIONS.md`, `plans/CURRENT.md`, `plans/ROADMAP.md` |
| Workspace theo role | `plans/intake/OVERVIEW.md`, `RECEPTIONIST.md`, `TECHNICIAN_KCS.md`, `ADMIN.md` |
| Quyền / API / schema / mã mẫu | `docs/RBAC.md`, `docs/API_CONVENTIONS.md`, `docs/DATABASE.md`, `docs/SAMPLE_CODING.md` |
| Thông báo khách | `docs/INTAKE_NOTIFICATIONS.md` |
| Quy tắc code | `source/be/AGENTS.md`, `source/fe/AGENTS.md` |

Chỉ đọc phần liên quan trực tiếp. Đối chiếu với source và dùng `git log` để tra lịch sử.

## Quy tắc làm việc

- Có `.codegraph/`: dùng `codegraph_explore` (hoặc `codegraph explore "<câu hỏi>"`) trước grep/đọc nhiều file.
- Chạy `git status --short` và `git log` trước khi lập plan; bảo toàn thay đổi chưa commit của người khác.
- Không commit, merge, xóa, ghi đè hoặc thao tác destructive trừ khi người dùng yêu cầu rõ.
- Không đọc/ghi/lộ `.env`, mật khẩu, token; không đưa dữ liệu khách thật vào plan, log hay commit.
- Không bịa package, schema, API hay quy định nghiệp vụ. Điều chưa được LAB xác nhận (SLA, loại file Phiếu 5, chính sách thiếu ảnh…) phải ghi **"cần xác nhận"**.
- Giữ nguyên framework, router, package manager, API contract hiện có nếu chưa được yêu cầu đổi; nếu cần đổi, nêu phương án tương thích.
- Trả lời bằng tiếng Việt.

## Kiểm tra mà plan nên yêu cầu Codex chạy

- Backend: `mvn test` (Java 21) trong `source/be`; migration Flyway mới phải kiểm tra version thực tế trước khi đặt số.
- Frontend: `npm run build` trong `source/fe`.
- Giao diện: Chrome thật ở 390×844, 1024×768, 1440×900, 2560×952; không tràn ngang; có loading/error/empty.
- Test có ý nghĩa: quyền truy cập chéo (attachment–receipt–transfer), nhận/từ chối đồng thời, timezone/aggregate, upload nhiều file.
- Sau kiểm thử Codex cập nhật Project Memory trong `docs/ai/`.

## Lưu ý hiện tại

- Dashboard Receptionist đã có ở commit `556bfa5` nhưng chưa nghiệm thu bằng Chrome; API `/api/v1/intake/dashboard/receptionist` chưa trả hàng đợi hành động.
- Export Phiếu 3 DOCX chưa được xác nhận mở bằng Word thật.
