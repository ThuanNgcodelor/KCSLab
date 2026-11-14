# Architecture Decisions

## 14/11/2026 — Workspace Tiếp nhận, query và notification nội bộ

- Tách module Tiếp nhận thành dashboard, danh sách, tạo phiếu và notification center; có plan riêng cho `ADMIN`, `RECEPTIONIST`, `TECHNICIAN` tại `plans/intake/`.
- Search/filter/sort/pagination receipt chạy trong SQL qua specification/custom query; frontend chỉ quản lý query state và render envelope. Dashboard dùng aggregate API riêng, không đếm từ page và không dùng dữ liệu hard-code.
- App header chỉ hiển thị ba notification mới nhất; trang notification dùng pagination 20/trang. Notification nội bộ được lưu theo recipient trước khi WebSocket phát; REST/database là nguồn sự thật.
- WebSocket dùng authenticated user destination, không cho client subscribe role topic rộng. Customer ZBS/SMS là context riêng, không dùng chung bảng/read state với notification nội bộ.
- KCS hiện là một phòng duy nhất; không tạo UI chọn phòng. Backend dùng giá trị canonical/config `KCS`.
- Phiếu 3 không định nghĩa danh sách đóng loại sản phẩm. UI dùng combobox search + nhập mới; Receptionist đề xuất tên, Admin/backend quản lý product code để bảo vệ quy tắc mã mẫu.

## 14/11/2026 — Thông báo khách hàng sau tiếp nhận

- Sự kiện MVP là `INTAKE_RECORDED`, chỉ được phát sau khi Phiếu 3 và các mẫu commit thành công. Nội dung xác nhận đã ghi nhận, không khẳng định mẫu đã đạt, đã mã hóa hoặc có kết quả.
- Kênh Zalo dùng **ZBS Template Message** theo số điện thoại và template đã duyệt; không dùng chat OA thông thường làm contract cho thông báo giao dịch.
- Hệ thống không khóa cứng một hãng SMS. ZBS, SMS Brandname/MultiSMS và fake provider cùng implement provider interface; adapter SMS thật chỉ được chốt khi LAB chọn nhà cung cấp và cung cấp contract/sandbox hiện hành.
- Ghi `notification_outbox` cùng transaction tiếp nhận, gửi bất đồng bộ bằng worker sau commit. Provider lỗi không rollback Phiếu 3.
- Delivery dùng idempotency/tracking ID và state machine chuẩn hóa; fallback ZBS → SMS chỉ áp dụng cho lỗi vĩnh viễn đã xác định, không fallback khi kết quả kênh đầu còn không rõ để tránh gửi kép.
- Lưu consent theo từng phiếu, template version, actor, provider message ID và trạng thái giao; secret chỉ nằm trong cấu hình deployment, số điện thoại phải mask trong log/UI theo quyền.
- WebSocket chỉ là kênh realtime nội bộ cho nhân viên; database/REST là nguồn sự thật và ZBS/SMS là kênh gửi ra khách hàng.
- Thiết kế và thứ tự triển khai đầy đủ nằm tại `docs/INTAKE_NOTIFICATIONS.md`.

## 13/11/2026 — Kiểu chuỗi và Unicode trên MySQL

- Schema MySQL dùng `VARCHAR`/`TEXT`; không dùng `NVARCHAR`/`NCHAR`.
- Dữ liệu tiếng Việt được lưu bằng charset `utf8mb4`; table nghiệp vụ mặc định dùng `utf8mb4_unicode_ci` và không phụ thuộc default của database.
- Token, hash, checksum hoặc mã cần phân biệt hoa-thường được phép dùng binary collation phù hợp nhưng vẫn giữ kiểu `VARCHAR`/`CHAR`.
- JPA map Java `String` theo length/type phù hợp, không hard-code `NVARCHAR` trong `columnDefinition`.

## 13/11/2026 — Mã mẫu, hồ sơ nguồn và Phiếu kết quả

