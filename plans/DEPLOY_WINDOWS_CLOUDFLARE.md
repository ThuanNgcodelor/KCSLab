# Triển khai Windows + Cloudflare Tunnel — `lab.cfc.io.vn`

Trạng thái: **đề xuất, chưa chạy thử trên Windows thật**, chưa có server. Nền tảng chạy Windows đã có tài liệu ở [RUN_WINDOWS_SERVER.md](../RUN_WINDOWS_SERVER.md) (Laragon + Java 21 + Apache + LibreOffice). Plan này chỉ **thêm phần đưa ra Internet bằng Cloudflare Tunnel** và phần vận hành, không đổi kiến trúc.

## Mục tiêu

- Nhân viên và khách mở `https://lab.cfc.io.vn` từ bất kỳ đâu. QR trên phiếu KQKN trỏ `https://lab.cfc.io.vn/tracuu/`.
- Không cần IP tĩnh, không mở cổng router. HTTPS do Cloudflare cấp.
- Một máy Windows chạy: MySQL + Redis (Laragon), Apache (Laragon, phục vụ FE + proxy `/api` `/ws`), backend Java, `cloudflared`.

## Giải đáp nhanh

| Câu hỏi | Trả lời |
|---|---|
| Cần Docker Desktop không? | **Không.** Windows chạy không Docker. LibreOffice cài bình thường như một phần mềm. |
| LibreOffice để làm gì? | Chuyển DOCX KQKN sang PDF (`soffice --convert-to pdf`). Cài bản Windows vào `C:\Program Files\LibreOffice`. Nếu không tự tìm được thì đặt `REPORT_OFFICE_COMMAND`. |
| Cài những gì? | Laragon Full, JDK 21 (Temurin), Node.js 22 LTS, LibreOffice, Git, `cloudflared`. Maven tự tải khi build. |
| Cần cấu hình gì trước? | Cloudflare trước (bước A), vì chưa cần server. Bước B trở đi làm khi có máy. |
| Tên miền | `cfc.io.vn` đã có trong Cloudflare, DNS chưa có bản ghi nào (đúng ý, Tunnel sẽ tự thêm). Chỉ dùng **một** hostname `lab.cfc.io.vn`. |

## Quyết định cần người dùng chốt

1. **Bảo vệ trang nhân viên.** Phần mềm công khai trên Internet thì trang đăng nhập cũng bị dò mật khẩu.
   - **Phương án 1 (khuyến nghị):** Cloudflare Access (Zero Trust, gói free tới 50 người dùng). Nhân viên nhập email, nhận mã, rồi mới thấy trang đăng nhập. Hai đường dẫn của khách (`/tracuu/*`, `/api/v1/public/*`) để chế độ Bypass.
   - Phương án 2: không dùng Access, chỉ dựa vào đăng nhập của ứng dụng cộng giới hạn tốc độ ở Cloudflare cho `/api/v1/auth/login`. Đơn giản hơn nhưng yếu hơn.
2. **Nhân viên vào từ mạng LAN bằng IP hay luôn dùng tên miền?** Khuyến nghị luôn dùng tên miền: khi `SECURE_COOKIE=true` thì đăng nhập qua `http://IP:5173` sẽ không giữ được cookie. Cần Codex kiểm tra hành vi thực tế.
3. Máy server là máy nào, có chạy 24/7 và có UPS không (**cần xác nhận**).

## Các bước

### A. Cloudflare (làm ngay, không cần server)

