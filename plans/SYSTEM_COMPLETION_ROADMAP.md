# Kế hoạch hoàn thiện và nghiệm thu hệ thống CFC Lab

**Ngày lập:** 17/11/2026 · **Loại tài liệu:** plan điều phối, chưa phải bằng chứng hoàn thành.

## 0. Tiến độ thực hiện — 17/11/2026

| Phase | Trạng thái | Bằng chứng / còn thiếu |
|---|---|---|
| P0 | **Xong** (môi trường cô lập) | Baseline 38 test; Docker MySQL/Redis tạm; `scripts/run-be-tests-docker.sh`. Máy không có Java/Maven cài sẵn nên chạy qua Docker. |
| P1 | **Xong phần tự động; chưa nghiệm thu thủ công** | E2E HTTP + Chrome 4 viewport; 401/403/409, KCS projection, đồng thời nhận/từ chối (test). **Chưa:** mở DOCX bằng Word thật, hai phiên WebSocket thật, Phiếu 5 nhiều file riêng. |
| P2 | **Đã code + test; chờ LAB** | Sequence nguyên tử, xác minh hai người, lịch sử, gate (kể cả API cũ). **Chưa:** `-V/-R`, QC/mã mù (chờ LAB). Mẫu cũ = `VERIFIED/legacy` (giả định). |
| P3 | **Đã code + test** | File an toàn, duyệt/phát hành/thu hồi/phiên bản, lịch sử, gate P2, một bản PUBLISHED do DB bảo đảm. **Chưa:** quét virus, backup/retention, số phiếu theo quy tắc LAB, UI click thật, test đồng thời phát hành. |
| P4 | **Đã code + test; chưa deploy** | API public, 4 số cuối, rate limit, audit, no-store/noindex, UI tra cứu. **Chưa:** QR in, domain/TLS/SEO/headers hạ tầng, rate limit Redis khi scale. |
| P5, P6 | **Chưa làm** | Ngoài phạm vi lượt này. |

Chi tiết: `docs/SYSTEM_STATUS.md`. Các checkbox dưới đây giữ nguyên là danh sách gốc, không tự động phản ánh bảng này.

## 1. Mục tiêu và cách sử dụng

Đưa hệ thống từ trạng thái đã có các phần nội bộ đến quy trình được nghiệm thu: tiếp nhận → chuyển mẫu → nhận mẫu → thử nghiệm → phát hành kết quả → tra cứu an toàn. Plan cũng bao gồm phần chưa rà soát, dashboard Admin, frontend và thông báo khách.

Trạng thái thực tế đọc tại `docs/SYSTEM_STATUS.md`. Trước mỗi đợt triển khai, đối chiếu source, git status, migration và các plan chi tiết; không coi các mục `[ ]` dưới đây là đã làm. Mỗi đợt cần ghi kết quả test thật và cập nhật Project Memory.

## 2. Các plan đã có

| Nội dung | Tài liệu hiện có | Vai trò |
|---|---|---|
| Tổng thể FE, hồ sơ kết quả, public, ZBS/SMS | `plans/COMPLETION_PLAN.md` | Plan chi tiết; một số quyết định/câu hỏi cũ cần đối chiếu mục 3 bên dưới. |
| Receptionist, KCS, Admin, Phiếu 3 | `plans/intake/OVERVIEW.md`, `RECEPTIONIST.md`, `TECHNICIAN_KCS.md`, `ADMIN.md`, `TEST_CASES_FORM3.md` | Nghiệp vụ, API và case theo role. Một số mô tả trạng thái là lịch sử. |
| Thông báo nội bộ, login, form | `plans/NOTIFICATION_REALTIME.md` | Chi tiết realtime và rà soát FE. |
| Mã hóa mẫu | `docs/SAMPLE_CODING.md`, `plans/CURRENT.md`, tài liệu gốc CFC-TN-14 | Quy tắc nghiệp vụ; cần chốt thiết kế triển khai chi tiết trước khi code. |
| Lookup/bảo mật | `docs/PUBLIC_LOOKUP_SEO.md`, mục 5 `plans/COMPLETION_PLAN.md` | Kiến trúc dự kiến, không chứng minh chức năng đã hoạt động. |
| ZBS/SMS | `docs/INTAKE_NOTIFICATIONS.md`, mục 6 `plans/COMPLETION_PLAN.md` | Thiết kế tích hợp khách và điều kiện provider. |

