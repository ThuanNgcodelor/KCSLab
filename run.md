# Chạy CFC Lab trên máy Linux (máy dev)

> Windows server chạy theo cách khác (Laragon, không Docker): xem [RUN_WINDOWS_SERVER.md](RUN_WINDOWS_SERVER.md).

## Chạy

```bash
cd ~/Works/CFC/KCSLab
./scripts/run-all.sh
```

Lệnh này:

1. **Chỉ kiểm tra kết nối** MySQL và Redis theo file `.env`, không bật, tắt hay tạo gì. Nếu không kết nối được, script báo lỗi và dừng: bạn bật MySQL hoặc Redis lên rồi chạy lại.
2. Build và chạy 3 thứ:
   - Ứng dụng: http://localhost:5173 (trong mạng LAN: http://192.168.1.132:5173)
   - Tra cứu: http://localhost:5173/tracuu/
   - API: http://localhost:8080
3. Kiểm tra LibreOffice (nằm sẵn trong container backend) và báo nếu không dùng được.

Database tự cập nhật bằng Flyway khi API khởi động. File tải lên nằm ở `source/be/storage`.

## Tắt

```bash
./scripts/run-all.sh stop
```

Chỉ tắt Ứng dụng, Tra cứu và API. MySQL và Redis vẫn chạy.

## Lệnh khác

| Lệnh | Làm gì |
|---|---|
| `./scripts/run-all.sh status` | Xem MySQL, Redis, LibreOffice, Ứng dụng, Tra cứu, API cái nào đang được ✔ / ✘ |
| `./scripts/run-all.sh logs` | Xem log backend và frontend |
| `./scripts/run-be-docker.sh` | Chế độ sửa code: chỉ chạy backend bằng `mvn spring-boot:run` (dùng cùng `cd source/fe && npm run dev`) |
| `./scripts/run-be-tests-docker.sh` | Chạy test backend với MySQL/Redis tạm, không đụng dữ liệu thật |

Lưu ý: gõ `./scripts/...` (có dấu `.` ở đầu), chạy từ thư mục dự án.
