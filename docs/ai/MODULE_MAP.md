# Module Map

**Ngày cập nhật:** 13/11/2026 · **Trạng thái:** Auth/user/intake đang chạy; transfer là module kế tiếp

Các module auth, user và intake đã có code chạy được; các vùng còn lại vẫn ở mức thiết kế.

| Vùng | Trách nhiệm | Vị trí | API / public interface |
|---|---|---|---|
| Frontend app | React/Vite app, router, auth provider | `source/fe/src/app/`, `source/fe/src/main.jsx` | `/login`, `/admin`, `/admin/intake` |
| Frontend auth | Login bằng email, session state, logout | `source/fe/src/features/auth/` | `/api/v1/auth/*` |
| Frontend users | Tạo/list/khóa tài khoản Admin-only | `source/fe/src/features/users/` | `/api/v1/users` |
| Frontend intake | Danh sách và tạo phiếu tiếp nhận, upload file phiếu 3 | `source/fe/src/features/intake/` | `/admin/intake`, `/api/v1/receipts` |
| Frontend shared | HTTP client, cookie credentials, CSRF header | `source/fe/src/shared/api/` | Dùng bởi features |
| Backend config/common | Security, JWT, CORS, seed, exception mapping | `source/be/src/main/java/com/example/app/config`, `common` | Cross-cutting |
| Backend auth | Login email, JWT access cookie, refresh session, logout | `source/be/src/main/java/com/example/app/auth/` | `/api/v1/auth/*` |
| Backend user | User entity, role, Admin-only management | `source/be/src/main/java/com/example/app/user/` | `/api/v1/users` |
| Backend intake | Customer, receipt, sample, attachment, mã CFC-TN-14 | `source/be/src/main/java/com/example/app/intake/` | `/api/v1/receipts` |
| Backend transfer | Phiếu 4 và custody | Chưa triển khai | Chưa có |
| Backend testing/result | Phiếu 5, review, PDF release | Chưa triển khai | Chưa có |
| Public lookup | Chưa triển khai | — | Chưa có |
| Project memory | Trạng thái, quyết định, kế hoạch | `docs/ai/` | Markdown files |

`docs/ai/CONTEXT.md` là entry point ngắn; dùng bảng này khi cần định vị module thay vì quét toàn repository.
