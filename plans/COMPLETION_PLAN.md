# Kế hoạch hoàn thiện dự án — FE "wow", trang tra cứu public, phát hành hồ sơ, Zalo/SMS

**Ngày lập:** 16/11/2026 · **Người lập:** Claude (chỉ lập plan, Codex triển khai) · **Trạng thái:** Đề xuất, chưa triển khai

> Plan này bổ sung, không thay thế `plans/CURRENT.md`, `docs/PUBLIC_LOOKUP_SEO.md`, `docs/INTAKE_NOTIFICATIONS.md`, `docs/SAMPLE_CODING.md`. Phần Admin dashboard được giữ ở mức nêu phụ thuộc vì người dùng nói Admin chưa làm.

## 0. Quyết định đã chốt với người dùng (16/11/2026)

| # | Quyết định |
|---|---|
| D1 | Trang tra cứu public là **site riêng, HTML tĩnh**, tách khỏi Admin React (`source/public`). |
| D2 | Tra cứu **cả hai cách**: nhập tay (mã tra cứu + 4 số cuối SĐT) và quét QR trên phiếu kết quả. |
| D3 | Khách có thể tải **PDF, Word và Excel** của hồ sơ phát hành. Đề xuất giảm rủi ro ở mục 4.3 (cờ hiển thị theo từng file). |
| D4 | **Technician tải file lên, Admin duyệt phát hành**; Admin duyệt phải khác người lập/tải. |
| D5 | Zalo OA + SMS ở **khâu đầu tiên (tiếp nhận)** nhưng triển khai **cuối cùng**. Việc hành chính (xác thực OA, duyệt template, chọn nhà cung cấp SMS) nên **bắt đầu song song ngay** vì thời gian chờ duyệt dài. |

Thuật ngữ cần xác nhận: người dùng gọi cuối chuỗi là "biên bản nghiệm thu"; trong tài liệu dự án là **Phiếu kết quả KQKN-DV** (phát hành sau Phiếu 5). Plan dùng tên "hồ sơ kết quả" cho cả hai và coi đó là cùng một bước, **cần người dùng xác nhận**.

## 1. Rà soát toàn bộ Frontend (bằng chứng từ source và Chrome ngày 16/11)

**Quy mô:** 2.453 dòng JS/JSX, 20 trang, 1 file `styles.css` 108 KB / 1.116 dòng, bundle 1 file ~420 KB, không TypeScript/UI library/test/ESLint.

### 1.1 Điểm mạnh giữ lại
- Feature-based rõ ràng; `httpClient` tập trung (CSRF, refresh); `ConfirmDialog`, loading CFC, toast realtime đã có.
- Bộ nhận diện CFC nhất quán: xanh Forest/Leaf, Space Grotesk + DM Sans, logo cò bay.
- Phân quyền route/menu theo role chạy đúng; không tràn ngang ở 390/1024/1440/2560 trên các route đã rà.

