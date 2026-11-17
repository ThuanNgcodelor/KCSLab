# Trang tra cứu CFC

Trang tĩnh riêng tại `/tracuu/`, không cần đăng nhập. Form đã nối với API công khai `POST /api/v1/public/results/lookup` và `/download` (same-origin; reverse proxy phải chỉ chuyển tiếp hai đường dẫn này, không mở `/api/v1/*` khác). Mã tra cứu và 4 số cuối chỉ đi trong body POST, không nằm trong URL. QR chỉ cần mã: `/tracuu/?code=MÃ`; khách vẫn phải nhập 4 số cuối.

Trạng thái: **đã kiểm thử API ở mức tự động (MockMvc), chưa kiểm thử trên Chrome thật và chưa triển khai/public**. Metadata `noindex` vẫn giữ cho tới khi có domain, canonical, sitemap và được duyệt triển khai. Python `http.server` bên dưới không proxy API nên form sẽ báo lỗi kết nối; để thử đầy đủ hãy dùng Vite proxy hoặc reverse proxy.

```bash
python3 -m http.server 4174 --bind 127.0.0.1 --directory source/public
```

Lệnh trên chỉ để xem giao diện trên máy, không dùng làm server production hoặc expose ra Internet.
