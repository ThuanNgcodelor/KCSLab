# Module Map

**Ngày cập nhật:** 12/11/2026 · **Trạng thái:** Cập nhật sau Giai đoạn 1, phạm vi auth/user

Repository hiện chưa có module ứng dụng đã triển khai. Bảng dưới ghi vùng code dự kiến và trách nhiệm ở mức thiết kế; không coi đây là module đã tồn tại.

| Vùng | Trách nhiệm | Vị trí | API / public interface |
|---|---|---|---|
| Frontend app | React/Vite app, router, auth provider | `source/fe/src/app/`, `source/fe/src/main.jsx` | `/login`, `/users` |
| Frontend auth | Login bằng email, session state, logout | `source/fe/src/features/auth/` | `/api/v1/auth/*` |
| Frontend users | Tạo/list/khóa tài khoản Admin-only | `source/fe/src/features/users/` | `/api/v1/users` |
| Frontend shared | HTTP client, cookie credentials, CSRF header | `source/fe/src/shared/api/` | Dùng bởi features |
| Backend config/common | Security, JWT, CORS, seed, exception mapping | `source/be/src/main/java/com/example/app/config`, `common` | Cross-cutting |
| Backend auth | Login email, JWT access cookie, refresh session, logout | `source/be/src/main/java/com/example/app/auth/` | `/api/v1/auth/*` |
| Backend user | User entity, role, Admin-only management | `source/be/src/main/java/com/example/app/user/` | `/api/v1/users` |
| Public lookup | Chưa triển khai | — | Chưa có |
| Project memory | Trạng thái, quyết định, kế hoạch | `docs/ai/` | Markdown files |

Khi có code sau phê duyệt, thay các vị trí “dự kiến” bằng đường dẫn thực tế và ghi endpoint/interface đã xác minh.