Plan này liên kết và sắp thứ tự, không thay thế hợp đồng API, test case hay quy trình gốc.

## 3. Quyết định đã chốt và ràng buộc

- Giữ ba role `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`; Technician nhận mẫu và có thể lập Phiếu 5. Không tự tạo role mới.
- KQKN-DV là hồ sơ kết quả được nói đến. Chỉ tạo/lưu nháp ở giai đoạn hiện tại; **không phát hành cho tới khi hoàn tất gate mã hóa CFC-TN-14**.
- Cả mã tra cứu nhập tay và QR đều yêu cầu 4 số cuối điện thoại. Dự kiến tối đa 10 MB/file và 10 file/hồ sơ; mặc định chỉ PDF public, Admin có thể bật Word/Excel khi phát hành.
- Giữ phân tách API public và nội bộ; không đưa PII, file tiếp nhận, dữ liệu thô hoặc bản nháp ra public. `noindex` không thay cho kiểm soát truy cập.
- Duy trì cảnh báo thiếu ảnh theo hành vi hiện tại, không tự biến thành quy định LAB. Chính sách tên khách trong thông báo cần LAB xác nhận.
- Không sửa migration Flyway đã chạy, secrets hoặc dữ liệu làm việc để thử nghiệm. Không commit/merge/xóa thay đổi có sẵn trừ khi người dùng yêu cầu.

## 4. Thứ tự và phụ thuộc

```text
P0 Chuẩn bị dữ liệu thử và baseline
 → P1 Nghiệm thu luồng nội bộ + Word
 → P2 Mã hóa CFC-TN-14 và gate xác minh
 → P3 Hồ sơ kết quả: file, duyệt, phát hành/thu hồi
 → P4 Tra cứu public + QR + bảo mật + triển khai

P5 Dashboard Admin và hoàn thiện frontend: song song sau P0 theo phạm vi được giao
P6 ZBS/SMS: chuẩn bị tài khoản/template song song; tích hợp sau P3/P4
```

Đây là thứ tự đề xuất; việc ưu tiên một đợt và quyền thực hiện thay đổi hệ thống thật phải được giao riêng. Không cần chờ SEO hoặc domain để kiểm thử P1–P3 ở môi trường cô lập.

## 5. P0 — Baseline và môi trường thử

- [ ] Ghi nhận commit, thay đổi đang có, phiên bản Java/Node; kiểm tra V1–Vn trong source và lịch sử Flyway trên **database thử**.
- [ ] Chuẩn bị MySQL/Redis và tài khoản ba role tách biệt; dùng dữ liệu giả, file ảnh/PDF/DOCX hợp lệ và file sai định dạng. Chỉ dùng database làm việc khi người dùng cho phép rõ.
- [ ] Kiểm tra backend/frontend khởi động, smoke test không ghi dữ liệu thật; chụp baseline giao diện 390×844, 1024×768, 1440×900, 2560×952.
- [ ] Ghi điểm nghẽn, phiên bản, lệnh và kết quả; không tự khẳng định migration đã áp dụng ở database làm việc.

**Đạt khi:** môi trường test và dữ liệu thử xác định rõ, có thể chạy lại mà không làm thay đổi dữ liệu vận hành.

## 6. P1 — Nghiệm thu Tiếp nhận → KCS → Phiếu 5

- [ ] Theo `TEST_CASES_FORM3.md`: kiểm tra validation bắt buộc, nhiều mẫu, niêm phong, file, search/filter/pagination và phân quyền Phiếu 3.
- [ ] Kiểm tra draft/completion, catalog theo `OVERVIEW.md`; ghi phần thiếu và lập đợt triển khai riêng, không mặc định chức năng đã tồn tại.
- [ ] Xuất Phiếu 3 với dữ liệu tiếng Việt, ký tự đặc biệt, nhiều mẫu; kiểm tra cấu trúc và **mở bằng Microsoft Word thật**, đối chiếu biểu mẫu. Nếu không có Word, ghi chưa nghiệm thu.
- [ ] Với hai role thật trong môi trường thử: lập/gửi Phiếu 4 có ảnh → KCS xem đúng file → nhận; thực hiện riêng nhánh từ chối có/không có lý do và kiểm tra trạng thái mẫu, lịch sử.
- [ ] Kiểm tra API và UI: Technician không đọc receipt API đầy đủ; file chéo hồ sơ bị từ chối; phân trang/sort, timezone, cạnh tranh nhận/từ chối, 401/403/409 và reload thất bại.
- [ ] Kiểm tra Phiếu 5 từ mẫu đã nhận, dữ liệu thử nghiệm, gửi/trả lại/review; xác minh quy định người phân tích/người kiểm tra trước khi sửa quyền.
- [ ] Dùng hai phiên thật kiểm tra thông báo gửi, nhận, từ chối; notification sau commit, rollback và reconnect; kiểm tra không gửi trùng. Cần môi trường test tách biệt.
- [ ] Chrome thật tại bốn viewport, bàn phím/gallery, file thật; rà console, network, loading/error/empty và không tràn ngang toàn trang.

