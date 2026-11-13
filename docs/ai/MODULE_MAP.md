# Module Map

**Ngày cập nhật:** 13/11/2026 · **Trạng thái:** Auth, user, intake, transfer và testing đang chạy; coding hardening đang thiết kế

Các module auth, user, intake, transfer và testing đã có code chạy được. Mã hóa hiện mới có mã gốc; phần `-V/-R`, QC/mã mù và xác minh hai người đang ở gate 5A. Result release và public lookup chưa triển khai.

| Vùng | Trách nhiệm | Vị trí | API / public interface |
|---|---|---|---|
| Frontend app | React/Vite app, router, auth provider, layout/footer CFC | `source/fe/src/app/`, `source/fe/src/main.jsx` | `/login`, `/admin/*` |
| Frontend auth | Login bằng email, session state, logout | `source/fe/src/features/auth/` | `/api/v1/auth/*` |
| Frontend users | Tạo/list/khóa tài khoản Admin-only | `source/fe/src/features/users/` | `/api/v1/users` |
| Frontend intake | Danh sách và tạo phiếu tiếp nhận, upload file phiếu 3 | `source/fe/src/features/intake/` | `/admin/intake`, `/api/v1/receipts` |
| Frontend transfer | Danh sách, lập và xác nhận phiếu chuyển mẫu 4 | `source/fe/src/features/transfers/` | `/admin/transfers`, `/api/v1/transfers` |
| Frontend testing | Danh sách, nhập và kiểm tra biên bản thử nghiệm 5 | `source/fe/src/features/testing/` | `/admin/testing`, `/api/v1/testing-records` |
| Frontend shared | HTTP client, cookie credentials, CSRF header | `source/fe/src/shared/api/` | Dùng bởi features |
| Backend config/common | Security, JWT, CORS, seed, exception mapping | `source/be/src/main/java/com/example/app/config`, `common` | Cross-cutting |
| Backend auth | Login email, JWT access cookie, refresh session, logout | `source/be/src/main/java/com/example/app/auth/` | `/api/v1/auth/*` |
| Backend user | User entity, role, Admin-only management | `source/be/src/main/java/com/example/app/user/` | `/api/v1/users` |
| Backend intake | Customer, receipt, sample, attachment, mã CFC-TN-14 | `source/be/src/main/java/com/example/app/intake/` | `/api/v1/receipts` |
| Sample coding contract | Quy tắc mã gốc/phần/QC/mã mù, xác minh và hồ sơ nguồn | `docs/SAMPLE_CODING.md` | API coding đang đề xuất |
| Backend transfer | Phiếu 4, mẫu bàn giao và quyết định nhận/từ chối | `source/be/src/main/java/com/example/app/transfer/` | `/api/v1/transfers` |
| Backend testing | Phiếu 5, thiết bị, hóa chất, QC/raw data và review | `source/be/src/main/java/com/example/app/testing/` | `/api/v1/testing-records` |
| Backend result release | Version PDF, checksum, thu hồi/đính chính | Chưa triển khai | Chưa có |
| Public lookup | Chưa triển khai | — | Chưa có |
| Project memory | Trạng thái, quyết định, kế hoạch | `docs/ai/` | Markdown files |

`docs/ai/CONTEXT.md` là entry point ngắn; dùng bảng này khi cần định vị module thay vì quét toàn repository.
