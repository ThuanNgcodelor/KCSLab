# Trang tra cứu CFC — bản xem trước nội bộ

Trang tĩnh riêng tại `/tracuu/` không cần đăng nhập. Chưa kết nối API, chưa có QR hoặc kết quả; biểu mẫu bị vô hiệu hóa để tránh hiểu nhầm. Metadata `noindex` là tạm thời cho bản xem trước, không dùng thay bảo mật.

Từ thư mục gốc dự án, chạy:

```bash
python3 -m http.server 4174 --bind 127.0.0.1 --directory source/public
```

Mở `http://127.0.0.1:4174/tracuu/`. Lệnh này chỉ để xem thử trên máy, không dùng làm server production hoặc expose ra Internet. Trang chỉ kết nối tra cứu sau khi có hồ sơ phát hành, API riêng, kiểm soát mã + bốn số điện thoại, rate limit, no-store và kiểm thử bảo mật. Tên miền/canonical/sitemap sẽ chốt khi có hostname thật.