**Đạt khi:** case có kết quả và bằng chứng, lỗi nghiêm trọng được xử lý và chạy lại; ghi rõ mọi case chưa thể chạy. K1+K2 chỉ đóng sau nghiệm thu tương ứng.

## 7. P2 — Mã hóa CFC-TN-14

- [ ] Đối chiếu tài liệu gốc, thực tế LAB, mã gốc đang dùng và dữ liệu cũ; lập thiết kế các trạng thái, vai trò, ngoại lệ và kế hoạch chuyển dữ liệu.
- [ ] Cấp sequence nguyên tử theo ngày Việt Nam, không trùng khi chạy đồng thời; thêm migration mới tăng version, không sửa V1–V6.
- [ ] Thực hiện xác minh độc lập hai người, lịch sử hiệu chỉnh và các trạng thái `PENDING_CODING → PENDING_VERIFICATION → VERIFIED`; bảo vệ chuyển mẫu bằng gate đã thống nhất.
- [ ] Thiết kế và triển khai `-V/-R`, QC/mã mù theo CFC-TN-14 sau khi LAB xác nhận cách cấp, hiển thị và quyền truy cập.
- [ ] Test concurrency, chuyển trạng thái, cấm tự xác minh, dữ liệu cũ, quyền truy cập, timezone, lịch sử và chặn chuyển mẫu chưa verified; kiểm tra migration trên database thử.

**Đạt khi:** LAB chấp thuận quy tắc; code và test chứng minh gate hoạt động, kể cả đường API cũ. Không mở phát hành chỉ vì có giao diện xác minh.

## 8. P3 — Hồ sơ kết quả và Phiếu 5

- [ ] Hoàn thiện file đính kèm Phiếu 5 và quyền theo role; kiểm tra loại file, giới hạn và lưu trữ sau khi chốt quy tắc.
- [ ] Phát triển từ nháp kết quả hiện có: danh sách/chi tiết, upload nhiều file, gửi duyệt, trả lại, phát hành, thu hồi và phiên bản; có actor/thời gian và lịch sử bất biến.
- [ ] Tách quyền lập/tải/duyệt; xác minh người duyệt độc lập theo quy định LAB; chỉ Phiếu 5 hợp lệ và mẫu qua gate P2 mới phát hành. Xử lý xung đột đồng thời.
- [ ] Xác thực dung lượng/đuôi/MIME/nội dung, kiểm tra OOXML nguy hiểm, tên lưu ngẫu nhiên, checksum và tải qua API kiểm quyền; làm rõ quét virus, backup và retention.
- [ ] Test file giả mạo, quá giới hạn, upload lỗi một phần, quyền chéo, concurrent decision, thu hồi và version; migration và luồng chạy trên môi trường tách biệt.

**Đạt khi:** chỉ phát hành được hồ sơ hợp lệ sau gate; kiểm thử an toàn file và phân quyền đạt, lịch sử không bị ghi đè. UI/DB phải phản ánh trạng thái thật.

## 9. P4 — Tra cứu public, QR và hạ tầng

