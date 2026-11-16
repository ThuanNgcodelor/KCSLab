# Bàn giao cho Claude — lập plan, Codex triển khai

**Ngày tổng hợp:** 16/11/2026 · **Dự án:** CFC Lab Management

## 1. Cách phối hợp

Người dùng muốn **Claude phân tích và lập kế hoạch**, sau đó **Codex triển khai, kiểm thử và báo kết quả**. Claude không tự sửa source, schema, database hay secrets trong nhiệm vụ lập kế hoạch này. Có thể đọc code, tài liệu và kiểm tra trạng thái Git; nếu được giao ghi plan thì chỉ sửa tài liệu plan được chỉ định.

Hai agent không tự chia sẻ lịch sử hội thoại. Bản bàn giao này, source và Project Memory là đầu vào chung. Khi Claude hoàn thành, trả plan có thể giao trực tiếp cho Codex: mục tiêu, hiện trạng có bằng chứng, quyết định đã chốt, giả định/câu hỏi còn mở, các bước và file dự kiến, contract API/schema nếu cần, phụ thuộc, rủi ro và tiêu chí nghiệm thu. Phân biệt **đã chạy**, **đang dở**, **đề xuất** và **chưa kiểm thử**; không đánh dấu code là hoàn thành chỉ vì đã viết plan.

Không tự ý commit, merge, xóa hay ghi đè thay đổi đang có. Không đưa mật khẩu, token, nội dung `.env` hoặc thông tin khách hàng thật vào plan, log, commit hoặc câu trả lời.

## 2. Những file cần đọc

Đọc theo phạm vi tác vụ, ưu tiên source thực tế và trạng thái Git nếu tài liệu cũ khác nhau:

1. `AGENTS.md`, `docs/ai/CONTEXT.md`, `docs/ai/STATE.md` và `plans/CURRENT.md`.
2. `plans/intake/OVERVIEW.md`, `plans/intake/RECEPTIONIST.md`, `plans/intake/TECHNICIAN_KCS.md`, `plans/intake/ADMIN.md`.
3. `docs/RBAC.md`, `docs/SAMPLE_CODING.md`; khi đề xuất API/schema, đọc thêm `docs/API_CONVENTIONS.md`, `docs/DATABASE.md` và code tương ứng.
4. Quy tắc từng phần: `source/be/AGENTS.md`, `source/fe/AGENTS.md`.
5. Biểu mẫu gốc ở thư mục dự án: `1. Phiếu KQKN-DV.docx`, `3. Phiếu tiếp nhận và bàn giao mẫu.docx`, `4.Phieu chuyen mau.docx`, `5.Bien_ban_thu_nghiem.docx`, `CFC14Quy trình mã hóa mẫu trái cây.doc`.

`docs/ai/START_HERE.md` và `docs/ai/HANDOFF.md` có bản đồ dự án, nhưng một số mốc là lịch sử; đối chiếu thay đổi chưa commit và source trước khi kết luận. Nếu CodeGraph được index và hoạt động, tuân theo `AGENTS.md`; nếu index không truy vấn được thì ghi nhận và kiểm tra source trực tiếp.

## 3. Công nghệ và vận hành

- Backend: `source/be`, Java 21, Spring Boot 4.1.2, Maven, package-by-feature.
- Frontend: `source/fe`, React 19, Vite 6, JSX, feature-based; dùng router và UI hiện hữu.
- MySQL/Redis thật qua cấu hình local; không ghi giá trị `.env` vào tài liệu. Flyway quản lý schema, Hibernate `validate`; kiểm tra version migration thực tế trước khi đề xuất version mới.
- WebSocket chỉ là kênh cập nhật realtime; database và REST là nguồn dữ liệu chính. Zalo/SMS khách hàng là luồng khác và chưa có provider runtime.
- Responsive theo `source/fe/AGENTS.md`: kiểm tra 390×844, 1024×768, 1440×900 và 2560×952 cho thay đổi layout.

## 4. Quy trình và các role đã chốt

```text
RECEPTIONIST: tiếp nhận khách/mẫu → Phiếu 3 → lập/gửi Phiếu 4
TECHNICIAN/KCS: xem hồ sơ chuyển → đối chiếu mẫu/ảnh → nhận hoặc từ chối
TECHNICIAN hoặc ADMIN: lập Phiếu 5 và đính kèm hồ sơ thử nghiệm
ADMIN: giám sát toàn hệ thống và quyền kiểm tra theo quy trình hiện có
```