- Phân biệt ba định danh: mã hồ sơ tiếp nhận, mã mẫu và số Phiếu kết quả; không dùng chung generator.
- Người dùng đã xác nhận CFC-TN-14 là quy trình chuẩn cho mã mẫu `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`. Phiếu ví dụ dùng `PRODUCT-SOURCE-...` là sai lệch tài liệu, không phải contract cho generator.
- Ngày trong mã là business date `Asia/Ho_Chi_Minh`; timestamp audit vẫn lưu UTC.
- Không dùng `COUNT(*) + 1` để cấp sequence; triển khai counter/locking nguyên tử và unique constraint.
- PDF phát hành phải lấy từ dữ liệu hồ sơ nguồn có cấu trúc và snapshot bất biến. Hoàn thiện `-V/-R`, đối chiếu hai người và lịch sử mã là prerequisite của release.
- Quy trình đầy đủ và acceptance criteria nằm tại `docs/SAMPLE_CODING.md`.

## 13/11/2026 — Responsive contract cho Admin UI

- Admin page dùng shell chung `admin-page`; các shell cũ `dashboard-page`, `staff-page`, `intake-page` giữ tương thích.
- Content fluid, tối đa `1680px`, lề ngang dùng `clamp()`; tránh cả hai cực đoan là khóa ở `1280px` trên màn 2K và kéo full-width trên ultrawide.
- `<=1080px`: sidebar thành drawer để tablet/laptop nhỏ không bị ép nội dung. `<=520px`: bảng dữ liệu chuyển card/stack khi cần.
- `>=1600px` tăng spacing và vùng tương tác; `>=2200px` tăng typography. Bộ viewport kiểm tra chuẩn: 390, 1024, 1440 và 2560px.
- Quy tắc chi tiết và checklist bắt buộc nằm tại `source/fe/AGENTS.md`.

## 13/11/2026 — Context tối giản cho AI

- `docs/ai/CONTEXT.md` là nguồn tổng quan ngắn mặc định.
- CodeGraph được dùng trước grep/read cho câu hỏi về source; tài liệu chuyên biệt chỉ đọc khi task cần.
- Changelog và toàn bộ skill references không được nạp mặc định để tránh tốn token.

## 13/11/2026 — Transfer, testing và giao diện CFC

- Phiếu 4 được lưu bằng `sample_transfers` + `sample_transfer_items`; quyết định nhận/từ chối nằm trên transfer, chưa tách bảng custody event tổng quát.
- Mỗi transfer item có tối đa một biên bản thử nghiệm; biên bản chứa collections thiết bị, hóa chất và measurement QC/raw.
- Technician/Admin lập và submit; Admin kiểm tra hoặc trả lại. Đây là quyền runtime tạm thời cho đến khi LAB chốt role reviewer chuyên biệt.
- Admin UI dùng nền trắng ngà, xanh nhạt cho trạng thái và xanh CFC cho hành động; logo chuẩn dùng asset có sẵn thay cho huy hiệu chữ tạm.
- Frontend mặc định gọi API cùng origin qua Vite proxy để chạy được bằng localhost hoặc IP LAN mà không hard-code địa chỉ máy.

## 12/11/2026 — Stack và cấu trúc đã chốt

- Java 21 + Spring Boot 4.1.2 + Maven; React 19 + Vite 6 + JSX; MySQL profile cho dữ liệu thực.
- Giữ `source/be` và `source/fe`; backend package-by-feature, frontend feature-based.
- Kiến trúc modular monolith, chưa tách microservice.

## 12/11/2026 — Authentication và tài khoản

- Login bằng email; access JWT cookie ngắn hạn, refresh session hash phía server, CSRF cookie và logout revoke.
- Không có public registration; chỉ Admin tạo tài khoản.
- Role hiện có: `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`.

## 12/11/2026 — Tiếp nhận mẫu

- Phiếu 3 là vertical slice nghiệp vụ đầu tiên.
- Mã mẫu dạng `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`, unique và không tái sử dụng.
- Attachment lưu local trong dev, có metadata và SHA-256 checksum; production cần object storage/backup.

## Quyết định còn mở

- Người ký/xác nhận phiếu 4 và người duyệt/phát hành kết quả.
- Có thêm role `LAB_MANAGER`/`REVIEWER` hay không.
- Flyway hay Liquibase; object storage và chính sách retention/backup.