### 1.2 Vấn đề tìm thấy (ưu tiên giảm dần)
| Mức | Vấn đề | Bằng chứng |
|---|---|---|
| Cao | **CSS là một khối 108 KB chồng lớp "append"**: nhiều rule cùng selector bị ghi đè cuối file, màu hard-code, không có design token cho Admin → sửa chỗ này vỡ chỗ khác (đã gặp: form nhân viên mobile, login bị cắt, sidebar HD). | `styles.css`: 41 `@media`, token chỉ ở `:root` của login |
| Cao | **Chữ < 12px ở màn ≥ 2200px** trên gần mọi trang, vi phạm `source/fe/AGENTS.md`. | Quét 2560×952: 8–11px ở sidebar, `small`, bảng, phân trang, badge |
| Cao | **Không code-splitting**: người dùng Tiếp nhận tải luôn code Testing/Admin. | 1 bundle 420 KB, `App.jsx` import tĩnh 20 trang |
| Trung | Font tải bằng `@import` Google Fonts trong CSS (chặn render, hỏng khi mạng LAN không ra Internet). | `styles.css` dòng 1 |
| Trung | Trùng lặp pattern: bảng→card mobile, thẻ KPI, tiêu đề trang, form field, lỗi form được viết lại ở từng trang; 5 kiểu thông báo khác nhau. | `form-error`, `success-note`, `toast`, `intake-toast`, toast realtime |
| Trung | Form: lỗi chỉ ở đầu trang/native tooltip tiếng Anh, không có `*` bắt buộc, chưa cảnh báo "thay đổi chưa lưu", `autocomplete` thiếu. | Rà Phiếu 3/4/5 |
| Trung | A11y: `<main>` lồng nhau; focus không trả về nút gốc khi hủy `ConfirmDialog` ở trang Phiếu 4; nhiều `button` thiếu `type`. | Chrome E2E |
| Thấp | Ngày không đồng nhất (`2026-11-16` vs `16/11/2026`); lời chào lấy "từ cuối của tên" ("Chào nhận."). | Trang nhận mẫu, dashboard |
| Thấp | Chưa có empty-state minh họa, chưa có điều hướng nhanh (tìm kiếm toàn cục), chưa có dark mode. | — |

### 1.3 Chưa kiểm chứng
Hiệu năng trên mạng chậm, trình duyệt Safari/Firefox, trình đọc màn hình thật, dữ liệu lớn (> 500 phiếu).

## 2. Hướng thiết kế "wow" cho Admin (không đổi stack)

**Ý tưởng chủ đạo: "Hành trình của một mẫu"** — mọi màn hình xoay quanh việc nhìn thấy mẫu đang đi đến đâu: *Tiếp nhận → Chuyển KCS → Thử nghiệm → Phát hành*.

### 2.1 Nền tảng (làm trước, ít thấy nhưng quyết định chất lượng)
1. **Design tokens** (`src/styles/tokens.css`): màu, spacing, radius, shadow, type scale có `clamp()`, trạng thái (success/warn/danger/info) có chữ + icon, không chỉ màu.
2. **Tách `styles.css`** thành `styles/base.css`, `layout.css`, `components/*.css` và CSS theo feature; xóa rule bị ghi đè. Không đổi hành vi.
3. **Type scale sàn 12px** áp ở ≥ 2200px bằng token (sửa gốc thay vì vá từng selector).
4. **Self-host font** (woff2 subset tiếng Việt, `font-display: swap`) trong `public/fonts`.
5. **Lazy route** theo role (`React.lazy` + `Suspense` dùng `PageLoader` hiện có); mục tiêu bundle khởi đầu < 200 KB.

### 2.2 Bộ component dùng chung (`src/shared/ui`)
`PageHeader`, `StatCard` (số đếm lên, sparkline nhỏ), `DataTable→CardList` (tự chuyển card ≤ 520px), `FormField` (nhãn, `*`, lỗi cạnh trường, `aria-describedby`), `EmptyState` (minh họa SVG CFC), `Toast` thống nhất 1 hệ thống, `Lightbox/Gallery`, `FileDropzone` (nhiều file, tiến trình, thử lại từng file — dùng lại cho Phiếu 5 và hồ sơ kết quả), `StatusPill`, `Timeline`.

