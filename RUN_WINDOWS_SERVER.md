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

### Chạy FE, backend và Cloudflare Tunnel bằng launcher

Để chạy theo cấu hình hiện tại (Vite dev trên cổng 5173, backend trên cổng 8080), bấm đúp [scripts/windows/Run-CFC-Lab.cmd](scripts/windows/Run-CFC-Lab.cmd). Launcher sẽ:

- hỏi có muốn `git pull` trước khi build không; chỉ pull kiểu fast-forward, để Git từ chối nếu có thay đổi cục bộ bị ảnh hưởng, không tự stash/reset/ghi đè;
- kiểm tra Java 21+, Node/npm, khả năng kết nối cổng MySQL và Redis, LibreOffice, Cloudflared service và cổng ứng dụng;
- build FE/backend bằng `scripts\windows\build.ps1`, sau đó mở backend và Vite trong hai cửa sổ riêng; Cloudflared chạy dưới dạng Windows service.
- lưu thông tin định danh hai cửa sổ để file stop chỉ dừng đúng tiến trình do launcher tạo.

Trước lần chạy đầu, đóng các cửa sổ FE/backend đang chạy thủ công và mở Laragon > **Start All**. Nếu Git từ chối pull vì thay đổi cục bộ, launcher hỏi có build code hiện tại không; chọn `Y` chỉ khi muốn chạy phiên bản đang có trên máy. Nếu vừa `git pull` thủ công, launcher sẽ build code hiện tại; backend đang chạy phải được dừng trước để nạp JAR mới. Kiểm tra MySQL/Redis ở đây chỉ xác nhận cổng dịch vụ có phản hồi; backend mới xác nhận thông tin đăng nhập DB.

Để dừng, bấm đúp [scripts/windows/Stop-CFC-Lab.cmd](scripts/windows/Stop-CFC-Lab.cmd). File này dừng cây tiến trình backend/frontend được tạo bởi phiên launcher gần nhất và yêu cầu Windows dừng service `Cloudflared`; có thể cần chạy với quyền Administrator. Các cửa sổ FE/backend mở thủ công không bị dừng. Laragon/MySQL/Redis không bị tắt.

Launcher chạy Vite development server, phù hợp để thử nghiệm Tunnel, không phải cấu hình production. Không nên để dev server mở công khai lâu dài; triển khai production cần build `dist` và dùng Apache Laragon theo các bước bên trên.

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

---

## 8. Mã QR và khi gắn tên miền

Mã QR trên phiếu kết quả là địa chỉ `PUBLIC_LOOKUP_URL` cộng thêm `?code=<mã tra cứu>`. Ví dụ: `http://192.168.1.50:5173/tracuu/?code=ABCD…`.

- **Đang dùng IP:** đặt `PUBLIC_LOOKUP_URL=http://<IP-server>:5173/tracuu/`. IP dạng `192.168.x.x` là **mạng nội bộ**: chỉ máy trong công ty quét QR mở được, khách ở ngoài thì không. Giai đoạn này chỉ nên dùng để thử.
- Dưới QR luôn in **mã tra cứu dạng chữ**. Khách có thể gõ tay mã này vào trang tra cứu, kể cả khi QR trỏ tới địa chỉ cũ.
- Admin xem QR đang trỏ tới đâu ở đầu trang **Hồ sơ kết quả** (`/admin/results`).

### Các bước khi có tên miền (ví dụ `tracuu.cfccobay.com`)

1. **Đưa trang tra cứu ra Internet bằng Cloudflare Tunnel** (không cần IP tĩnh, không phải mở cổng router, có HTTPS miễn phí):
   - Thêm tên miền vào Cloudflare. Cài `cloudflared` trên Windows server, rồi tạo tunnel trỏ `https://tracuu.cfccobay.com` tới `http://localhost:5173`.
   - **Chỉ cho ra ngoài** trang `/tracuu/` và 2 API `/api/v1/public/results/lookup`, `/api/v1/public/results/download`. Chặn mọi đường dẫn khác bằng Cloudflare Access/WAF hoặc quy tắc của tunnel. Trang nhân viên vẫn chỉ dùng trong mạng LAN.
2. **Sửa `.env`:**
   ```
   PUBLIC_LOOKUP_URL=https://tracuu.cfccobay.com/tracuu/
   PUBLIC_CLIENT_IP_HEADER=CF-Connecting-IP
   ```
   `PUBLIC_CLIENT_IP_HEADER` giúp giới hạn số lần tra cứu tính theo IP thật của khách. Nếu trang nhân viên cũng chạy qua HTTPS thì đặt thêm `SECURE_COOKIE=true`, và thêm địa chỉ HTTPS đó vào `FRONTEND_ORIGIN`.
3. **Khởi động lại backend** (đóng cửa sổ backend rồi chạy lại `start-backend.ps1`).
4. **Cập nhật QR cho phiếu đã phát hành:** vào `/admin/results` bằng tài khoản Admin, kiểm tra địa chỉ QR mới, rồi bấm **"Tạo lại PDF … phiếu đã phát hành"**.
   - Số phiếu và mã tra cứu giữ nguyên, chỉ QR trong PDF đổi sang tên miền.
   - Phiếu dùng file tải lên tự giữ bố cục của file đó, nhưng QR cũng không được đóng lại.
   - Phiếu đã **in giấy** thì không đổi được: khách dùng mã tra cứu dạng chữ.
5. **SEO trang tra cứu:** trong `source/public/tracuu/index.html`, đổi `<link rel="canonical">` thành `https://tracuu.cfccobay.com/tracuu/`. Thêm `sitemap.xml` và dòng `Sitemap:` vào `source/fe/public/robots.txt`. Build lại frontend.

> Giữ nguyên đường dẫn `/tracuu/?code=…` mãi về sau, chỉ thay phần tên miền. Đổi đường dẫn thì mọi QR đã phát hành sẽ hỏng.
