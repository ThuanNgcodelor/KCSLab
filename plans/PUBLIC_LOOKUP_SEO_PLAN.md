# Plan — SEO trang tra cứu `https://lab.cfc.io.vn/tracuu/`

**Ngày lập:** 28/11/2026 · **Người lập:** Claude (plan) · **Người code:** Codex · **Trạng thái:** ĐỀ XUẤT — chưa triển khai, chưa kiểm thử sau thay đổi

Liên quan: [docs/PUBLIC_LOOKUP_SEO.md](../docs/PUBLIC_LOOKUP_SEO.md) (thiết kế cũ), [RUN_WINDOWS_SERVER.md](../RUN_WINDOWS_SERVER.md) mục 8.

## 0. Quyết định đã chốt (28/11/2026, theo chủ dự án)

1. **Chỉ SEO `https://lab.cfc.io.vn/tracuu/`.** Host `lab.cfc.io.vn` giữ nguyên là host cuối cùng (bỏ phương án `tracuu.cfccobay.com`). Canonical/sitemap/JSON-LD/QR đều dùng host này.
2. **Hệ quản trị (đăng nhập, workspace nhân viên, API nội bộ) tuyệt đối không SEO** — mọi đường dẫn ngoài `/tracuu/` đều `noindex`. Cách hiểu của mình: `lab.cfc.io.vn` đang phục vụ cả hai (gốc `/` là app nhân viên). Nếu hệ quản trị nằm ở host khác trỏ về cùng server, áp dụng đúng quy tắc này cho host đó (xem S9).
3. Chủ dự án quản lý DNS/Cloudflare của `cfc.io.vn`, nên xác minh Search Console bằng TXT DNS được. `www.cfccobay.com` **không** do chủ dự án quản lý → **bỏ mọi bước backlink/sửa website đó** (S7.4); `sameAs`/liên kết ngược chỉ đặt khi tự tay thêm được.
4. Server thật chạy `npm run build` + `npx vite preview --host 0.0.0.0 --port 5173` (xem §0.1).
5. Không cần trả lời nữa: Q-SEO-3, Q-SEO-4 (dùng nội dung đang có trong `index.html`, không bổ sung dịch vụ/chứng nhận mới).

### 0.1 `vite preview` có dùng được không?

**Dùng được cho quy mô hiện tại, có điều kiện.** Tài liệu Vite ghi `preview` chỉ để xem thử bản build, không phải server production được gia cố, nên cần biết giới hạn:

| Điểm | Đánh giá |
|---|---|
| Middleware `serveLanding` có chạy ở preview không | **Có** — `vite.config.js` đăng ký qua `configurePreviewServer`. Vì vậy header `noindex` bạn thấy trên site thật đến từ chính nó, và sửa S1 trong file này **có hiệu lực** với `vite preview` |
| Proxy `/api`, `/ws`, `allowedHosts` | Preview dùng lại cấu hình `server.proxy`/`server.allowedHosts` nếu không đặt riêng → giữ nguyên được |
| `/tracuu/*` đọc từ đâu | `landingRoot = source/public/tracuu` (đọc thẳng thư mục nguồn, **không** phải `dist`). Sửa `index.html` landing có hiệu lực ngay, không cần build |
| `robots.txt`, `sitemap.xml` | Nằm ở `source/fe/public/` → vào `dist/` khi build → **phải `npm run build` lại** rồi (nên) restart preview |
| Sập thì sao | Không tự khởi động lại. Nên chạy bằng dịch vụ (NSSM/Task Scheduler/pm2) có auto-restart và ghi log |
| Nén/cache | Preview không tối ưu; Cloudflare phía trước nén và có thể cache file tĩnh → đặt `Cache-Control` đúng (S1) để Cloudflare cache được |
| Bảo mật | Preview không có WAF/rate limit riêng; mọi chặn đường dẫn phải làm ở Cloudflare (xem S9) |
| SPA fallback | Đường dẫn lạ trả `index.html` 200 (soft 404). Với `/tracuu/*` đã trả 404 thật bởi `serveLanding`; đường dẫn khác chấp nhận vì đã `noindex` + Disallow |

