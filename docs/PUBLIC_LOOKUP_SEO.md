# Kế hoạch trang tra cứu public và SEO

**Cập nhật:** 15/11/2026  
**Trạng thái:** Chưa triển khai runtime; đây là kiến trúc đã chốt.

## Mục tiêu

Hệ thống vận hành là nội bộ. Chỉ một trang public được đưa lên Google để khách tìm các cụm như:

- tra cứu phân bón Cần Thơ;
- tra cứu kết quả kiểm nghiệm CFC;
- phòng kiểm nghiệm phân bón Cần Thơ;
- tra cứu hóa chất Cần Thơ.

Không public toàn bộ React Admin, login, API nội bộ, WebSocket hoặc dữ liệu Phiếu 3.

## Route cần thiết

```text
Public và được SEO:
  /tracuu

Public nhưng không index:
  /tracuu/ket-qua
  /api/public/v1/lookup

Chỉ nội bộ:
  /login
  /admin/*
  /api/v1/*
  /ws/notifications
```

## SEO landing page

`/tracuu` là landing page tĩnh hoặc SSR/HTML có nội dung thật, gồm:

- title: `Tra cứu kết quả kiểm nghiệm phân bón Cần Thơ | CFC`;
- meta description rõ dịch vụ CFC;
- nội dung giới thiệu phòng kiểm nghiệm/CFC tại Cần Thơ;
- hướng dẫn nhập mã tra cứu;
- FAQ có nội dung đúng nghiệp vụ;
- thông tin liên hệ và địa điểm;
- structured data phù hợp như `Organization`/`LocalBusiness`;
- canonical URL;
- sitemap chỉ chứa `/tracuu`.

Không dùng meta keywords để thay cho nội dung hữu ích.

## Kết quả tra cứu

Kết quả không được index hoặc cache:

```http
X-Robots-Tag: noindex, nofollow, noarchive
Cache-Control: no-store
```

HTML kết quả cũng đặt:

```html
<meta name="robots" content="noindex,nofollow,noarchive">
```

Không chỉ dựa vào `robots.txt` để bảo mật. `noindex` là điều khiển index, không phải authentication. Dữ liệu phải được bảo vệ bằng API riêng, mã tra cứu, đuôi số điện thoại/OTP, rate limit và quyền phát hành.

## API public riêng

Không cho public gọi `/api/v1/receipts/{id}`. Tạo API riêng:

```text
POST /api/public/v1/lookup
```

Chỉ đọc bản ghi `RELEASED`, không đọc receipt nháp, mẫu chưa review hoặc dữ liệu nội bộ.

Response public tối thiểu:

```json
{
  "found": true,
  "lookupCode": "...",
  "sampleCode": "CT001-SR-15112026-001",
  "status": "RELEASED",
  "issuedAt": "2026-11-15",
  "resultPdfUrl": "temporary-signed-url"
}
```

Không trả:

- `receivedBy`, `deliveredBy`;
- email, số điện thoại đầy đủ, địa chỉ;
- ID database tuần tự;
- file Phiếu 3 và ảnh lô hàng;
- mapping mã mù, dữ liệu KCS hoặc raw data;
- dữ liệu kết quả chưa phát hành.

## Cloudflare Tunnel

Chỉ route hostname public, ví dụ:

```text
tracuu.example.com → public lookup service
```

Không route:

```text
/admin
/login
/api/v1
/ws/notifications
```

Tạo Cloudflare Access application và policy trước khi publish Tunnel. Origin cũng cần xác thực Access token hoặc chỉ nhận traffic từ Tunnel; không xem Tunnel như thay thế RBAC trong ứng dụng.

Mô hình:

```text
Internet → Cloudflare DNS/Access → Tunnel → public lookup
LAN      → frontend/backend nội bộ → MySQL/Redis
```

Tốt nhất tách frontend public khỏi frontend Admin thay vì chỉ ẩn route bằng JavaScript.

## Thứ tự triển khai

1. Tạo `result_releases` và trạng thái phát hành.
2. Tạo mã tra cứu ngẫu nhiên, không dùng ID database.
3. Snapshot dữ liệu kết quả đã review.
4. Tạo PDF versioned và checksum.
5. Tạo API public projection riêng.
6. Thêm rate limit, audit lookup, no-store và signed URL.
7. Làm landing page `/tracuu`, sitemap và structured data.
8. Cấu hình Cloudflare Tunnel chỉ cho public hostname.
9. Đưa hostname lên Google Search Console và submit sitemap.
10. Chỉ sau khi test bảo mật mới public QR trỏ tới trang tra cứu.

## Tài liệu tham khảo

- Google robots/noindex: https://developers.google.com/search/docs/crawling-indexing/block-indexing
- Google sitemap/recrawl: https://developers.google.com/search/docs/crawling-indexing/sitemaps/build-sitemap
- Cloudflare publish self-hosted app: https://developers.cloudflare.com/cloudflare-one/access-controls/applications/http-apps/self-hosted-public-app/
- Cloudflare Tunnel: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/get-started/create-remote-tunnel/
