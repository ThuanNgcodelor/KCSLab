# Triển khai trang tra cứu kết quả

Trang khách nằm ở `source/public/tracuu/`. Dùng hostname công khai riêng nếu hạ tầng cho phép. Địa chỉ nội bộ của nhân viên cần HTTPS để dùng PWA và kết nối WebSocket bảo mật.

## Tuyến công khai

| Tuyến | Mục đích |
|---|---|
| `/tracuu/` | Landing/form tra cứu và đầu vào QR của phiếu phát hành |
| `POST /api/v1/public/results/lookup` | Tra cứu bằng mã và bốn số cuối điện thoại |
| `POST /api/v1/public/results/download` | Tải file được công khai của phiếu, có kiểm tra lại mã, bốn số và file |

Landing `/tracuu/` được index: có `robots` index, JSON-LD (`Organization`, `WebPage`, `FAQPage`) và `source/fe/public/robots.txt` chỉ cho `/tracuu/`. Canonical đang để tương đối (chưa làm S2). Từ 28/11/2026 middleware Vite không còn gửi `X-Robots-Tag: noindex` cho `/tracuu/` (trước đó header này chặn index dù HTML ghi `index`), còn mọi đường dẫn khác gửi `noindex`; có `sitemap.xml` và dòng `Sitemap:` trong `robots.txt`. Host công khai: `lab.cfc.io.vn`. Kế hoạch còn lại: [PUBLIC_LOOKUP_SEO_PLAN.md](../plans/PUBLIC_LOOKUP_SEO_PLAN.md).

QR trên PDF = `PUBLIC_LOOKUP_URL` + `?code=…`. Hiện là IP nội bộ. Khi có tên miền, làm theo mục 8 của [RUN_WINDOWS_SERVER.md](../RUN_WINDOWS_SERVER.md): Cloudflare Tunnel, đổi biến, tạo lại PDF hàng loạt. Giữ đường dẫn `/tracuu/?code=` cố định.

Chỉ đưa landing vào sitemap. Phản hồi kết quả/API dùng `no-store` và `noindex`. QR và liên kết tra cứu dùng URL HTTPS đã cấu hình. Không đưa mã tra cứu và số điện thoại vào trang có index. Reverse proxy công khai chỉ forward hai API trên. Giữ login, workspace nhân viên, WebSocket và các API nội bộ trên origin nội bộ. Nếu reverse proxy cung cấp IP client, cấu hình `app.public.client-ip-header` theo header của proxy đã tin cậy.

## Nghiệm thu

1. Thử QR từ một PDF đã phát hành qua hostname cấu hình, với bốn số cuối đúng và sai.
2. Thử PDF đã thu hồi, mã ngẫu nhiên, file nội bộ và giới hạn tần suất. Kiểm tra header cache, robots và trường dữ liệu trả về.
3. Xác thực URL canonical, sitemap và Search Console sau khi đã có hostname thật.