Khuyến nghị: **giữ `vite preview` trước**, sửa S1–S3, chạy thành dịch vụ có auto-restart. Chuyển nginx/Apache (đã có sẵn `deploy/windows/apache-cfc-lab.conf`) khi lưu lượng tăng hoặc cần nén/cache chuẩn.

## 1. Vấn đề

Tìm "cfc tra cứu" trên Google không ra trang `lab.cfc.io.vn/tracuu/`.

## 2. Chẩn đoán (đã đo trực tiếp ngày 28/11/2026 bằng `curl` tới site thật)

| # | Phát hiện | Bằng chứng | Mức độ |
|---|---|---|---|
| 1 | **Trang trả header `X-Robots-Tag: noindex, nofollow`**. Header này **thắng** thẻ `<meta name="robots" content="index, follow">` trong HTML → Google bị cấm lập chỉ mục | Phản hồi thật của `/tracuu/` có `x-robots-tag: noindex, nofollow`. Nguồn: `source/fe/vite.config.js:33` (hàm `serveLanding`) | **Nghiêm trọng — nguyên nhân chính** |
| 2 | `Cache-Control: no-store` cho cả HTML, CSS, JS, ảnh | Cùng hàm `serveLanding` (dòng 32) → chậm, Googlebot/trình duyệt không cache được, ảnh hưởng Core Web Vitals | Cao |
| 3 | **Không có sitemap thật**: `/sitemap.xml` trả 200 nhưng là **trang HTML của app React** (soft 404) | `Content-Type: text/html`, nội dung là `<!doctype html>… CFC Lab` | Cao |
| 4 | Canonical là đường dẫn tương đối `/tracuu/`; không có `og:url`, `og:image`, `twitter:image` (card chỉ `summary`) | `source/public/tracuu/index.html:12-18` | Trung bình |
| 5 | JSON-LD dùng `@id: "#cfc"` tương đối, thiếu `logo`, `sameAs`, `url` của WebPage, `BreadcrumbList` | `index.html:26-` | Trung bình |
| 6 | Trang chủ `lab.cfc.io.vn/` là **ứng dụng nhân viên (màn đăng nhập)**, title "CFC Lab Management", `robots.txt` chặn `/`. Google không có "trang chủ" nào để neo site | Phản hồi `/` 200, `<title>CFC Lab Management</title>` | Trung bình + **bảo mật** (xem §7) |
| 7 | Tên miền/subdomain mới, chưa Search Console, chưa có backlink (không có bằng chứng `www.cfccobay.com` đã trỏ về) | Không đo được từ bên ngoài — **cần xác nhận** | Cao (về xếp hạng) |
| 8 | Ảnh hero `lab-hero.png` 1,9 MB (có bản `.webp` 122 KB); font tải từ Google Fonts chặn render | `source/public/tracuu/` | Thấp–TB |

**Kết luận:** dù bạn sửa mọi thứ khác, #1 phải sửa trước, nếu không Google **sẽ không bao giờ** hiện trang này. Sau khi sửa, thường cần vài ngày đến vài tuần để Google thu thập lại; tôi không thể đảm bảo thứ hạng cụ thể cho từ khóa "cfc tra cứu".

Ghi chú: file `source/public/README.md` ghi "metadata `noindex` vẫn giữ cho tới khi có domain…" và `docs/PUBLIC_LOOKUP_SEO.md` ghi "được index" — hai tài liệu mâu thuẫn nhau và với code; sau khi sửa cần cập nhật cả hai.

## 3. Mục tiêu và từ khóa

Mục tiêu: người tìm "tra cứu kết quả kiểm nghiệm CFC / CFC Lab" thấy đúng trang, snippet đẹp, link chia sẻ có ảnh xem trước. **Không** lập chỉ mục bất kỳ trang kết quả hay dữ liệu khách nào.

