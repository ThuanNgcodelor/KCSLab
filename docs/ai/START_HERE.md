# CFC Lab — Bắt đầu phiên làm việc

**Cập nhật:** 17/11/2026
**Mục đích:** file chỉ dẫn nhanh cho phiên làm việc mới ở công ty.

## Đọc theo thứ tự

1. `docs/DOC_HIEU_HE_THONG.md` để hiểu nhanh trạng thái và việc cần làm.
2. File này để xem cách chạy; `docs/SYSTEM_STATUS.md` nếu cần chi tiết kỹ thuật.
3. `docs/ai/CONTEXT.md` để lấy context ngắn của hệ thống.
4. `docs/ai/STATE.md` để xem các thay đổi đã xác minh gần nhất.
5. `plans/SYSTEM_COMPLETION_ROADMAP.md` để biết kế hoạch triển khai/nghiệm thu hiện hành; `plans/CURRENT.md` để tham khảo lịch sử và kế hoạch chi tiết.
6. `docs/ai/HANDOFF.md` để tiếp tục công việc kỹ thuật từ phiên trước.
7. Chỉ đọc tài liệu chuyên môn theo tác vụ:
   - Tiếp nhận: `plans/intake/OVERVIEW.md`, `RECEPTIONIST.md`, `ADMIN.md`, `TECHNICIAN_KCS.md`.
   - Mã hóa: `docs/SAMPLE_CODING.md` và tài liệu CFC-TN-14 ở thư mục gốc.
   - Quyền: `docs/RBAC.md`.
   - Zalo/SMS: `docs/INTAKE_NOTIFICATIONS.md`.
   - SEO/trang tra cứu public: `docs/PUBLIC_LOOKUP_SEO.md`.
   - Database/API: `docs/DATABASE.md`, `docs/API_CONVENTIONS.md`.

## Trạng thái: xem tổng hợp hiện hành

Ưu tiên `docs/SYSTEM_STATUS.md`; các chi tiết bên dưới là ghi chép nền, không thay thế bản tổng hợp.

### Đã chạy được

- Spring Boot 4.1.2/Java 21 ở `source/be`.
- React 19/Vite 6/JSX ở `source/fe`.
- Login email, JWT cookie, CSRF, Redis refresh session, logout và tự refresh access token.
- Role `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`.
- Phiếu 3: customer, nhiều mẫu, hồ sơ nguồn, người tiếp nhận, ảnh/file, checksum, tìm kiếm, filter, sort và pagination backend 20/trang.
- Phiếu 4: draft, sửa, submit, KCS nhận/từ chối, khóa mẫu khi submit và đồng bộ trạng thái.
- Phiếu 5: thiết bị, hóa chất, QC/raw data, submit, trả lại và review nội bộ.
- Notification nội bộ lưu database, header/page riêng và WebSocket; đây không phải Zalo/SMS.
- Profile, avatar nén/lưu JPEG, đổi mật khẩu và responsive admin UI.
- MySQL profile đã được bật trong `.env` bằng `SPRING_PROFILES_ACTIVE=mysql`.

### Chưa được xem là hoàn tất

- Export Phiếu 3: đã sửa XML template/exporter, nhưng vẫn phải test mở bằng Microsoft Word thật trước khi đánh dấu hoàn tất.
- Mã hóa CFC-TN-14: runtime mới có mã gốc; chưa có sequence nguyên tử, xác minh hai người, `-V/-R`, QC/mã mù và correction history.
- Zalo OA/ZBS/SMS: mới có thiết kế, chưa có provider runtime.
- Trang tra cứu: có landing tĩnh xem trước không login, form khóa và `noindex`; chưa có API tra cứu, QR, PDF phát hành hay SEO production.
- Tách hệ thống nội bộ và public bằng Cloudflare Tunnel: chưa triển khai.

## Cách chạy bằng tay