- Giữ ba role hiện tại `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`. Người dùng xác nhận `TECHNICIAN` vừa nhận mẫu vừa được lập Phiếu 5 cùng Admin; không tự thêm `SAMPLE_RECEIVER`.
- Mỗi role có dashboard riêng. Receptionist theo công việc tiếp nhận; Technician theo nhận mẫu KCS và thử nghiệm; Admin bao quát toàn hệ thống.
- `receivedBy` của Phiếu 3 là nhân viên đăng nhập tiếp nhận; `deliveredBy` là người giao mẫu nhập trên form. Người giao trên Phiếu 4 là actor bàn giao nội bộ theo model hiện có; kiểm tra code trước khi thiết kế DTO.
- KCS được xem đầy đủ ảnh/file gốc của hồ sơ được chuyển giao để đối chiếu. Nội dung file gốc có thể chứa thông tin khách hàng; không tuyên bố file đã được ẩn thông tin. Bảo vệ quyền theo đúng transfer và attachment; không mặc định mở toàn bộ receipt API cho Technician.
- Phiếu 5 cần cho `ADMIN` và `TECHNICIAN` upload nhiều PDF và tài liệu phù hợp, xem/tải lại; chưa thấy attachment riêng trong module testing hiện tại. Định dạng cụ thể, giới hạn và quy tắc sửa/xóa cần lập đề xuất rõ, không giả là đã chốt.
- Quyền lập Phiếu 5 không tự quyết định quyền tự kiểm tra/phát hành. Giữ các yêu cầu phân tách trách nhiệm và trạng thái phát hành chưa triển khai.

## 5. Hiện trạng và giới hạn đã biết

- Người dùng đã xác nhận luồng `RECEPTIONIST` đăng nhập → tạo Phiếu 3 → sinh mã phiếu/mã mẫu chạy được trên Chrome. Đây chỉ là xác nhận luồng tạo cơ bản, không phải nghiệm thu toàn bộ chức năng tiếp nhận.
- Phiếu 3 có nhiều mẫu, hồ sơ nguồn, file/ảnh; Phiếu 4 có nháp → gửi → nhận/từ chối; Phiếu 5 có vertical slice nhập dữ liệu, submit/review. Đối chiếu code/test để xác định hành vi hiện tại.
- Dashboard Admin hiện chủ yếu phản ánh tiếp nhận, chưa bao quát đầy đủ cả chuỗi. Trang và route dashboard Receptionist đã có commit được ghi tại mục 6; chưa có kết quả nghiệm thu mới được xác minh trong lần bàn giao này.
- Danh sách transfer từng được xác định lấy tối đa 50 bản ghi rồi lọc role trong service; kiểm tra source hiện tại trước khi lập plan phân trang/query.
- Export Phiếu 3 đã sửa ZIP/XML và có test, nhưng chưa có xác nhận mở thành công bằng Microsoft Word thật; chưa đánh dấu nghiệm thu.
- Mã mẫu chuẩn CFC-TN-14: `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`, ví dụ `CT001-SR-15112026-001`. Runtime mới có mã gốc; sequence nguyên tử, xác minh độc lập, `-V/-R`, QC/mã mù, correction history và gate chặn chuyển mẫu chưa hoàn thiện. Không gọi mã hiện tại là đã xác minh.
- Chưa có result release/PDF phát hành, QR/public lookup hoặc gửi ZBS/SMS thật. Không dựng số liệu hoặc trạng thái chưa có runtime như thể đã hoạt động.

## 6. Snapshot worktree khi bàn giao

Ngày 16/11/2026, trong lúc tổng hợp, `main` tiến từ `d6e953c` tới commit `556bfa5` (`feat: implement Receptionist dashboard and update intake navigation`). Commit này gồm:

- `source/fe/src/app/App.jsx`
- `source/fe/src/features/admin/components/AdminLayout.jsx`
- `source/fe/src/features/admin/pages/DashboardPage.jsx`
- `source/fe/src/features/intake/pages/ReceiptCreatePage.jsx`
- `source/fe/src/features/intake/pages/ReceiptDetailPage.jsx`
- `source/fe/src/features/intake/pages/ReceptionistDashboardPage.jsx`.

Quan sát sơ bộ: thay đổi tách dashboard Receptionist và route danh sách, thêm mục Tổng quan và gọi API dashboard hiện có. **Chưa kiểm tra build, E2E hoặc tính đúng của commit này trong lần bàn giao.** Không tự revert hoặc ghi đè; luôn chạy `git status --short` và đọc thay đổi hiện tại trước khi lập kế hoạch vì trạng thái có thể tiếp tục thay đổi.

Nếu Claude dùng worktree khác, cần kiểm tra commit nền có chứa `556bfa5`; các sửa đổi chưa commit và file chưa theo dõi không tự đi theo worktree mới. Phối hợp với người dùng/Codex để đọc đúng worktree hoặc chuyển giao thay đổi có kiểm soát.