1. Vào Cloudflare > `cfc.io.vn` > Overview, xác nhận trạng thái **Active**. Nếu còn "Pending", đổi nameserver ở nhà đăng ký tên miền `.io.vn` sang 2 nameserver Cloudflare cung cấp (**cần xác nhận**).
2. Zero Trust (`one.dash.cloudflare.com`) > Networks > Tunnels > **Create a tunnel** > loại *Cloudflared* > đặt tên `cfc-lab`. Chưa cần cài gì, chỉ giữ trang này.
3. Thêm **Public hostname**: `lab.cfc.io.vn`, Service `HTTP` → `localhost:5173`. Cloudflare tự tạo bản ghi CNAME. Apache cổng 5173 đã gom FE, `/api`, `/ws`, nên **một hostname là đủ**. WebSocket qua Cloudflare dùng được.
4. Nếu chọn phương án 1: Zero Trust > Access > Applications > thêm ứng dụng `lab.cfc.io.vn` (Allow = danh sách email nhân viên, thời hạn phiên 24 giờ). Thêm hai ứng dụng **Bypass** cho `lab.cfc.io.vn/tracuu` và `lab.cfc.io.vn/api/v1/public`. Cần kiểm tra trang tra cứu có tải thêm tài nguyên ở đường dẫn khác không (`source/public/tracuu/`) để bypass đủ.
5. Security > WAF > Rate limiting: giới hạn `/api/v1/auth/login` (ví dụ 10 lần/phút/IP) và `/api/v1/public/results/lookup`.
6. Lưu ý: gói free giới hạn **100 MB mỗi lần tải lên** qua Cloudflare. Cần xác nhận người dùng có upload file lớn hơn không (ảnh mẫu nhiều file, file Phiếu 5).

### B. Chuẩn bị máy Windows

1. Windows 10/11 hoặc Server, 8 GB RAM trở lên, ổ SSD. Đặt IP tĩnh trong LAN. Power plan **không Sleep**, giờ hoạt động của Windows Update đặt ngoài giờ làm việc.
2. Cài theo mục 1 của [RUN_WINDOWS_SERVER.md](../RUN_WINDOWS_SERVER.md): Laragon Full, JDK 21 (chọn *Set JAVA_HOME*), Node.js 22 LTS, LibreOffice, Git. Kiểm tra `java -version`, `node -v`.
3. Chép/clone mã nguồn vào `C:\cfc-lab`.
4. Kiểm tra phông chữ: mẫu KQKN dùng phông nào thì cài đủ trên Windows trước khi so sánh PDF (**cần xác nhận**, vì PDF chỉ đẹp khi đủ phông).

### C. Chạy ứng dụng ở LAN (chưa dùng tên miền)

Làm theo mục 2–6 của RUN_WINDOWS_SERVER.md: tạo database `cfc_lab` và user `cfc`, copy `.env.windows.example` thành `.env`, cấu hình Apache `deploy/windows/apache-cfc-lab.conf`, chạy `scripts\windows\build.ps1`, Start All trong Laragon, chạy `start-backend.ps1`. Mở `http://<IP>:5173`, đăng nhập Admin, kiểm tra tạo hồ sơ và xuất KQKN thành PDF. **Đạt bước này rồi mới sang D.**

Nếu chuyển dữ liệu từ máy Linux: `mysqldump` + chép `source/be/storage` (cách làm có trong mục 2 của tài liệu kia).

### D. Cài `cloudflared` và bật Tunnel

1. Tải `cloudflared` Windows (MSI) từ trang chính thức của Cloudflare và cài.
2. Ở trang Tunnel đã tạo ở bước A.2, chọn Windows, chạy lệnh `cloudflared service install <TOKEN>` trong PowerShell **Run as administrator**. Token là bí mật, không dán vào plan, chat hay git.
3. Kiểm tra `services.msc` thấy *Cloudflared agent* chạy tự khởi động, và Tunnel hiện **Healthy**.
4. Mở `https://lab.cfc.io.vn` từ điện thoại dùng 4G (ngoài LAN).

### E. Sửa `.env` cho tên miền

```
FRONTEND_ORIGIN=https://lab.cfc.io.vn
SECURE_COOKIE=true
PUBLIC_LOOKUP_URL=https://lab.cfc.io.vn/tracuu/
PUBLIC_CLIENT_IP_HEADER=CF-Connecting-IP
```

Giữ thêm `http://localhost:5173` trong `FRONTEND_ORIGIN` nếu cần thử tại máy server. Restart backend. Sau đó theo mục 8 của RUN_WINDOWS_SERVER.md: vào `/admin/results`, kiểm tra địa chỉ QR, bấm **Tạo lại PDF** cho các phiếu đã phát hành. Phiếu đã in giấy thì khách gõ mã tra cứu dạng chữ.