### 2.3 Điểm nhấn "wow" (chọn lọc, có giá trị nghiệp vụ)
| Ý tưởng | Giá trị | Ghi chú |
|---|---|---|
| **Sample Journey stepper** 4 bước hiện trên chi tiết phiếu và dashboard (tiếp nhận → KCS → thử nghiệm → phát hành), thời điểm + actor mỗi bước | Thấy ngay tiến độ, thay bảng chữ | Dữ liệu đã có ở transfer/testing |
| **Dashboard kể chuyện**: KPI đếm số, sparkline 7 ngày, hàng đợi ưu tiên chờ lâu nhất, "việc cần làm hôm nay" | Mỗi role mở ra biết làm gì | Cần API hàng đợi (xem mục 5) |
| **Command palette `Ctrl+K`**: nhảy tới phiếu/mẫu theo mã, tạo phiếu, mở thông báo | Nhanh cho người dùng thao tác nhiều | Dùng endpoint search server-side hiện có |
| **Quét QR/camera nhận mẫu** ở mobile (nhập mã mẫu bằng camera) | Giảm gõ sai mã mẫu | Cần quyết định có nhãn QR trên mẫu không |
| **Mobile-first cho Tiếp nhận**: chụp ảnh trực tiếp, nháp tự lưu, tiến trình upload từng file | Đúng acceptance đã ghi trong plan | |
| **Micro-interaction**: chuyển trang mượt, skeleton, toast có hành động hoàn tác, nút có trạng thái | Cảm giác cao cấp | Tôn trọng `prefers-reduced-motion` |
| **Chế độ tối** (tùy chọn cuối) | Làm việc ca tối | Chỉ làm sau khi token xong |

Không đề xuất: thư viện UI/animation nặng, đổi sang TypeScript hay đổi router — vi phạm `source/fe/AGENTS.md`.

### 2.4 Tiêu chí hoàn thành FE
Quét 4 viewport không tràn ngang; chữ ≥ 12px ở ≥ 2200px; bundle khởi đầu < 200 KB; Lighthouse Accessibility ≥ 95 các trang chính; mọi màn có loading/error/empty; build + Chrome E2E ba role.

## 3. Lộ trình tổng thể và phụ thuộc

```text
F0  FE nền tảng (tokens, tách CSS, font, lazy, a11y)         ── không phụ thuộc
F1  Dashboard theo role + hàng đợi + Admin tổng hợp          ── API aggregate (BE)
G5A Gate mã hóa CFC-TN-14 (sequence, -V/-R, xác minh 2 người)── prerequisite của phát hành
R1  Hồ sơ kết quả: upload PDF/Word/Excel + Admin duyệt        ── cần TEST_REVIEWED (có sẵn) + G5A
R2  Trang tra cứu public + QR + API public                   ── cần R1
F2  Wow polish (journey stepper, Ctrl+K, QR nhận mẫu…)        ── song song sau F0
N*  Zalo OA + SMS (khâu tiếp nhận) ── CUỐI CÙNG
    N0 hành chính (OA, template, SMS provider) chạy NGAY song song vì chờ duyệt lâu
```

Thứ tự đề xuất: **F0 → F1 → G5A → R1 → R2 → F2 → N1–N5**; N0 bắt đầu hôm nay (không cần code).

## 4. R1 — Hồ sơ kết quả: upload PDF / Word / Excel và phát hành

### 4.1 Luồng nghiệp vụ
```text
Phiếu 5 REVIEWED (Admin A kiểm tra)
  → Technician tạo "hồ sơ kết quả" (DRAFT), tải nhiều file PDF/DOCX/XLSX
  → Technician gửi duyệt (SUBMITTED)
  → Admin B (khác người lập, khác người tải) xem file → PHÁT HÀNH (RELEASED) hoặc TRẢ LẠI (RETURNED, bắt buộc lý do)
  → RELEASED: sinh mã tra cứu + QR; khách tra cứu được
  → Cần sửa: tạo VERSION mới; bản cũ giữ lịch sử; THU HỒI (REVOKED) có lý do
```
Quy tắc: một Phiếu 5 `REVIEWED` có tối đa một hồ sơ kết quả hiện hành; mọi bước ghi actor + thời điểm; Admin không tự duyệt hồ sơ do chính mình lập/tải.