## 7. Yêu cầu Claude cần phân tích

1. **Dashboard Receptionist:** đánh giá phần đang dở, landing/navbar, thống kê theo ngày/tuần/tháng và hàng đợi tiếp nhận, phân biệt dữ liệu cá nhân với toàn bộ phòng. Tận dụng API hiện có nếu phù hợp.
2. **Dashboard KCS:** số phiếu và số mẫu chuyển lên theo ngày/tuần/tháng, số chờ/đã nhận/từ chối, hàng đợi, xem file, nhận/từ chối; mốc gửi và mốc nhận cần tách riêng.
3. **Dashboard Admin:** số liệu tổng hợp toàn chuỗi Phiếu 3 → Phiếu 4 → Phiếu 5, drill-down, phạm vi quyền, chỉ số lấy từ database; không tính tổng từ trang dữ liệu đang hiển thị.
4. **Phiếu 5:** thiết kế attachment nhiều file cho Admin/Technician, metadata, quyền upload/xem/tải, lưu trữ, validation, failure/retry, migration và test. Không đồng nhất file đính kèm thử nghiệm với PDF kết quả đã phát hành.
5. **Bảo mật và đồng thời:** projection theo role, kiểm tra quan hệ attachment–receipt–transfer và bảo vệ trường dữ liệu; nhận/từ chối đồng thời phải xử lý xung đột và không nhân đôi thông báo.
6. **Tích hợp:** contract API, route/navbar, Flyway, test backend, frontend build và nghiệm thu Chrome theo breakpoint dự án. Xác định phụ thuộc với gate mã hóa và không tuyên bố gate đã tồn tại.

Các KPI phải phân biệt **phiếu** với **mẫu**, thời điểm phát sinh với trạng thái hiện tại, thời gian theo `Asia/Ho_Chi_Minh` và khả năng dữ liệu nhiều trang. Nếu cần thay đổi API/route hiện dùng, nêu phương án tương thích.

## 8. Quyết định còn mở, không tự giả là quy định

- Thiếu ảnh: hiện mới đề xuất cảnh báo và cho nhận, không phải quy định LAB đã xác nhận.
- SLA/quá hạn nhận mẫu: chưa đặt; trước mắt chỉ hiển thị thời gian chờ.
- Phạm vi dashboard Receptionist cá nhân hay toàn nhóm là đề xuất hiện tại, cần nêu rõ nếu cần người dùng chốt.
- Danh sách loại file Phiếu 5, giới hạn dung lượng/số lượng, chính sách xóa/đính chính và thời hạn lưu cần đề xuất hoặc xin LAB xác nhận.
- Không suy diễn quy trình mẫu giấy hoặc ISO thành yêu cầu đã được phê duyệt; khi phụ thuộc quyết định nghiệp vụ, đánh dấu cần xác nhận.

## 9. Kết quả Claude cần bàn giao cho Codex

- Một plan theo giai đoạn độc lập, ưu tiên và phụ thuộc; nêu file/module dự kiến, ranh giới quyền và API/DTO/migration đề xuất.
- Tiêu chí nghiệm thu cho ba role và luồng: Receptionist tạo/gửi → KCS xem file/nhận hoặc từ chối → KCS/Admin lập Phiếu 5 có nhiều file → Admin theo dõi dashboard.
- Danh sách test có ý nghĩa: quyền truy cập kể cả truy cập chéo attachment, đồng thời nhận mẫu, timezone/filter/aggregate, upload nhiều file, loading/error/empty và responsive.
- Nêu thay đổi đang có cần bảo toàn, các điểm đã kiểm tra thực tế, các rủi ro và phần chưa kiểm tra. Không tiết lộ secrets.
- Codex tiếp nhận plan sau khi người dùng đồng ý phạm vi, đối chiếu source/worktree mới nhất, triển khai và cập nhật Project Memory sau kiểm thử.

## 10. Prompt có thể gửi Claude

> Hãy đọc `docs/ai/CLAUDE_PLANNING_BRIEF.md`, `AGENTS.md` và các tài liệu trực tiếp liên quan. Tôi muốn Claude phân tích và lập plan, Codex sẽ viết code. Dựa trên source và `git status` hiện tại, hãy xây dựng kế hoạch triển khai ba dashboard theo role, nhận mẫu KCS/xem ảnh và nhiều file cho Phiếu 5; đề xuất API, migration, quyền, thứ tự thực hiện, test và các câu hỏi nghiệp vụ còn thiếu. Giữ nguyên thay đổi chưa commit và không sửa code, database hoặc secrets. Trả plan cụ thể để tôi giao Codex triển khai.
