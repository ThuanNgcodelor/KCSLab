# Thông báo nội bộ realtime

## Phạm vi

- Hiển thị toast CFC cho notification mới nhận qua WebSocket; không thêm âm thanh hoặc thông báo ra ngoài trình duyệt.
- Chuông và trang thông báo vẫn lấy dữ liệu từ REST; toast có thể đóng hoặc mở chi tiết, tối đa ba thông báo, tự đóng sau tám giây.
- Khi WebSocket kết nối lại, đọc lại REST để đồng bộ số chưa đọc và danh sách. Không tạo toast hàng loạt cho thông báo cũ vừa đồng bộ.
- Giữ nguyên người nhận và các quyền backend; không thay đổi gửi Zalo/SMS.

## Kiểm tra

- Mất kết nối và kết nối lại đồng bộ số chưa đọc; event trùng không tạo hai toast hoặc tăng bộ đếm hai lần.
- Toast mở chi tiết/đánh dấu đã đọc, đóng và tự hết hạn; 401/403 hoặc lỗi mạng không giả vờ đánh dấu đã đọc.
- Kiểm tra bốn breakpoint chuẩn, accessibility và build frontend. Thử nghiệm thực tế hai phiên sau khi có dữ liệu thử phù hợp.

## Kết quả 16/11/2026

- Đã triển khai toast, chống event lặp trong cache gần đây và refresh REST khi socket mở/kết nối lại; giữ quyền người nhận hiện có.
- `npm run build` pass. Playwright với WebSocket/REST giả lập kiểm tra sự kiện mới/trùng, đóng, tự đóng, mở chi tiết, đồng bộ sau kết nối lại và bốn breakpoint không tràn ngang toàn trang.
- Chưa kiểm thử WebSocket thực với hai phiên và dữ liệu thử độc lập; chưa kiểm thử riêng lỗi 401/403 hoặc mạng khi đánh dấu đã đọc. Không xem giả lập là E2E production.

## Mở rộng — Login và rà soát frontend

### Hiện trạng đã rà source

- Login dùng `/cfc-mark.png` kèm bộ lọc CSS làm logo khó nhìn; favicon là biểu tượng chữ CFC riêng, không khớp logo sidebar. Ô mật khẩu chưa có nút hiện/ẩn.
- Không còn `window.confirm`, `window.alert` hoặc `window.prompt` trong `source/fe/src` tại lần rà soát này. Giữ `ConfirmDialog` dùng chung cho xác nhận; không loại bỏ bước xác nhận nghiệp vụ.
- Thông báo thao tác hiện có nhiều kiểu: `form-error`, `success-note`, `toast`, và toast realtime; một số trang transfer/testing chưa gắn `role=alert/status` cho thông báo lỗi/thành công. Chưa khẳng định mọi toast hoạt động nhất quán.
- Form mật khẩu ở profile và quản lý nhân viên chưa có hiện/ẩn. Profile ghi tối thiểu 10 ký tự, form nhân viên ghi 12; cần đối chiếu validation backend trước khi thống nhất, không tự thay đổi chính sách.

### Kế hoạch thực hiện

1. Login: dùng logo CFC hiện có cùng sidebar cho trang và favicon; nút hiện/ẩn mật khẩu có tên truy cập, `type=button`, không đổi giá trị và không gửi form. Giữ remember-me, xác thực và điều hướng hiện tại.
2. Toast: phân biệt toast realtime có lịch sử với phản hồi lưu/lỗi tại form; thống nhất màu, thời gian, đóng, thứ tự lớp, thông báo cho screen reader; không tự đánh dấu notification đã đọc khi chỉ đóng toast.
3. Xác nhận: tiếp tục dùng `ConfirmDialog` cho nghiệp vụ cần xác nhận; rà `window.confirm/alert/prompt` và các thao tác quan trọng chưa có xác nhận, không bỏ chốt an toàn.
4. Forms: rà label, lỗi cạnh trường, focus, trạng thái loading, bàn phím, `autocomplete`, dữ liệu chưa lưu và validation frontend/backend. Không đổi luật nghiệp vụ nếu chưa xác minh.
5. Giao diện: rà login, tiếp nhận, chuyển mẫu, nhận KCS, thử nghiệm, profile, nhân viên và thông báo với trạng thái loading/error/empty; gom sửa theo feature và giữ API/RBAC.
6. Kiểm thử: build; kiểm tra login đúng/sai, toggle password, favicon sau build, xác nhận/hủy, toast/reconnect; Chrome mobile/desktop ở 390×844, 1024×768, 1440×900, 2560×952. Dùng dữ liệu tách biệt cho thao tác ghi.