### 4.2 Dữ liệu (Flyway V6+, `VARCHAR/TEXT` utf8mb4; kiểm tra version thực tế trước)
- `result_releases`: id, `testing_record_id`, `version`, `status` (DRAFT/SUBMITTED/RETURNED/RELEASED/REVOKED), `result_number` (**cần chốt quy tắc**), `lookup_code` (ngẫu nhiên, không dùng ID DB), `qr_token` (≥ 128 bit, lưu hash), `created_by`, `submitted_at`, `released_by`, `released_at`, `revoke_reason`, `revoked_at`.
- `result_release_files`: id, `release_id`, `kind` (PDF/DOCX/XLSX), `original_name`, `stored_name` (ngẫu nhiên), `size`, `sha256`, `content_type` đã xác thực, `uploaded_by`, `uploaded_at`, **`public_visible`** (cờ hiển thị cho khách), `sort_order`.
- `result_lookup_audit`: thời điểm, `lookup_code` hash, kết quả (found/denied/rate-limited), IP hash, UA rút gọn. Không lưu SĐT.

### 4.3 File và an toàn (theo D3: khách tải được cả ba loại)
- Danh sách cho phép: `.pdf`, `.docx`, `.xlsx`. **Chặn** `.doc/.xls` cũ, `.docm/.xlsm/.xlsb` (macro), mọi file thực thi/archive. Kiểm tra đuôi + MIME + **magic bytes** (ZIP bên trong cho OOXML; kiểm tra không có `vbaProject.bin`, ngoại link/`externalLinks` nếu có thể).
- Giới hạn đề xuất: ≤ 10 MB/file, ≤ 10 file/hồ sơ (**cần xác nhận**; backend hiện giới hạn 5 MB cho receipt attachment — tách cấu hình riêng).
- Lưu tên ngẫu nhiên, checksum SHA-256, không phục vụ trực tiếp từ thư mục web; tải qua endpoint kiểm quyền; `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`.
- Quét virus (ClamAV) nếu LAB có máy chủ; nếu chưa, ghi rõ rủi ro còn lại.
- **Rủi ro D3:** Word/Excel sửa được, dễ bị mạo danh. Giảm thiểu: (a) **PDF là bản chính thức**, trang tra cứu gắn nhãn "Bản chính thức" cho PDF, "Tài liệu kèm theo" cho Word/Excel; (b) cờ `public_visible` theo từng file — Admin tick khi phát hành, mặc định chỉ PDF; (c) hiển thị checksum/mã xác thực để đối chiếu; (d) Excel không chứa công thức ngoại liên kết. Người dùng đã chọn cho phép cả ba loại; cờ này vẫn cho phép bật cả ba.
- Thu hồi: file không còn tải được (410 + thông báo "đã thu hồi/đính chính"), không xóa vật lý ngay.

### 4.4 API nội bộ (`/api/v1/result-releases`)
`POST` tạo, `GET` danh sách phân trang/lọc, `GET /{id}`, `POST /{id}/files` (multipart, nhiều file, trả kết quả từng file), `DELETE /{id}/files/{fileId}` (chỉ DRAFT/RETURNED), `PATCH /{id}/files/{fileId}` (`public_visible`), `POST /{id}/submit`, `POST /{id}/decision` (RELEASE/RETURN), `POST /{id}/revoke`, `GET /{id}/files/{fileId}/content`. Quyền: Technician lập/tải/gửi; Admin duyệt/thu hồi; Receptionist chỉ xem trạng thái (**cần xác nhận**).

### 4.5 Giao diện nội bộ
- Trang **Phát hành kết quả** (`/admin/releases`): danh sách + tab Nháp/Chờ duyệt/Đã phát hành/Thu hồi.
- Chi tiết: `FileDropzone` nhiều file, tiến trình + thử lại từng file, preview PDF (trình duyệt), icon theo loại cho Word/Excel, cột "Hiển thị cho khách", checklist trước khi gửi, lịch sử phiên bản.
- Hàng đợi duyệt cho Admin + thông báo nội bộ realtime; xác nhận bằng `ConfirmDialog`.

### 4.6 Test bắt buộc
Quyền chéo; Admin tự duyệt hồ sơ của mình bị chặn; file giả đuôi (exe đổi `.pdf`); `.docm`/`.xlsm` bị từ chối; file quá lớn; upload nhiều file lỗi một phần; thu hồi; version mới không làm mất bản cũ; tải file đã thu hồi; đồng thời hai Admin cùng duyệt (409).