Từ khóa đề xuất (cần LAB xác nhận cách khách thực sự gọi):
- Chính: `tra cứu kết quả kiểm nghiệm CFC`, `CFC Lab tra cứu`, `tra cứu phiếu kết quả CFC Cò Bay`.
- Phụ: `CFC Lab Cần Thơ`, `phòng kiểm nghiệm CFC Cò Bay`, `kiểm nghiệm sầu riêng CFC` (chỉ dùng nếu LAB thực sự cung cấp — **cần xác nhận**, không tự thêm dịch vụ/chứng nhận chưa được xác nhận).
- Lưu ý: "CFC" là tên viết tắt dùng chung nhiều tổ chức → cần kèm "Lab", "Cò Bay" hoặc "Cần Thơ" để phân biệt.

## 4. Các bước (giao Codex, theo thứ tự ưu tiên)

### S1 — Gỡ chặn index (làm trước, ~15 phút, rủi ro thấp)

File: `source/fe/vite.config.js` (`serveLanding`).
- **Bỏ** `X-Robots-Tag: noindex, nofollow` cho các phản hồi của landing (HTML, CSS, JS, ảnh). Giữ nguyên `noindex` ở API công khai (`PublicResultController.java:74`) và mọi route nhân viên.
- Đổi cache: HTML `public, max-age=0, must-revalidate`; tài nguyên tĩnh (`.css .js .png .webp`) `public, max-age=86400` (hoặc đổi tên file có hash rồi `immutable`). Cái đang sai hiện giờ là `no-store` cho tất cả.
- Giữ `Referrer-Policy: no-referrer` (tránh rò `?code=` qua referrer).
- **Quan trọng:** đây là Vite; cần xác nhận server thật đang chạy `vite dev`, `vite preview` hay Apache/nginx. Nếu có proxy/Cloudflare thêm header riêng thì sửa ở đó (kiểm lại bằng `curl -I` trên domain thật sau khi deploy). Cả Cloudflare cache cũng phải purge.
- Test: `curl -sI https://lab.cfc.io.vn/tracuu/` **không** còn `x-robots-tag: noindex`; API vẫn còn.

### S2 — Meta, canonical, Open Graph (file `source/public/tracuu/index.html`)

- **Canonical tuyệt đối** `https://lab.cfc.io.vn/tracuu/` (host lấy từ cấu hình build, ví dụ biến `PUBLIC_LOOKUP_URL`; tránh hard-code lệch domain). Trang mở bằng `?code=…` vẫn canonical về `/tracuu/` sạch → không sinh bản trùng và không rò mã vào chỉ mục.
- `<title>` ≤ 60 ký tự, có từ khóa chính, ví dụ: `Tra cứu kết quả kiểm nghiệm CFC Lab | CFC Cò Bay Cần Thơ` (kiểm độ dài; hiện tại đã gần đúng, chỉ nên thêm "CFC" gần đầu).
- `<meta name="description">` 120–155 ký tự, có hành động ("Nhập mã tra cứu và 4 số cuối điện thoại…"). Hiện tại 190+ ký tự, sẽ bị cắt.
- Open Graph: `og:url`, `og:image` (+ `og:image:width/height/alt`), `og:title`, `og:description`, `og:locale=vi_VN` (đã có). **Cần tạo ảnh 1200×630** từ bộ nhận diện (`guideline-assets/`), dưới 300 KB, **không** chứa thông tin khách.
- Twitter: đổi `summary` → `summary_large_image`, thêm `twitter:image`, `twitter:title`, `twitter:description`.
- Giữ `theme-color`, `lang="vi"`, `viewport`. **Không** thêm `meta keywords` (Google bỏ qua từ lâu). Không cần `hreflang` (chỉ một ngôn ngữ).
- Favicon: thêm `favicon.ico` 48×48 và/hoặc `favicon-48.png` (Google yêu cầu bội số 48px) cho kết quả tìm kiếm; `apple-touch-icon` đã có.
- `robots`: giữ `index, follow, max-image-preview:large`.