### F. Tự chạy khi bật máy và sao lưu

1. Laragon: bật *Run Laragon when Windows starts* và *Start All automatically*. (Hoặc đăng ký MySQL, Redis, Apache thành Windows service.)
2. Backend: Task Scheduler chạy `start-backend.ps1` lúc khởi động (mục 7 của RUN_WINDOWS_SERVER.md).
3. Thứ tự sau khi mất điện: Laragon (MySQL, Redis, Apache) → backend → `cloudflared` (tự chạy). Backend cần đợi MySQL/Redis, nên cho task trễ 1–2 phút.
4. **Sao lưu hằng ngày** bằng Task Scheduler: `mysqldump cfc_lab` và chép `source\be\storage`, giữ ít nhất 14 ngày, một bản ra ổ ngoài hoặc máy khác. Thử khôi phục một lần.

## Việc Codex nên làm (nếu muốn "một file chạy luôn")

| Việc | File dự kiến |
|---|---|
| Script cài một lần: kiểm tra Java/Node/LibreOffice, tạo database và user, sinh `JWT_SECRET`, tạo `.env` từ mẫu, in ra phần còn thiếu | `scripts/windows/setup.ps1` (mới) |
| Script chạy hằng ngày: Start Laragon, build nếu có code mới, chạy backend, kiểm tra `/api` phản hồi | `scripts/windows/run-all.ps1` (mới), có thể gọi `build.ps1` và `start-backend.ps1` |
| Script sao lưu | `scripts/windows/backup.ps1` (mới) |
| Thêm mẫu biến cho tên miền | `.env.windows.example`: dòng chú thích cho bản dùng tên miền |
| Cập nhật tài liệu: thêm mục Cloudflare Tunnel, bỏ ví dụ `tracuu.cfccobay.com` dùng `lab.cfc.io.vn` | `RUN_WINDOWS_SERVER.md` |
| Kiểm tra cookie `Secure` và đăng nhập qua Cloudflare/Apache (header `X-Forwarded-Proto`, WebSocket `/ws`) | đọc `source/be` phần auth/cấu hình, thêm test nếu cần |

## Rủi ro

- Chưa chạy thử trên Windows thật: các bước C có thể phát sinh lỗi môi trường (mod Apache, đường dẫn `soffice`, quyền thư mục).
- Máy server là điểm đơn lẻ: tắt máy hoặc mất mạng là cả hệ thống ngừng. Cần sao lưu và người xử lý sự cố.
- Hệ thống chứa dữ liệu khách thật và ra Internet: chọn phương án 1 (Access), dùng mật khẩu mạnh, đổi mật khẩu Admin mặc định, `.env` không đưa vào git.
- `SECURE_COOKIE=true` làm đăng nhập qua `http://IP` không hoạt động.
- Cloudflare free: giới hạn upload 100 MB mỗi lần; Access free tới 50 người dùng.
- Tên miền `.io.vn` phụ thuộc nhà đăng ký, cần theo dõi ngày gia hạn.

## Tiêu chí nghiệm thu

- `https://lab.cfc.io.vn` mở được từ mạng ngoài, có ổ khóa HTTPS, đăng nhập đủ ba role.
- Thông báo thời gian thực (WebSocket) nhận được trên hai máy.
- Tạo hồ sơ có upload ảnh, chuyển KCS, nhập kết quả, Admin phát hành, PDF có QR trỏ `https://lab.cfc.io.vn/tracuu/?code=…`.
- Điện thoại 4G quét QR mở được trang tra cứu mà **không** bị hỏi đăng nhập Cloudflare; mở `/admin` thì bị chặn nếu chọn phương án 1.
- Khởi động lại máy: không cần thao tác tay mà hệ thống tự lên, kể cả `cloudflared`.
- Một bản sao lưu đã khôi phục thử thành công.
- Sau triển khai Codex cập nhật Project Memory trong `docs/ai/`.
