# Module Map

**Ngày cập nhật:** 12/11/2026 · **Trạng thái:** Bản đồ thiết kế, chưa phải source map

Repository hiện chưa có module ứng dụng đã triển khai. Bảng dưới ghi vùng code dự kiến và trách nhiệm ở mức thiết kế; không coi đây là module đã tồn tại.

| Vùng | Trách nhiệm | Vị trí | API / public interface |
|---|---|---|---|
| Frontend app | React/Vite app, router, providers, layouts | `source/fe/src/app/` (dự kiến) | Chưa runtime |
| Frontend features | auth, users, customers, reception, samples, testing, results, tracking | `source/fe/src/features/` (dự kiến) | Chưa runtime |
| Frontend shared | UI, API client, hooks, utils, config | `source/fe/src/shared/` (dự kiến) | Chưa runtime |
| Backend config/common | Cấu hình toàn hệ thống và shared thật sự | `source/be/src/main/java/.../config`, `common` (dự kiến) | Chưa runtime |
| Backend modules | auth, user, customer, reception, sample, testing, result, tracking, audit | `source/be/src/main/java/.../` (dự kiến) | Chưa runtime |
| Public lookup | Tra cứu tiến độ có kiểm soát | `/api/v1/public/lookups` (đề xuất) | Chưa chốt |
| Authentication | Login, refresh, logout, current user | `/api/v1/auth/*` (đề xuất) | Chưa chốt |
| Project memory | Trạng thái, quyết định, kế hoạch | `docs/ai/` | Markdown files |

Khi có code sau phê duyệt, thay các vị trí “dự kiến” bằng đường dẫn thực tế và ghi endpoint/interface đã xác minh.