### Trạng thái

- Login dùng logo CFC và favicon theo logo sidebar, có nút hiện/ẩn mật khẩu. `npm run build` pass; Playwright giả lập kiểm tra logo tải được, toggle giữ giá trị/không submit, lỗi đăng nhập và không tràn ngang toàn trang tại bốn breakpoint. Chưa kiểm tra login thành công hay favicon bằng bản build/Chrome thật.
- Các mục toast/forms/giao diện toàn hệ thống còn cần rà và triển khai theo từng feature; kiểm tra hai phiên realtime thật vẫn chưa hoàn tất.

## Kế hoạch triển khai chi tiết và nghiệm thu

### P0 — Khóa phạm vi và dữ liệu thử

- [ ] Dùng database/Redis và tài khoản kiểm thử tách biệt cho các bước tạo, gửi, nhận/từ chối mẫu; không thao tác trên hồ sơ vận hành chỉ để test.
- [ ] Ghi rõ hiện trạng Git trước khi làm, giữ thay đổi sẵn có; không sửa `.env`, secrets, migration cũ hoặc tự commit.
- [ ] Giữ `ConfirmDialog` của CFC. `window.confirm/alert/prompt` không dùng trong code ứng dụng; cảnh báo rời trang do trình duyệt quản lý nếu có `beforeunload` cần được báo riêng, không hứa tùy biến được.

### P1 — Login và nhận diện CFC

- [x] Source login đã dùng logo CFC, nút hiện/ẩn mật khẩu, favicon dùng logo cùng sidebar sau khi JS khởi tạo.
- [ ] Kiểm tra favicon trong tab Chrome với bản build, kể cả lần tải đầu, tải lại và cache; nếu cần đưa favicon vào HTML tĩnh để không phụ thuộc JS.
- [ ] Kiểm tra đăng nhập thành công/thất bại, remember-me, refresh/đăng xuất, redirect theo từng role, thông báo lỗi, `Tab`/`Enter` và trình quản lý mật khẩu. Không in hoặc lưu mật khẩu trong log/screenshot.
- [ ] Xác nhận logo, icon hiện/ẩn, vùng chạm, tương phản và không tràn ngang ở bốn breakpoint chuẩn.

### P2 — Thông báo và realtime

- [x] Đã có toast CFC và đồng bộ REST khi WebSocket kết nối lại; thử nghiệm trước dùng mock, không phải hai phiên thật.
- [ ] Thử hai phiên RECEPTIONIST/TECHNICIAN trên dữ liệu thử: gửi Phiếu 4 → KCS nhận toast; KCS nhận/từ chối → người bàn giao nhận toast; kiểm tra chuông, trang lịch sử và link đúng role.
- [ ] Test mất mạng → phát sinh notification → kết nối lại; REST trả đúng danh sách/số chưa đọc và không bắn toast hàng loạt cho tin cũ. Kiểm tra event lặp, tab đồng thời và event đến trong khi REST đang tải.
- [ ] Test mở/đóng/tự hết hạn toast; khi PATCH đánh dấu đọc lỗi 401/403/mạng thì không báo thành công hoặc trừ sai số. Kiểm tra focus, screen reader, màn hình nhỏ và toast không che thao tác quan trọng.
- [ ] Phân biệt toast realtime lưu lịch sử với thông báo thành công/lỗi của form; đề xuất và áp dụng style nhất quán theo từng feature, không xóa lịch sử hay tự đánh dấu đọc khi đóng toast.

