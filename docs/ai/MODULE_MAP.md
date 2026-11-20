# Bản đồ module

Các module backend nằm trong `source/be/src/main/java/com/example/app`. Frontend nằm trong `source/fe/src/features`.

| Module | Backend | Frontend | Cổng vào |
|---|---|---|---|
| Phiên đăng nhập | `auth`, `config` | `auth` | `/login`, `/api/v1/auth` |
| Người dùng | `user` | `users`, `profile` | `/admin/users`, `/admin/profile` |
| Yêu cầu / lấy mẫu | `sampling` | `sampling` | `/admin/requests` |
| Tiếp nhận / mã mẫu | `intake` | `intake` | `/admin/intake`, `/api/v1/receipts` |
| Chuyển mẫu / KCS | `transfer` | `transfer`, `receiving` | `/admin/transfers`, `/admin/receiving` |
| Phương pháp thử | `testing/method` | `methods` | `/admin/test-methods` |
| Biên bản thử nghiệm | `testing` | `testing` | `/admin/testing` |
| Kết quả | `results` | `results` | `/admin/results`, `/api/v1/result-releases` |
| Tra cứu khách | `publicapi` | `source/public/tracuu/` | `/tracuu/`, `/api/v1/public/results` |
| Thông báo | `notification` | `notifications` | `/admin/notifications`, `/ws/notifications` |

Router ở `source/fe/src/app`, thành phần và HTTP client dùng chung ở `source/fe/src/shared`. PWA dùng manifest, service worker và trang mất mạng trong `source/fe/public`.
