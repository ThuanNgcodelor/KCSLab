# Tổng hợp hiện trạng hệ thống CFC Lab

**Mốc đối chiếu:** 17/11/2026 · Phạm vi: source và tài liệu trong workspace, gồm cả thay đổi chưa commit.

Tài liệu này là bản tổng hợp hiện hành cho người dùng và phiên AI tiếp theo. Source và kết quả test thực tế vẫn là nguồn quyết định; các ghi chép cũ trong Project Memory và plan là lịch sử, có thể không còn đúng. Không xem “đã có code” là đồng nghĩa “đã nghiệm thu” hoặc “đã sẵn sàng production”.

## 1. Mục tiêu và quy trình

Hệ thống nội bộ quản lý luồng: **tiếp nhận/Phiếu 3 → chuyển mẫu/Phiếu 4 → KCS nhận hoặc từ chối → thử nghiệm/Phiếu 5 → hồ sơ kết quả**. Các phần phát hành và tra cứu công khai mới ở giai đoạn nền/xem trước; chưa có chuỗi hoàn chỉnh đến khách.

Ba role hiện có:

| Role | Phạm vi chính | Lưu ý |
|---|---|---|
| RECEPTIONIST | Tiếp nhận, quản lý Phiếu 3, lập/gửi Phiếu 4; dashboard riêng | Không được truy cập workspace nhận mẫu KCS. |
| TECHNICIAN | Nhận/từ chối Phiếu 4; lập biên bản thử nghiệm; tạo nháp hồ sơ kết quả | Không được gọi API Phiếu 3 đầy đủ. File gốc được chuyển và ghi chú vẫn có thể chứa thông tin khách. |
| ADMIN | Quản lý nhân viên, thao tác nội bộ theo quyền và kiểm tra biên bản | Có trang admin, nhưng dashboard tổng hợp toàn hệ thống chưa được coi là hoàn chỉnh/nghiệm thu. |

Không tạo role SAMPLE_RECEIVER. `receivedBy` là nhân viên lập Phiếu 3; `deliveredBy` là người giao được nhập trên form. Public tương lai không trả các thông tin này hoặc thông tin liên hệ đầy đủ.

## 2. Nền tảng và chạy cục bộ

- Backend: Java 21, Spring Boot, Maven, package-by-feature tại `source/be`. Frontend nội bộ: React/Vite/JSX tại `source/fe`.
- MySQL lưu dữ liệu; Redis hỗ trợ refresh session. Cấu hình được lấy từ môi trường/.env, không ghi credentials vào tài liệu.
- Flyway quản lý migration `source/be/src/main/resources/db/migration/`; Hibernate dùng `validate`, không tự viết migration. Trong source có V1–V6; V5/V6 đã được thử trên database tạm, **chưa xác nhận đã chạy trên database làm việc**. Kiểm tra `flyway_schema_history` trên đúng database trước khi kết luận.
- File hồ sơ/avatar hiện lưu filesystem local; cần xác định backup, retention và lưu trữ khi triển khai thực tế.
- Lệnh chạy backend/frontend và hướng dẫn theo hệ điều hành: `docs/ai/START_HERE.md`. Không thay đổi database đang có chỉ để kiểm tra tài liệu.

## 3. Tính năng nội bộ