### S3 — Sitemap và robots

- Tạo `source/public/tracuu/../sitemap.xml` hoặc đặt trong `source/fe/public/sitemap.xml` (vì `robots.txt` đang ở đó), nội dung chỉ **một URL** `https://lab.cfc.io.vn/tracuu/` + `lastmod` (cập nhật khi nội dung đổi). Trả `Content-Type: application/xml`.
- `source/fe/public/robots.txt`: thêm dòng `Sitemap: https://lab.cfc.io.vn/sitemap.xml`. Giữ `Allow: /tracuu/` + `Disallow: /`. Kiểm tra CSS/JS/ảnh landing nằm dưới `/tracuu/` nên không bị chặn render.
- Sửa SPA fallback để đường dẫn không tồn tại **trả 404 thật** thay vì 200 + index.html (đặc biệt `/sitemap.xml`, `/robots.txt`, `/tracuu/<không có>`). Với Apache: `deploy/windows/apache-cfc-lab.conf` dòng ~34 (`RewriteCond`); với Vite: kiểm cấu hình `appType`.
- Quy ước tiền tố: giữ `/tracuu/?code=` cố định (đổi đường dẫn thì QR đã in hỏng).

### S4 — Dữ liệu có cấu trúc (JSON-LD)

Làm lại trong `index.html`:
- `@id` tuyệt đối (`https://lab.cfc.io.vn/#organization`, `…/tracuu/#webpage`).
- `Organization`: `name`, `alternateName`, `url` (website chính `https://www.cfccobay.com/`), `logo` (URL tuyệt đối tới `cfc-logo.png`, ≥112×112), `contactPoint` (hotline `1900 5307`), `address`; `sameAs` (Facebook/Zalo OA/YouTube — **cần LAB cung cấp**, không bịa).
- `WebSite` (`name`, `url`, `inLanguage`, `publisher`) và `WebPage` (`url`, `isPartOf`, `about`, `breadcrumb`) + `BreadcrumbList` (Trang chủ CFC → Tra cứu).
- Giữ `FAQPage` làm ngữ nghĩa nhưng **đừng kỳ vọng** hiện rich result: Google chỉ hiển thị FAQ rich result cho một số loại site (chủ yếu chính phủ/y tế). Nội dung FAQ phải khớp chữ hiển thị trên trang.
- Không dùng `AggregateRating`, `Review`, giải thưởng, chứng nhận (ISO 17025/VILAS…) nếu LAB chưa xác nhận → thông tin sai làm mất uy tín và có thể bị phạt thủ công.
- Kiểm bằng Rich Results Test / Schema Markup Validator.

### S5 — Nội dung hiển thị (SEO on-page)

Trang hiện chỉ là form + 3 bước + FAQ. Để Google hiểu trang và khách tìm thấy:
- `<h1>` chứa cụm từ chính: `Tra cứu kết quả kiểm nghiệm CFC Lab` (hiện: "Tra cứu kết quả kiểm nghiệm" — thêm "CFC Lab").
- Thêm đoạn giới thiệu ngắn 80–150 chữ: CFC Lab là ai, thuộc CFC Cò Bay, phục vụ loại mẫu gì (**nội dung do LAB xác nhận**).
- Địa chỉ, hotline, email hiển thị thật trong HTML (đã có một phần ở khối liên hệ) và **link ngược về `https://www.cfccobay.com/`**.
- Alt text ảnh có nghĩa; ảnh hero `width/height` đầy đủ.
- Nội dung phải có sẵn trong HTML (không phụ thuộc JS) — đã đúng cho phần giới thiệu/FAQ; kết quả tra cứu render bằng `lookup.js` và **không** cần index.

### S6 — Tốc độ (Core Web Vitals)

