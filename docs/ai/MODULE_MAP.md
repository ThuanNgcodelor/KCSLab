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
| Biên bản thử nghiệm | `testing` | `testing` (form nhập kết quả: `components/ResultEntryForm.jsx`) | `/admin/testing` |
| Kết quả / phiếu KQKN | `results` (`KqknDocxExporter`, `OfficeConverter`, `ReportFileStamper`, `ResultEntryService`) | `results` | `/admin/results`, `/api/v1/result-releases` |
| Tra cứu khách | `publicapi` | `source/public/tracuu/` | `/tracuu/`, `/api/v1/public/results` |
| Thông báo | `notification` | `notifications` | `/admin/notifications`, `/ws/notifications` |

Router ở `source/fe/src/app`, thành phần và HTTP client dùng chung ở `source/fe/src/shared`. PWA dùng manifest, service worker và trang mất mạng trong `source/fe/public`.

Vận hành:
- `docker-compose.yml`, `source/be/Dockerfile.prod` và `source/fe/Dockerfile` + `nginx.conf`: chạy trên Linux.
- `scripts/run-all.sh`: chạy, dừng và xem trạng thái trên Linux.
- `scripts/windows/*.ps1` và `deploy/windows/apache-cfc-lab.conf`: chạy trên Windows server.
- Mẫu Word KQKN: `source/be/src/main/resources/templates/kqkn-dv.docx`.