| Hạng mục | Có trong source / đã ghi nhận | Chưa hoàn tất hoặc cần nghiệm thu |
|---|---|---|
| Auth và tài khoản | Login, cookie/session, CSRF, refresh Redis, logout, remember-me; Admin quản lý nhân viên; profile, avatar, đổi mật khẩu. | Rà lại E2E thật và vận hành bảo mật khi triển khai. |
| Phiếu 3 | Nhiều mẫu/hồ sơ nguồn, điều kiện nhận, niêm phong, chỉ tiêu, ảnh/file có phân loại và checksum; tìm kiếm/phân trang; dashboard Tiếp nhận. Người dùng đã xác nhận tạo phiếu bằng role thật trên Chrome. | Draft/completion và các quy định liên quan chưa đầy đủ. Export DOCX đã sửa lỗi XML nhưng **chưa nghiệm thu mở bằng Word thật**. |
| Phiếu 4 | Nháp, sửa, gửi, nhận/từ chối; khóa mẫu khi gửi; tìm hồ sơ có thể chuyển ở server. | Nghiệm thu lại chuỗi thật với file và các lỗi phát sinh. |
| Workspace KCS | Dashboard `/admin/receiving`, danh sách, chi tiết, gallery, nhận/từ chối; projection riêng; aggregate tại DB, tìm mã, phân trang, sort whitelist; khóa hàng khi quyết định. | Chưa nghiệm thu E2E bản cuối với hai role, file thật, 401/403/409 và realtime; không coi K1+K2 đã hoàn thành. Thiếu ảnh hiện chỉ cảnh báo; LAB cần xác nhận. |
| Phiếu 5 | Biên bản thử nghiệm: thiết bị, hóa chất, QC/raw data, gửi, trả lại và review nội bộ. | Chưa hoàn tất upload nhiều file cho Phiếu 5; cần kiểm tra tách biệt người phân tích/người kiểm tra. |
| Thông báo nội bộ | Lưu DB, notification center, WebSocket và toast; event tiếp nhận/chuyển/nhận/từ chối. | Chưa nghiệm thu đầy đủ hai phiên WebSocket thật, sau commit/rollback và tình huống lỗi. Chính sách hiển thị tên khách trong notification cần LAB xác nhận. |
| Dashboard Admin | Có route/trang Admin hiện hữu. | Chưa coi dashboard bao quát toàn hệ thống là hoàn thành. |

## 4. Mã hóa CFC-TN-14