Laragon phải bật MySQL và Redis. Database dùng `cfc_lab`.

Trên Linux, bảo đảm MySQL và Redis đang chạy, cấu hình environment phù hợp, sau đó chạy trong hai terminal:

```bash
cd source/be
mvn spring-boot:run
```

```bash
cd source/fe
npm run dev
```

Trang tra cứu chỉ là preview riêng: xem lệnh tại `source/public/README.md`. Backend đọc cấu hình theo môi trường khởi chạy; không đưa mật khẩu vào lệnh hoặc tài liệu.

Ví dụ trên Windows/Laragon:

Backend:

```powershell
cd source/be
$env:SPRING_PROFILES_ACTIVE="mysql"
mvn.cmd spring-boot:run
```

Frontend:

```powershell
cd source/fe
npm.cmd run dev
```

Không dùng `npm run dev` nếu PowerShell đang chặn `npm.ps1`; dùng `npm.cmd run dev`.

## Dữ liệu và secrets

- MySQL: `cfc_lab`, cấu hình local ở `.env` và `source/be/src/main/resources/application-mysql.yml`.
- Schema: Flyway tự chạy `source/be/src/main/resources/db/migration/` khi backend startup; database hiện có sẽ được baseline ở version `1`, database rỗng sẽ chạy `V1__baseline_schema.sql`. Hibernate chỉ `validate`.
- File/ảnh receipt: `source/be/storage/receipts`.
- Avatar: `source/be/storage/avatars`.
- Refresh session: Redis, không lưu raw refresh token.
- Không ghi password, JWT secret hoặc token vào tài liệu này; chỉ đọc `.env` trên máy được cấp quyền.

## Mã mẫu chuẩn

CFC-TN-14 dùng:

```text
SOURCE-PRODUCT-DDMMYYYY-SEQUENCE
```

Ví dụ:

```text
CT001-SR-15112026-001
```

`CT001` là mã nguồn, `SR` là mã sản phẩm, ngày dùng giờ Việt Nam, `001` là thứ tự trong ngày. Mã hồ sơ `TN-...` và số Phiếu kết quả là hai định danh khác; không gộp làm một.

## Quyền nhìn người tiếp nhận

- Admin và Receptionist xem đầy đủ Phiếu 3 và `receivedBy`.
- Technician/KCS không được gọi API Phiếu 3 đầy đủ; notification nội bộ có thể cho biết tên nhân viên đã ghi nhận để giữ chuỗi trách nhiệm.
- Trang tra cứu public tuyệt đối không trả `receivedBy`, `deliveredBy`, email, phone đầy đủ hoặc dữ liệu nội bộ.

## Nguyên tắc public/SEO

Chỉ public một vùng riêng cho `/tracuu`; hệ thống `/admin`, `/login`, `/api/v1` vẫn nội bộ. Chỉ landing page `/tracuu` được index. Kết quả cụ thể và API lookup dùng `noindex`, `no-store`, rate limit và chỉ trả kết quả đã phát hành. Chi tiết ở `docs/PUBLIC_LOOKUP_SEO.md`.

## Khi bắt đầu tác vụ mới

- Tác vụ backend: đọc `source/be/AGENTS.md`, `docs/ai/STATE.md`, `docs/API_CONVENTIONS.md`, rồi dùng CodeGraph.
- Tác vụ frontend: đọc `source/fe/AGENTS.md`, dùng feature tương ứng trong `source/fe/src/features`.
- Tác vụ mã hóa: đọc `docs/SAMPLE_CODING.md`; không tự đổi thứ tự mã từ `SOURCE-PRODUCT-...` sang `PRODUCT-SOURCE-...`.
- Tác vụ public lookup: đọc `docs/PUBLIC_LOOKUP_SEO.md`; không expose API nội bộ.
- Sau thay đổi có ý nghĩa: chạy test/build phù hợp và cập nhật `docs/ai/STATE.md`.