- Dùng `lab-hero.webp` (122 KB) qua `<picture>`; bỏ hoặc chỉ giữ PNG 1,9 MB làm dự phòng; `fetchpriority="high"` + `<link rel="preload" as="image">` cho ảnh LCP.
- Font: self-host (WOFF2, subset tiếng Việt) hoặc giữ Google Fonts với `display=swap` (đã có) và `preconnect` (đã có); cân nhắc bỏ bớt họ font (đang tải 3 họ).
- Bật nén (Cloudflare tự nén; kiểm `content-encoding`), cache như S1.
- Kiểm bằng PageSpeed Insights (mobile). Mục tiêu đề xuất: LCP < 2,5 s, CLS < 0,1, INP < 200 ms — đo thực tế rồi chốt, không coi là cam kết.

### S7 — Search Console, Bing, liên kết ngoài (không phải code, làm tay)

1. Xác minh `lab.cfc.io.vn` (hoặc cả `cfc.io.vn`) trên **Google Search Console**; cách dễ nhất là TXT DNS trong Cloudflare. **Cần xác nhận ai sở hữu `cfc.io.vn`** và tài khoản Google dùng quản lý.
2. Gửi `sitemap.xml`; dùng **URL Inspection → Request indexing** cho `/tracuu/`. Mục "Page indexing" sẽ nói thẳng lý do nếu còn bị loại (ví dụ "Excluded by noindex tag").
3. Làm tương tự **Bing Webmaster Tools**.
4. ~~Đặt liên kết từ `www.cfccobay.com`~~ — **bỏ** (không quản lý site đó). Thay bằng những nơi chủ dự án tự quản lý: trang Facebook/Zalo OA/YouTube của CFC (nếu có quyền), chữ ký email, và **in link/QR `https://lab.cfc.io.vn/tracuu/` trên phiếu KQKN** (đã có QR). Liên kết ngoài vẫn là yếu tố mạnh cho tên miền mới; nếu không có thì chấp nhận xếp hạng chậm hơn.
5. (Tùy chọn, **cần xác nhận**) Tạo **Google Business Profile** cho CFC Lab Cần Thơ (địa chỉ, hotline, giờ làm việc) để hiện trên Maps/bảng thông tin khi tìm "CFC Lab".
6. Thêm Search Console theo dõi truy vấn "cfc tra cứu" sau 2–4 tuần.

### S8 — Host (đã chốt)

Host cuối cùng: `lab.cfc.io.vn`. Cập nhật `RUN_WINDOWS_SERVER.md` mục 8 (đang ghi ví dụ `tracuu.cfccobay.com`) cho khớp. Giữ `/tracuu/?code=` cố định vì QR đã in.

### S9 — Hệ quản trị không SEO (BẢN CHỐT 28/11: nhân viên dùng cả LAN lẫn Internet, giữ chung host `lab.cfc.io.vn`, không chặn truy cập)

**Phạm vi làm:** chỉ `noindex`, không chặn đường dẫn, không dùng Cloudflare Access/host riêng.
1. `source/fe/index.html` (app React): thêm `<meta name="robots" content="noindex, nofollow">`.
2. `source/fe/vite.config.js`: thêm một middleware cho `configureServer` và `configurePreviewServer`, đặt **trước** các middleware khác: với mọi đường dẫn **không** bắt đầu bằng `/tracuu/` và không phải `/robots.txt`, `/sitemap.xml`, `/favicon*` thì set `X-Robots-Tag: noindex, nofollow`. Landing `/tracuu/` tuyệt đối **không** nhận header này (liên quan S1).
3. `robots.txt` giữ `Allow: /tracuu/` + `Disallow: /`.
4. Test: `curl -sI https://lab.cfc.io.vn/` có `x-robots-tag: noindex`; `curl -sI https://lab.cfc.io.vn/tracuu/` không có; `curl -sI .../api/v1/public/results/lookup` vẫn `noindex`.

