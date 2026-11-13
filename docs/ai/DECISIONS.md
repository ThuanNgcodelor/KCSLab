# Architecture Decisions

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