## 5. R2 — Trang tra cứu public (SEO) + API public

### 5.1 Kiến trúc (theo D1)
```text
source/public/            site tĩnh riêng (Astro hoặc HTML+CSS+JS thuần — chốt khi bắt đầu)
  /tracuu                 landing SEO (HTML tĩnh có nội dung thật)
  /tracuu/ket-qua         kết quả (noindex), render bằng JS gọi API public
  /r/{token}              đích của QR
Backend: /api/public/v1/*  controller + security chain RIÊNG, không dùng cookie/CSRF của Admin
Hạ tầng: Cloudflare Tunnel → hostname public (vd. tracuu.<domain>) chỉ route 3 đường trên
         KHÔNG route /admin, /login, /api/v1, /ws
```
Tách code và deploy khỏi Admin: sự cố hoặc lỗ hổng ở site public không chạm vào Admin.

### 5.2 Trang landing `/tracuu` — thiết kế "wow" và chuẩn SEO
**Bố cục:**
1. **Hero** tối-xanh CFC: logo cò bay lớn (cùng ngôn ngữ trang login), tiêu đề "Tra cứu kết quả kiểm nghiệm", thẻ tra cứu kính mờ nổi giữa: ô *Mã tra cứu* + ô *4 số cuối SĐT* + nút lớn; nút "Quét QR" mở camera trên mobile.
2. **Dải tin cậy**: số liệu thật (số mẫu đã kiểm nghiệm — chỉ hiển thị khi lấy được số thật, không bịa), chứng nhận/năng lực (**cần LAB cung cấp**: ISO/VILAS nếu có).
3. **Quy trình 4 bước** (Tiếp nhận → Chuyển mẫu → Thử nghiệm → Phát hành) dạng timeline động.
4. **Hướng dẫn tra cứu** có ảnh minh họa vị trí mã/QR trên phiếu.
5. **FAQ** (tag `FAQPage`): mã ở đâu, quên mã, kết quả bao lâu có, file nào tải được, bảo mật dữ liệu.
6. **Liên hệ + bản đồ/địa chỉ** (Cần Thơ), giờ làm việc, nút gọi/Zalo.
7. Footer thương hiệu CFC.

**SEO kỹ thuật:** `<title>` "Tra cứu kết quả kiểm nghiệm phân bón Cần Thơ | CFC"; meta description; `canonical`; `lang="vi"`; Open Graph/Twitter card + ảnh OG; JSON-LD `Organization`/`LocalBusiness` + `FAQPage`; `sitemap.xml` chỉ chứa `/tracuu`; `robots.txt` cho phép `/tracuu`, chặn `/tracuu/ket-qua` và `/r/`; HTTPS + HSTS; ảnh `webp/avif` có `width/height`; font self-host; mục tiêu LCP < 2,5 s, CLS < 0,1, không JS chặn render; Search Console + submit sitemap. Từ khóa nhắm: "tra cứu kết quả kiểm nghiệm phân bón Cần Thơ", "phòng kiểm nghiệm CFC". Không dùng meta keywords thay nội dung.

### 5.3 Trang kết quả (noindex, no-store)
- Header phản hồi: `X-Robots-Tag: noindex, nofollow, noarchive`, `Cache-Control: no-store`; thẻ `meta robots` tương ứng.
- Hiển thị: mã mẫu, ngày phát hành, trạng thái ("Đã phát hành"/"Đã thu hồi"/"Đang xử lý"), **thẻ tải file** theo loại (PDF "Bản chính thức", Word/Excel "Tài liệu kèm theo") kèm dung lượng + checksum rút gọn, **dòng thời gian tiến độ** (tiếp nhận → chuyển KCS → thử nghiệm → phát hành, chỉ mốc ngày, không tên nhân viên).
- Link tải là **signed URL sống ngắn (vd. 5 phút)**, một đường dẫn mỗi lần bấm; không cache; không lộ đường dẫn lưu trữ.
- Hết hạn/sai: thông báo trung tính, không phân biệt "mã không tồn tại" với "sai 4 số cuối".