### P3 — Hộp xác nhận và form

- [ ] Rà lại `window.confirm/alert/prompt` trong toàn frontend; thay các chỗ mới phát sinh bằng `ConfirmDialog` phù hợp. Không xóa component hoặc bỏ xác nhận các thao tác quan trọng.
- [ ] Với `ConfirmDialog`, test xác nhận/hủy/Escape, focus quay về nút gốc, double click, trạng thái chờ, lỗi API và 409; không tạo request khi hủy.
- [ ] Rà login/profile/nhân viên: nhãn trường, `autocomplete`, hiển thị mật khẩu và validation; đối chiếu minLength 10/12 với backend trước khi đề xuất thay đổi.
- [ ] Rà form Phiếu 3/4/5 và nhận mẫu: trường bắt buộc, lý do, ngày, mẫu/file, thông báo lỗi gần trường, loading, không gửi hai lần, keyboard, thay đổi chưa lưu; kiểm tra cả validation backend.
- [ ] Với mọi thay đổi nghiệp vụ hoặc yêu cầu xác nhận mới, ghi trade-off và xin LAB xác nhận nếu quy trình chưa chốt; không tự thêm điều kiện thiếu ảnh hoặc SLA.

### P4 — Đồng bộ giao diện và kiểm thử

- [ ] Lập danh sách lỗi theo màn hình: login, Tiếp nhận, Chuyển mẫu, Nhận mẫu KCS, Thử nghiệm, Thông báo, Profile và Nhân viên; sửa theo feature, không làm lại dashboard Admin ngoài yêu cầu.
- [ ] Mỗi màn hình có loading/error/empty, chữ và icon rõ, trạng thái không chỉ dựa vào màu, thao tác bằng bàn phím, không tràn ngang toàn trang.
- [ ] Chạy `npm run build` và `git diff --check`; chạy test backend nếu có sửa backend, không báo kết quả test chưa chạy.
- [ ] Chụp/kiểm tra Chrome tại 390×844, 1024×768, 1440×900, 2560×952; báo role, dữ liệu thật hay giả lập và từng trường hợp chưa kiểm tra.
- [ ] Cập nhật `docs/ai/CONTEXT.md` sau các thay đổi về hành vi hoặc trạng thái kiểm thử.

### Thứ tự ưu tiên

P0 → nghiệm thu phần login P1 → P2 thông báo/realtime → P3 xác nhận và validation → P4 đồng bộ các màn hình còn lại. Không đánh dấu toàn bộ hoàn thành chỉ vì build pass.

## Cập nhật tiếp theo — 16/11/2026 (code, chưa E2E)

- Favicon chuyển thành `source/fe/public/favicon.png` (192px) khai báo tĩnh trong `index.html`, bỏ đổi favicon bằng JS.
- Thêm `role=alert/status` cho thông báo lỗi/thành công ở các trang transfer, testing và receipt detail.
- Thêm "Hiện mật khẩu" ở Profile và form tạo nhân viên.
- Dùng `ConfirmDialog` cho: gửi Phiếu 4, nhận/từ chối ở trang transfer cũ, gửi biên bản, duyệt/trả lại biên bản; từ chối/trả lại bắt buộc ghi lý do trước khi hỏi xác nhận.
- `npm run build` pass. Chưa chạy Chrome, hai phiên realtime thật, hay test ConfirmDialog (Escape/focus/double-click/409).
- Lệch chính sách chưa đổi: mật khẩu tự đổi tối thiểu 10, Admin tạo/sửa user tối thiểu 12 (đúng với backend); cần LAB/Admin chốt.