- [ ] Xây API public riêng chỉ trả hồ sơ đã phát hành, không dùng receipt API nội bộ; phản hồi lỗi trung tính, hạn chế PII.
- [ ] Cả QR và mã nhập tay xác minh 4 số cuối SĐT; token khó đoán, rate limit, audit phù hợp, không lưu/lộ số điện thoại đầy đủ; kiểm tra brute force và thu hồi.
- [ ] Chỉ cho tải file được phép; kiểm tra quyền và trạng thái tại lúc tải, `no-store`, `noindex` cho trang kết quả. Mặc định PDF; Word/Excel phải được Admin bật.
- [ ] Kết nối UI preview với API thật, có validation/loading/error; không bật form trước khi có API và bảo vệ. Test QR in thực tế và hết hạn/thu hồi.
- [ ] Sau khi có domain, cấu hình TLS, sitemap/canonical/SEO landing và route public qua hạ tầng riêng; không expose `/admin`, `/login`, `/api/v1` hay WebSocket nội bộ. Rà soát firewall và headers.
- [ ] Test bảo mật, không rò PII, kết quả chưa phát hành, file sau thu hồi, 4 viewport và trải nghiệm khách; chỉ public khi đạt tiêu chí và được duyệt triển khai.

**Đạt khi:** khách tra cứu và tải an toàn trên hồ sơ phát hành thử; hồ sơ nháp/thu hồi và API nội bộ không bị lộ, có bằng chứng test và phê duyệt triển khai.

## 10. P5 — Admin và rà soát frontend

- [ ] Hoàn thiện dashboard Admin tổng hợp theo `plans/intake/ADMIN.md`; thống nhất định nghĩa KPI và quyền trước khi hiển thị.
- [ ] Rà login, favicon/logo, form, validation cạnh trường, toast, modal dùng `ConfirmDialog`; không dùng `window.confirm/alert/prompt` cho luồng ứng dụng.
- [ ] Chuẩn hóa responsive, loading/error/empty, bàn phím/focus, độ tương phản và typography; chỉ refactor CSS/component theo từng phạm vi có baseline.
- [ ] Rà frontend theo role, breakpoint chuẩn, API thật và dữ liệu thử; chạy `npm run build` và ghi bằng chứng. Không coi kiểm thử mock là E2E thật.

**Đạt khi:** tiêu chí role, giao diện và khả năng sử dụng được kiểm tra và lỗi quan trọng đã xử lý; các giới hạn còn lại được ghi rõ.

## 11. P6 — ZBS/SMS

- [ ] Chuẩn bị Zalo OA, template, provider SMS, sandbox, consent và chính sách lưu log với LAB; ghi rõ người quản lý tài khoản, không đưa secrets vào repository.
- [ ] Sau khi xác định event và điều kiện gửi, triển khai outbox/idempotency, worker, callback, thử lại/fallback an toàn và audit; không dựa vào WebSocket nội bộ để gửi khách.
- [ ] Test bằng fake provider và sandbox: duplicate, timeout, callback, retry, consent và bảo vệ dữ liệu; chỉ bật provider thật khi người dùng phê duyệt.

**Đạt khi:** nội dung và provider được duyệt, test sandbox pass, quy trình vận hành và xử lý lỗi được nghiệm thu.

## 12. Câu hỏi cần LAB/người dùng xác nhận

1. Thiếu ảnh có được phép nhận mẫu không? Notification nội bộ có được chứa tên khách không?
2. Quy tắc mã `-V/-R`, QC/mã mù, người xác minh và xử lý hồ sơ cũ cụ thể theo CFC-TN-14?
3. Người phân tích, người kiểm tra, người ký/phát hành phải tách biệt thế nào? Quy tắc số phiếu kết quả?
4. Receptionist có xem được trạng thái hồ sơ kết quả? Loại file được đính kèm Phiếu 5?
5. Nơi lưu file production, backup, quét virus, retention; domain, hạ tầng và chứng nhận được công bố?
6. Nội dung consent, provider ZBS/SMS, có gửi thông báo “đã có kết quả” hay không?

Không chặn các bước kiểm tra độc lập khi còn câu hỏi mở; không tự chốt chính sách hay công bố khi thiếu quyết định.

## 13. Báo cáo sau mỗi đợt

Ghi: trạng thái từng mục, file/migration, contract và phần lệch plan, lệnh và kết quả nguyên văn, test mới, viewport/role/E2E, rủi ro và phần chưa kiểm chứng, Project Memory cập nhật. Không dùng dữ liệu thật hoặc secrets trong báo cáo. Khi chưa có bằng chứng, ghi **chưa kiểm chứng**, không đánh dấu hoàn thành.