Mã gốc theo dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE` (ví dụ `CT001-SR-15112026-001`). Chưa có bộ gate đầy đủ: sequence nguyên tử, xác minh hai người và các trạng thái xác minh, mã `-V/-R`, QC/mã mù, lịch sử hiệu chỉnh, chặn chuyển mẫu chưa xác minh. Không coi mã hiện tại đã tuân thủ đầy đủ CFC-TN-14; xem `docs/SAMPLE_CODING.md` và tài liệu gốc.

## 5. Hồ sơ kết quả, public lookup và tin nhắn khách

- `V6__result_release_drafts.sql` và feature `results` là thay đổi **chưa commit**. API hiện có `POST /api/v1/result-releases` cho TECHNICIAN tạo nháp từ Phiếu 5 `REVIEWED`, và `GET /api/v1/result-releases/{id}` cho ADMIN/TECHNICIAN đọc. Chưa có UI hồ sơ kết quả, upload, submit/duyệt, phát hành, thu hồi hoặc quản lý phiên bản hoàn chỉnh.
- Quyết định đã chốt: chỉ cho tạo/lưu nháp; **chặn phát hành đến khi hoàn tất gate mã hóa**. KQKN-DV là hồ sơ kết quả đã nói tới. Định hướng file: tối đa 10 MB/file, 10 file/hồ sơ; mặc định chỉ PDF public, Admin có thể bật Word/Excel. Đây chưa phải tính năng đã chạy.
- `source/public/tracuu/` là site tĩnh **xem trước cục bộ, không login**; form vô hiệu hóa, có thông báo chưa khả dụng và `noindex`. Ảnh hero là minh họa AI, không phải ảnh phòng lab thật. Cách chạy: `source/public/README.md`.
- Chưa có kết quả public, API tra cứu, mã/QR phát hành, tải file, rate limit, bảo vệ chống dò, SEO production, domain hoặc Cloudflare Tunnel. QR và tra mã tay trong tương lai đều yêu cầu thêm 4 số cuối điện thoại. Không expose API nội bộ; `noindex` không thay thế bảo mật.
- ZBS/SMS/outbox/provider/callback gửi khách chưa triển khai. WebSocket nội bộ không phải là Zalo/SMS.

## 6. Route và ranh giới

- Nội bộ: `/login`, `/admin/intake` (dashboard Receptionist), `/admin/intake/receipts`, `/admin/transfers`, `/admin/receiving`, `/admin/receiving/transfers`, `/admin/testing`, `/admin/notifications`, `/admin/profile`. Kiểm tra `source/fe/src/app/App.jsx` cho quyền và route chi tiết.
- API nội bộ dùng `/api/v1/**`; workspace KCS dùng `/api/v1/receiving/**`. API transfer cũ vẫn tồn tại, Technician nhận projection giới hạn; file gốc có thể chứa PII. Tra quyền chi tiết ở `docs/RBAC.md` và contract ở `docs/API_CONVENTIONS.md`.
- `/tracuu/` là **site preview riêng**, không phải route React hoặc endpoint public đã phát hành.

## 7. Bằng chứng kiểm thử đã ghi nhận

- Lần trước: backend Java 21/MySQL/Redis thử nghiệm cho bước nháp hồ sơ kết quả ghi nhận **38 test, 0 failures, 0 errors, 0 skipped**; không đồng nghĩa database làm việc đã được migrate hay toàn bộ nghiệp vụ được nghiệm thu.
- KCS: bộ trước đó ghi nhận 32 test, 0 failures/errors/skipped, có HTTP/MySQL kiểm tra attachment chéo và KPI/phân trang; vẫn còn khoảng trống E2E, WebSocket sau commit/rollback, một số ranh giới/sort.
- Frontend `npm run build` đã pass ở các lượt trước; nhiều ảnh/breakpoint và WebSocket được kiểm tra bằng API giả lập. Landing preview đã được kiểm tra HTTP 200 và không tràn ngang ở 390 và 1440 px.
- **Ngày cập nhật tài liệu này không chạy lại Maven, frontend build hoặc E2E.** Không suy diễn kết quả cũ là kết quả mới.

## 8. Ưu tiên tiếp theo và quyết định còn mở

1. Nghiệm thu luồng thật RECEPTIONIST → KCS → thử nghiệm, notification, file thật và export Phiếu 3 bằng Word; không tự thay đổi dữ liệu sản xuất để thử.
2. Hoàn thiện gate mã hóa và các quy tắc còn thiếu, kèm test cạnh tranh/kiểm soát quyền.
3. Hoàn thiện hồ sơ kết quả nội bộ và cơ chế duyệt/phát hành sau gate; xác định chữ ký, số phiếu, lưu trữ/backup và quét file.
4. Chỉ sau đó triển khai API/site tra cứu public an toàn, QR, rate limit và hạ tầng/domain; tiếp tục hoàn thiện dashboard Admin và frontend theo plan.
5. Triển khai ZBS/SMS khi có tài khoản provider, template được duyệt và quyết định về consent.

Cần LAB xác nhận: quy định thiếu ảnh, nội dung/tên khách trong notification, người kiểm tra/ký, quyền xem trạng thái kết quả của Receptionist, chứng nhận được công bố, chính sách lưu trữ và nội dung thông báo kết quả.

## 9. Bàn giao và nguồn tham khảo

- `docs/ai/START_HERE.md`, `docs/ai/CONTEXT.md`, `docs/ai/STATE.md`, `docs/ai/HANDOFF.md`: Project Memory.
- `plans/COMPLETION_PLAN.md`, `plans/NOTIFICATION_REALTIME.md`, `plans/intake/`: kế hoạch và tiêu chí kiểm tra; phần cũ có thể khác trạng thái hiện tại.
- `docs/RBAC.md`, `docs/API_CONVENTIONS.md`, `docs/DATABASE.md`, `docs/PUBLIC_LOOKUP_SEO.md`, `docs/INTAKE_NOTIFICATIONS.md`: tài liệu chuyên biệt, đối chiếu source trước khi thực hiện.
- Tại thời điểm lập tài liệu còn nhiều thay đổi chưa commit, gồm code `results`, migration V6, `source/public/`, route/frontend và tài liệu. Không commit, ghi đè hoặc xóa thay đổi đó khi chưa được yêu cầu.