**Rủi ro đã được chủ dự án chấp nhận:** màn đăng nhập nhân viên vẫn truy cập được từ Internet (cần cho nhân viên làm việc từ xa). Không còn trong phạm vi plan này; nếu sau này muốn tăng bảo mật có thể thêm giới hạn đăng nhập, 2FA cho ADMIN hoặc Cloudflare Access.

<details><summary>Phương án chặn bằng Cloudflare (KHÔNG áp dụng, giữ để tham khảo)</summary>

Mục tiêu: chỉ `/tracuu/` + 2 API công khai + file tĩnh của landing đi ra Internet.
- **WAF custom rule (Cloudflare):** với host công khai, `Block` (hoặc yêu cầu Cloudflare Access) mọi đường dẫn **không** thuộc: `/tracuu/*`, `/api/v1/public/results/lookup`, `/api/v1/public/results/download`, `/robots.txt`, `/sitemap.xml`, `/favicon*`. Nhân viên dùng qua LAN hoặc qua Access/VPN.
- Nếu muốn nhân viên vào qua Internet: dùng **host riêng** (ví dụ một subdomain khác của `cfc.io.vn`) đặt sau Cloudflare Access, và thêm **Transform Rule → Modify Response Header**: `X-Robots-Tag: noindex, nofollow` cho host đó. Trên host quản trị chặn luôn `/tracuu/` để không có bản trùng nội dung.
- Trong `source/fe/index.html` (app React) thêm `<meta name="robots" content="noindex, nofollow">` để chắc chắn dù lớp ngoài cấu hình sai.
- Lưu ý: `robots.txt` `Disallow: /` ngăn bot đọc nội dung nhưng không đảm bảo URL không lên kết quả; vì vậy cần cả `noindex` (meta/header) lẫn chặn truy cập.
- Giải quyết luôn cảnh báo §7: màn đăng nhập hiện đang public.
</details>

## 5. Những thứ "meta" bạn hỏi — cái nào có tác dụng

| Thẻ | Tác dụng | Làm không |
|---|---|---|
| `<title>`, `meta description` | Rất quan trọng cho snippet | Có (S2) |
| `canonical` tuyệt đối | Chống trùng URL (đặc biệt `?code=`) | Có (S2) |
| `meta robots` / `X-Robots-Tag` | Cho/không cho index; **header đang chặn** | Sửa (S1) |
| Open Graph + Twitter Card | Ảnh/tiêu đề khi chia sẻ Zalo/Facebook; không tăng hạng trực tiếp | Có (S2) |
| JSON-LD (Organization, WebSite, Breadcrumb, FAQ) | Giúp Google hiểu thương hiệu; hiển thị logo/tên | Có (S4) |
| `meta keywords` | Google bỏ qua | **Không** |
| `hreflang` | Chỉ khi có nhiều ngôn ngữ | Chưa |
| `sitemap.xml`, `robots.txt` | Khám phá trang | Có (S3) |
| Favicon chuẩn | Hiện icon cạnh kết quả | Có (S2) |

## 6. Rủi ro

- **Lộ dữ liệu khách**: chỉ trang landing được index; kết quả tra cứu đi qua `POST` và trả `no-store`+`noindex`; `?code=` phải canonical về `/tracuu/` và **không** nằm trong sitemap. Sau S1 phải kiểm lại header của API công khai vẫn `noindex`.
- Mở index mà server chạy `vite dev` (không phải bản build) → chậm, dễ lỗi; nên chạy bản build (xem phần triển khai Windows).
- Khai sai thông tin trong schema/nội dung (dịch vụ, chứng nhận) → vi phạm hướng dẫn Google và uy tín LAB.
- Đổi host sau khi index → mất thứ hạng tạm thời nếu thiếu 301/canonical đúng.
- Kỳ vọng thứ hạng: từ khóa "cfc tra cứu" ngắn, mơ hồ; kết quả phụ thuộc cạnh tranh, tuổi domain và backlink — không cam kết vị trí.