### 5.4 API public (theo D2 — hai cách)
- `POST /api/public/v1/lookup` body `{ lookupCode, phoneTail4 }` → trả projection tối thiểu (`found`, `sampleCode`, `status`, `issuedAt`, danh sách file `public_visible` kèm `downloadToken`).
- `GET /api/public/v1/r/{qrToken}` → mở hồ sơ từ QR. **Mặc định QR cũng yêu cầu 4 số cuối SĐT** (token 128 bit đủ chống dò nhưng ai nhặt được phiếu giấy sẽ xem được; **cần xác nhận**: có cho QR mở thẳng không?).
- `GET /api/public/v1/files/{downloadToken}` → stream file, kiểm tra còn hiệu lực/chưa thu hồi.
- **Không trả:** `receivedBy/deliveredBy`, email, SĐT, địa chỉ, ID DB tuần tự, ảnh/file Phiếu 3, mapping mã mù, dữ liệu KCS, raw data, bản nháp.

### 5.5 Chống lạm dụng và bảo mật
Rate limit theo IP + theo mã (Redis); khóa tạm sau N lần sai; Cloudflare Turnstile (CAPTCHA) trước khi tra cứu; audit mọi lượt tra cứu; CORS chỉ cho hostname public; CSP chặt; không cookie phiên; tách security filter chain. Cloudflare Access/WAF cho `/admin` (không public). `noindex` ≠ bảo mật: dữ liệu vẫn được chặn bằng mã + SĐT + rate limit.

### 5.6 QR trên phiếu kết quả
Tạo QR từ `qrToken` (URL `https://<host>/r/{token}`), chèn vào mẫu phiếu/PDF; kiểm tra quét được ở kích thước in; chỉ phát hành QR sau khi test bảo mật xong (đúng thứ tự trong `docs/PUBLIC_LOOKUP_SEO.md`).

### 5.7 Test và nghiệm thu
Dò mã (brute force) bị chặn; QR/mã đã thu hồi; mã hồ sơ chưa phát hành không lộ; header noindex/no-store; sitemap chỉ có `/tracuu`; JSON-LD hợp lệ (Rich Results Test); Lighthouse SEO ≥ 95, Performance ≥ 90 (mobile); 390/1024/1440/2560 không tràn ngang; lỗi/empty/loading; không có dữ liệu cá nhân trong response (test snapshot DTO).

## 6. N* — Zalo OA + SMS ở khâu đầu tiên (tiếp nhận) — LÀM CUỐI

Thiết kế chi tiết đã có trong `docs/INTAKE_NOTIFICATIONS.md` (outbox + worker + adapter + webhook, consent, idempotency, fallback chỉ khi lỗi xác định). Plan này chỉ chốt **thứ tự** và bổ sung điểm mới do có trang tra cứu.

### 6.1 N0 — Hành chính, bắt đầu NGAY (không cần code, chờ duyệt lâu)
- [ ] Xác thực Zalo OA; tạo ZBS Account + AppID; liên kết OA; soạn và gửi duyệt template `INTAKE_RECORDED` (và `RESULT_RELEASED` nếu chọn).
- [ ] Chọn nhà cung cấp SMS Brandname/MultiSMS, đăng ký brandname, template, lấy tài liệu API/callback/sandbox. Chốt ngân sách, hạn mức, người sở hữu tài khoản.
- [ ] Chốt nội dung consent (theo Nghị định 13/2023 về bảo vệ dữ liệu cá nhân — **rà soát pháp lý**), kênh mặc định, thời hạn lưu SĐT/log.

### 6.2 N1–N5 (làm sau R2)
N1 nền tảng outbox/worker/fake provider → N2 UI consent + timeline + hàng đợi lỗi Admin → N3 ZBS → N4 SMS + fallback → N5 WebSocket nội bộ, feature flag, runbook.

