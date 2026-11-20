# Chạy CFC Lab trên Windows server

Trên Windows server **không dùng Docker**:

| Thành phần | Chạy bằng |
|---|---|
| MySQL, Redis | Laragon |
| Frontend (trang web, trang tra cứu) | Apache của Laragon, phục vụ thư mục `source\fe\dist` |
| Backend | Java 21 (`java -jar`) |
| Chuyển DOCX sang PDF | LibreOffice cài trên Windows |

Hằng ngày chỉ cần: **build frontend và backend → mở Laragon (Start All) → chạy backend.**

---

## 1. Cài một lần

1. **Laragon** (bản Full, có MySQL, Redis, Apache). Trong Menu > Preferences > Services, bật MySQL, Redis và Apache.
2. **JDK 21**, ví dụ Eclipse Temurin 21. Khi cài, chọn "Set JAVA_HOME". Kiểm tra bằng `java -version`.
3. **Node.js 22 LTS**. Kiểm tra bằng `node -v`.
4. **LibreOffice** bản Windows, cài vào thư mục mặc định `C:\Program Files\LibreOffice`.
5. **Mã nguồn**: chép hoặc `git clone` vào một thư mục, ví dụ `C:\cfc-lab`.

## 2. Database và Redis (Laragon)

1. Mở Laragon > Database (HeidiSQL) và tạo database cùng user:
   ```sql
   CREATE DATABASE cfc_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'cfc'@'localhost' IDENTIFIED BY 'mat-khau-manh';
   GRANT ALL PRIVILEGES ON cfc_lab.* TO 'cfc'@'localhost';
   ```
   Không cần tạo bảng: backend tự tạo và cập nhật bằng Flyway khi chạy.
2. Muốn chuyển dữ liệu từ máy Linux sang:
   - Trên Linux, xuất database:
     ```bash
     docker exec source-mysql-1 sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" cfc_lab' > cfc_lab.sql
     ```
   - Trên Windows, nhập file `cfc_lab.sql` bằng HeidiSQL.
   - Chép thêm thư mục `source/be/storage` (ảnh, phiếu kết quả, avatar) sang `C:\cfc-lab\source\be\storage`.
3. Redis: dùng mặc định của Laragon. Nếu đặt mật khẩu (`requirepass`) thì ghi mật khẩu đó vào `REDIS_PASSWORD`.

## 3. File cấu hình `.env`

```bat
cd C:\cfc-lab
copy .env.windows.example .env
```
Mở `.env` và sửa:
- `DB_USERNAME`, `DB_PASSWORD`: user vừa tạo ở bước 2.
- `JWT_SECRET`: chuỗi ngẫu nhiên dài.
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`: tài khoản Admin đầu tiên.
- `FRONTEND_ORIGIN`, `PUBLIC_LOOKUP_URL`: thay `192.168.1.50` bằng IP máy server.

## 4. Apache của Laragon phục vụ frontend (làm một lần)

1. Mở [deploy/windows/apache-cfc-lab.conf](deploy/windows/apache-cfc-lab.conf) và sửa dòng `Define CFC_DIST` theo thư mục dự án. Dùng dấu `/`, ví dụ `C:/cfc-lab/source/fe/dist`.
2. Chép file đó vào `C:\laragon\etc\apache2\sites-enabled\00-cfc-lab.conf`.
3. Laragon > Apache > `httpd.conf`: bỏ dấu `#` ở các dòng `mod_proxy`, `mod_proxy_http`, `mod_proxy_wstunnel`, `mod_rewrite`, `mod_headers`. Danh sách đầy đủ có trong đầu file conf.
4. Mở cổng **5173** trên Windows Firewall cho mạng LAN. Cổng 8080 không cần mở, vì Apache gọi backend ngay trên máy.

## 5. Build (mỗi lần có code mới)

```bat
cd C:\cfc-lab
git pull
powershell -ExecutionPolicy Bypass -File scripts\windows\build.ps1
```
Kết quả: frontend ở `source\fe\dist`, backend ở `source\be\build\*.jar`. Lần đầu script tự tải Maven nên hơi lâu.

## 6. Chạy

1. Mở Laragon và bấm **Start All** (MySQL, Redis, Apache).
2. Chạy backend:
   ```bat
   powershell -ExecutionPolicy Bypass -File scripts\windows\start-backend.ps1
   ```
   Để cửa sổ này mở. Tắt cửa sổ là backend dừng.
3. Mở trình duyệt:
   - Ứng dụng: `http://<IP-server>:5173`
   - Trang tra cứu cho khách: `http://<IP-server>:5173/tracuu/`

Cập nhật code mới: build lại (bước 5) → đóng cửa sổ backend → chạy lại bước 6.2. Apache tự phục vụ bản `dist` mới, không cần restart.

## 7. Cho backend tự chạy khi bật máy (không bắt buộc)

Dùng Task Scheduler > Create Task:
- Trigger: *At startup*.
- Action: `powershell.exe`, Arguments: `-ExecutionPolicy Bypass -File C:\cfc-lab\scripts\windows\start-backend.ps1`, Start in: `C:\cfc-lab`.
- Tick *Run whether user is logged on or not*.

## Lỗi hay gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Backend báo không kết nối được MySQL | Laragon chưa Start All, hoặc user/mật khẩu trong `.env` chưa đúng |
| Lỗi `NOAUTH` / `AUTH` của Redis | `REDIS_PASSWORD` trong `.env` không khớp với `requirepass` của Redis (để trống nếu Redis không đặt mật khẩu) |
| Trang trắng hoặc lỗi 404 khi bấm F5 ở `/admin/...` | Chưa bật `mod_rewrite`, hoặc `CFC_DIST` trỏ sai thư mục |
| Đăng nhập được nhưng API lỗi 503 | Backend chưa chạy (bước 6.2), hoặc chưa bật `mod_proxy` / `mod_proxy_http` |
| Tải DOCX kết quả báo không chuyển được PDF | Chưa cài LibreOffice, hoặc đặt `REPORT_OFFICE_COMMAND` trỏ tới `soffice.com` |

> Đã kiểm tra trên Linux: cú pháp 2 script PowerShell và cấu hình Apache được viết theo cách nginx đang chạy ở máy Linux. **Chưa chạy thử trên Windows thật**: lần cài đầu tiên cần kiểm tra lại các bước 4–6.