## 7. Phát hiện bảo mật đi kèm (không thuộc SEO, cần xử lý riêng)

`https://lab.cfc.io.vn/` trả **màn đăng nhập ứng dụng nhân viên** ra Internet. Thiết kế trong tài liệu là chỉ mở `/tracuu/` và 2 API tra cứu ra ngoài, còn workspace nhân viên chỉ trong LAN ([RUN_WINDOWS_SERVER.md](../RUN_WINDOWS_SERVER.md) dòng 132). Mình chỉ thấy phản hồi 200 + title của app; **chưa kiểm** xem đăng nhập có dùng được từ ngoài không. Đề xuất: chặn mọi đường dẫn khác bằng WAF/Cloudflare Access hoặc tách host riêng cho nhân viên; hoặc nếu muốn nhân viên dùng qua Internet thì thêm 2FA/giới hạn đăng nhập/Cloudflare Access. **Câu hỏi mở Q-SEC-1.**

## 8. Câu hỏi mở

**Không còn câu hỏi mở.** Đã chốt: host = `lab.cfc.io.vn`; chủ dự án quản lý DNS, không quản lý `www.cfccobay.com`; server = `vite preview`; nhân viên dùng cả LAN và Internet trên cùng host (chỉ `noindex`, không chặn — S9); không có trang mạng xã hội nào để đặt link → **bỏ `sameAs` trong JSON-LD và bước backlink S7.4** (chỉ còn QR/link in trên phiếu KQKN).

## 9. Tiêu chí nghiệm thu

1. `curl -sI https://<host>/tracuu/` không còn `x-robots-tag: noindex`; `Cache-Control` đúng như S1; API `lookup`/`download` vẫn `noindex, nofollow, no-store`.
2. `/sitemap.xml` trả `application/xml` hợp lệ, 1 URL; `/robots.txt` có dòng `Sitemap:`; đường dẫn lạ trả 404 thật.
3. Canonical tuyệt đối; `/tracuu/?code=ABC` vẫn canonical về `/tracuu/`.
4. Rich Results Test/Schema Validator không lỗi; Facebook Sharing Debugger/Zalo xem trước có ảnh đúng.
5. PageSpeed mobile: ghi lại điểm trước/sau và LCP/CLS/INP.
6. Search Console: URL Inspection báo "URL is on Google" (sau khi đã yêu cầu index và chờ), không có "Excluded by noindex".
7. Hồi quy chức năng: nhập mã đúng/sai, giới hạn tần suất, tải PDF, thu hồi phiếu; QR cũ `?code=` vẫn mở được.

## 10. Test cần yêu cầu Codex chạy

- `npm run build` trong `source/fe`, kiểm `dist/tracuu/`, `dist/sitemap.xml`, `dist/robots.txt` có đủ (hàm `closeBundle` chỉ copy thư mục `tracuu`; sitemap/robots nằm ở `public/` của FE nên được Vite copy mặc định — kiểm lại).
- Chạy server build thật và `curl -sI` các URL ở §9 (1–3); chụp Chrome thật ở 390×844, 1440×900 cho landing (không tràn ngang; hero `.webp`).
- Backend: không đổi; chạy lại test public API hiện có (`PublicResultController`) để chắc header `noindex` của API còn nguyên.
- Cập nhật `docs/PUBLIC_LOOKUP_SEO.md`, `source/public/README.md` (hiện mâu thuẫn), `docs/ai/CONTEXT.md` sau khi xong.

## 11. Thứ tự đề xuất

`S1` (gỡ noindex + cache) → `S3` (sitemap/robots/404) → `S2` + `S4` (meta, JSON-LD) → `S7.1–7.3` (Search Console, gửi sitemap, yêu cầu index) → `S5`, `S6` → `S7.4–7.5` (backlink, Business Profile). S1 + S7.2 một mình đã đủ để Google bắt đầu thu thập trang.