### 6.3 Điểm bổ sung khi đã có trang tra cứu
- Tin `INTAKE_RECORDED` có thể kèm **mã tra cứu + link `/tracuu`** (trước đây ghi "chỉ thêm link sau khi lookup hoàn thành") — làm ở N1 vì lúc đó R2 đã xong.
- Tin mới `RESULT_RELEASED`: "Kết quả mẫu … đã có, tra cứu tại …" — **không chứa nội dung kết quả**. Trước đây docs ghi "chưa gồm gửi kết quả qua tin nhắn"; đây là thông báo có hồ sơ, **cần người dùng xác nhận** có làm hay không.
- Không gửi link tải file trực tiếp qua tin nhắn.

## 7. Câu hỏi còn mở cần người dùng/LAB quyết định
1. "Biên bản nghiệm thu" có đúng là Phiếu kết quả KQKN-DV không? Quy tắc **số phiếu kết quả** (`SRMMYYYY-SEQ`?) và **người ký/chức danh**?
2. Tên miền public (vd. `tracuu.cfccobay.com`)? Đã có Cloudflare chưa? Máy chủ chạy ở LAN hay VPS?
3. QR có mở thẳng không cần 4 số cuối SĐT không?
4. Giới hạn file (10 MB?/10 file?) và có chạy ClamAV được không? Nơi lưu file production (MinIO/S3/ổ đĩa + backup)?
5. Mặc định cờ hiển thị cho khách: chỉ PDF hay cả ba? Có cần đóng dấu mờ/watermark trên PDF không?
6. Receptionist có được xem trạng thái hồ sơ kết quả không?
7. Có gửi tin `RESULT_RELEASED` (chỉ báo "đã có kết quả") không?
8. OA Zalo đã xác thực chưa; đã chọn nhà cung cấp SMS chưa; ngân sách tháng?
9. Chứng nhận/năng lực nào được phép hiển thị trên trang public (ISO 17025/VILAS)?
10. Thời hạn lưu hồ sơ kết quả và log tra cứu?

## 8. Rủi ro chính
| Rủi ro | Giảm thiểu |
|---|---|
| Gate 5A (sequence/`-V/-R`) chưa xong làm chặn phát hành | Đặt G5A trước R1; hoặc R1 dùng mã gốc hiện có và đánh dấu "chưa xác minh" (**cần quyết định**) |
| Khách tải Word/Excel bị sửa/mạo danh | Cờ `public_visible`, nhãn bản chính thức, checksum, mặc định chỉ PDF |
| Lộ dữ liệu khách ở site public | Projection riêng, test snapshot DTO, tách deploy, Access cho Admin |
| Dò mã tra cứu | Mã ngẫu nhiên dài + 4 số SĐT + rate limit + Turnstile + audit |
| Chờ duyệt template Zalo/SMS kéo dài | Bắt đầu N0 ngay |
| Refactor CSS làm vỡ giao diện đang chạy | Chụp ảnh baseline 4 viewport × 3 role trước, so sánh sau từng bước |
| Dự án phình quá lớn | Giao Codex theo từng giai đoạn có tiêu chí nghiệm thu riêng (mục 3) |

## 9. Đề xuất chia việc cho Codex
1. **F0** FE nền tảng (token, tách CSS, font, lazy, a11y) — không đổi tính năng.
2. **F1** Dashboard KCS/Receptionist/Admin + API hàng đợi (xem `plans/intake/*`).
3. **G5A** Gate mã hóa.
4. **R1** Hồ sơ kết quả (backend + FE nội bộ).
5. **R2** Site public + API public + QR + Cloudflare.
6. **F2** Điểm nhấn wow.
7. **N1–N5** Zalo/SMS.

Mỗi giai đoạn: báo cáo theo mẫu 9 mục (`CLAUDE.md`), test thật, Chrome 4 viewport, cập nhật Project Memory sau khi pass.
