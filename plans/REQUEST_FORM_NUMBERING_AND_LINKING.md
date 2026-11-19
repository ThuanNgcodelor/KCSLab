# Plan: Phiếu yêu cầu kiểm nghiệm — số CFC26.xxxx, liên kết Phiếu 07/02, giao diện CFC

> Lập 19/11/2026 (Claude, chưa code). Giao cho Codex. Phạm vi: `/admin/requests/new` và `/admin/requests/:id` (feature `sampling`). Trạng thái: **đề xuất, chưa triển khai, chưa kiểm thử**.

## 1. Mục tiêu

1. Số/No. của Phiếu yêu cầu theo quy tắc `CFC{YY}.{NNNN}` (ví dụ `CFC26.0001`), YY là 2 số cuối của năm, sang năm mới đổi tiền tố và đếm lại từ `0001`.
2. Phiếu 07 (Kế hoạch lấy mẫu) và Phiếu 02 (Lấy mẫu hiện trường) **dùng chung số của Phiếu 1**, không nhập số riêng.
3. Dữ liệu đã nhập ở Phiếu 1 → tự điền sang 07; dữ liệu 07 → tự điền sang 02; vẫn sửa được. Không bắt nhập lại.
4. Mục III của Phiếu 1 là checklist theo mẫu Word (Cadimi: Ruột; Vàng O/Auramin O: Vỏ, Ruột), thay cho ô gõ tự do.
5. Hai khối 07/02 đồng bộ giao diện CFC với phần Phiếu 1 phía trên (hiện ô xám đặc, checkbox phình to).

## 2. Hiện trạng (có bằng chứng từ source)

- `TestRequestFormPage.jsx`: `Số/No.` là ô nhập tay, `required`, `disabled` khi sửa. `requestNumber` gửi lên `POST /api/v1/test-requests`; `TestRequestService.create` chỉ `normalize` (trim + upper-case) rồi kiểm trùng bằng `existsByRequestNumberIgnoreCase` → 409 "Số Phiếu yêu cầu đã tồn tại". **Chưa có bộ đếm, chưa kiểm định dạng.**
- `OptionalForm` (cùng file) có ô `Số/No.` riêng, `required`; `SavePlan`/`SaveField` có `@NotBlank formNumber`; DB có `uk_sampling_plans_number`, `uk_field_sampling_number` (V11). Dữ liệu 07/02 là hai state riêng (`plan`, `field`) khởi tạo `emptyPlan()/emptyField()`, **không đọc từ `form`**; 02 cũng không đọc từ 07 → đây là lý do phải nhập lại.
- Nguyên nhân giao diện: CSS `.sampling-form-section .checkbox-line`, input/label style (`source/fe/src/styles.css` quanh dòng 1313–1342) chỉ áp dụng trong `.sampling-form-section`; `OptionalForm` render `section.panel-card.optional-form` không có lớp đó nên ô nhập mất style, checkbox lấy kích thước mặc định.
- Mục III hiện: mỗi dòng `{ sampleNameOrId, parameters, matrix, testMethod }` toàn text tự do (`test_request_items`).
- Mẫu Word cũng yêu cầu mục IV (mục đích dạng checkbox), mục V "Thông báo khi chậm", mục VI xác nhận, nhãn song ngữ. Người dùng **chưa chọn** làm các mục này đợt này → ngoài phạm vi (xem mục 8).

## 3. Quyết định đã chốt (người dùng, 19/11/2026)

- Giữ nguyên `CFC` và `YY`; YY lấy theo năm hiện hành (2026 → `26`, 2027 → `27`).
- Phần số thứ tự **có thể hệ thống tự điền gợi ý hoặc người dùng tự nhập**; **trùng thì báo lỗi**.
- Phiếu 07 và 02 dùng chung số Phiếu 1.
- 07/02 tự điền từ phiếu trước và vẫn sửa được.
- Mục III dạng checklist đúng bảng trong mẫu Word: Cadimi (Cd) → Ruột; Vàng O (Auramin O) → Vỏ, Ruột. **Không có dòng "Khác"** (mẫu Word không có).
- Tiền tố `CFC{YY}` mặc định theo năm hiện hành nhưng **người dùng sửa được** (ví dụ lập phiếu cho năm khác). Quá `9999` thì thêm một chữ số (`CFC26.10000`).

## 4. Thiết kế

### 4.1 Số phiếu (backend)

- Định dạng: regex `^CFC\d{2}\.\d{4,}$`. Tối thiểu 4 chữ số có đệm 0 (`CFC26.0001`); khi vượt `9999` tự thêm chữ số (`CFC26.10000`), không chặn.
- Tiền tố `CFC{YY}`: gợi ý mặc định là năm hiện hành (`Asia/Ho_Chi_Minh`, dùng `Clock` để test), **người dùng được sửa YY**; backend chấp nhận mọi `YY` hợp lệ 2 chữ số. Bộ đếm tính riêng theo YY của số được dùng.
- Bộ đếm nguyên tử theo năm: bảng mới `test_request_counters(year_code CHAR(2) PK, last_sequence INT NOT NULL)`, cấp số bằng `INSERT … ON DUPLICATE KEY UPDATE last_sequence = LAST_INSERT_ID(last_sequence + 1)` theo mẫu `SampleCodeSequence` / `ResultNumberSequence`. Không dùng `COUNT+1`. Cột `request_number` hiện `VARCHAR(80)` nên đủ chỗ.
- API mới `GET /api/v1/test-requests/next-number` (ADMIN, RECEPTIONIST) trả `{ "requestNumber": "CFC26.0001" }` **chỉ gợi ý, không tiêu thụ số**. Đặt mapping trước `/{id}` để không bị nhầm.
- `create`: nếu `requestNumber` trống → cấp số mới từ bộ đếm (nguyên tử); nếu có → chuẩn hóa upper-case, kiểm định dạng (400), kiểm trùng (409 với thông báo "Số CFC26.0003 đã tồn tại, số kế tiếp là …"), rồi `last_sequence = GREATEST(last_sequence, <số nhập tay>)` để lần cấp tự động sau không trùng. Bắt cả `DataIntegrityViolationException` từ unique key để trả 409 khi hai người lưu cùng lúc.
- Sửa số sau khi tạo vẫn **không cho phép** (giữ hành vi hiện tại).
- Số cũ không đúng định dạng (nhập tay trước đây) giữ nguyên, không đổi, không ghi đè; validation định dạng chỉ áp dụng khi tạo mới.
- `SaveRequest.requestNumber`: bỏ `@NotBlank` (cho phép trống = tự cấp), giữ `@Size(max=80)`. Tương thích ngược với client cũ vẫn gửi số.

### 4.2 Số của Phiếu 07/02

- `savePlan`/`saveField` đặt `formNumber = testRequest.requestNumber`; **bỏ qua** giá trị client gửi; `SavePlan.formNumber`/`SaveField.formNumber` bỏ `@NotBlank`. Giữ cột và unique key V11 (mỗi Phiếu 1 chỉ có một 07 và một 02 nên không xung đột). Không cần migration đổi cột.
- Dữ liệu 07/02 đã lưu với số riêng trước đây: **giữ nguyên, không ghi đè**; response vẫn trả `formNumber` thực lưu. Kiểm tra database làm việc xem có bản ghi nào trước khi quyết định có cần báo cáo ngoại lệ.
- UI: ô `Số/No.` của 07/02 chuyển thành chỉ đọc, hiển thị số Phiếu 1 (kèm nhãn "Theo Phiếu yêu cầu").

### 4.3 Tự điền liên kết (frontend)

- Ngay trên khối 07/02 hiển thị **tóm tắt chỉ đọc** từ Phiếu 1: khách hàng, tên mẫu, mã lô, chỉ tiêu/nền mẫu, hạn trả — để nhân viên không cần nhập lại hoặc mở tab khác.
- Khi mở 07 mà **chưa có bản ghi 07**: điền sẵn `formDate = hôm nay`, `representative = contactPerson`, `growingArea = origin` (các trường Phiếu 07 có đối ứng trong Phiếu 1; trường nào không có nguồn thì để trống, không suy đoán).
- Khi mở 02 mà **chưa có bản ghi 02**: điền sẵn từ 07 đã lưu (nếu có) cho các trường dùng chung: người lấy mẫu, điện thoại, thời gian bắt đầu/kết thúc, địa điểm, vùng trồng, cơ sở đóng gói, xe, moóc, container; nếu chưa có 07 thì dùng cùng nguồn như điền sẵn 07. Không điền `sampleSealNumber`, `sampleQuantity`, `sampleCondition` (phần thực tế chỉ nhân viên hiện trường biết).
- Sau khi lưu 07 thành công, nếu 02 chưa lưu và người dùng chưa sửa ô nào của 02 thì cập nhật bản nháp 02 theo 07 vừa lưu. Mỗi khối có nút **"Lấy lại từ phiếu trước"** (ghi đè sau khi xác nhận). Không bao giờ ghi đè bản ghi đã lưu một cách im lặng.
- Giữ nguyên thao tác lưu riêng từng khối và các endpoint `PUT /sampling-plan`, `PUT /field-sampling`.

### 4.4 Mục III — checklist

- Bố cục giống bảng III của mẫu Word (ảnh người dùng gửi 19/11): mỗi **mẫu** là một khối có số TT và **một ô "Tên mẫu / Mã nhận diện"** dùng chung cho cả khối; trong khối có đúng hai hàng chỉ tiêu cố định, mỗi hàng có cột phương pháp thử riêng:
  - Hàng 1: Cadimi (Cd) — ☐ Ruột — Phương pháp thử
  - Hàng 2: Vàng O (Auramin O) — ☐ Vỏ ☐ Ruột — Phương pháp thử
  - Có nút thêm/xóa mẫu (khối). **Không có dòng "Khác"** vì mẫu Word không có.
- Lưu **không đổi schema**: mỗi tổ hợp (mẫu, chỉ tiêu, nền mẫu) được tick → một dòng `test_request_items` với `sampleNameOrId` = mã của mẫu, `parameters` = `Cadimi (Cd)` hoặc `Vàng O (Auramin O)`, `matrix` = `Vỏ`/`Ruột`, `testMethod` = phương pháp của hàng chỉ tiêu đó (lặp lại cho các nền mẫu của cùng chỉ tiêu). Chỉ tiêu có tick nền mẫu nào thì mới sinh dòng.
- **Mã nhận diện chính thức do nhân viên đặt ở Phiếu 3 (người dùng xác nhận 19/11)** nên Phiếu 1 không sinh mã, không đặt quy tắc mã, không kiểm trùng mã. Ô "Tên mẫu / Mã nhận diện" ở Phiếu 1 chỉ là nhãn tự do để phân biệt các mẫu khách yêu cầu. Backend hiện `trim()` bắt buộc `sampleNameOrId`: cho phép để trống ở UI và khi trống lưu mặc định `Mẫu {TT}` (TT theo thứ tự khối) để vẫn gom được các dòng cùng khối khi mở lại. Codex kiểm tra DTO `Item` (`@NotBlank`?) và ràng buộc cột trước khi sửa.
- Khi sửa phiếu cũ: gom các dòng cùng `sampleNameOrId` thành một khối; dòng có `parameters` hoặc `matrix` không khớp danh mục **không được xóa hay biến mất âm thầm** — hiển thị cảnh báo kèm đúng văn bản đã lưu và giữ nguyên khi lưu lại (cách hiển thị để Codex đề xuất, không thêm dòng "Khác" vào form mới).
- Phiếu 3/4 đọc `parameters`/`matrix` dạng chuỗi hiện có nên không đổi contract; Codex kiểm tra nơi tự điền sang Phiếu 3 (`ReceiptCreatePage`, `requestId`) vẫn hoạt động với dòng tách theo chỉ tiêu.

### 4.5 Giao diện CFC

- Dùng lại cùng lớp/section như phần Phiếu 1 (`sampling-form-section`) cho `OptionalForm`, hoặc đưa style input/label/checkbox ra lớp chung `.sampling-page` — **chọn cách ít ảnh hưởng nhất, không phá màn khác** dùng `panel-card`.
- Mục tiêu: ô nhập nền trắng, viền xanh nhạt, chữ rõ như form 01–04; label dùng cùng kiểu; checkbox 18px `accent-color` CFC; ô chỉ đọc (Số/No., tóm tắt) có kiểu riêng dễ phân biệt với ô disabled.
- Không dùng thư viện UI mới, không TypeScript.

## 5. Các bước và file dự kiến (Codex)

1. Backend: migration `V12__test_request_counters.sql` (**kiểm tra số version thực tế trong `db/migration` trước khi đặt**; không sửa V1–V11), `TestRequestNumberSequence` (cạnh `TestRequestService`), sửa `TestRequestService`/`TestRequestController`/`TestRequestDtos`.
2. Backend: `savePlan`/`saveField` dùng số Phiếu 1.
3. Frontend: `testRequestApi.js` (thêm `getNextRequestNumber`), `TestRequestFormPage.jsx` (số tự điền + báo trùng, prefill, nút lấy lại, checklist mục III), `styles.css`.
4. Test (mục 6), cập nhật Project Memory `docs/ai/` (CONTEXT/STATE), `docs/API_CONVENTIONS.md` (endpoint mới, `requestNumber` tùy chọn), `docs/DATABASE.md` (bảng đếm).

## 6. Tiêu chí nghiệm thu và test

- `POST` không gửi số → nhận `CFC26.0001`, lần sau `CFC26.0002`; 20 lần tạo song song → 20 số khác nhau, không lỗi; số nhập tay `CFC26.0010` → lần cấp tự động sau là `CFC26.0011`.
- Số sai định dạng → 400; số trùng (khác hoa/thường) → 409 với thông báo rõ; không đổi số khi `PUT`.
- Đổi năm (mock `Clock` sang 2027) → gợi ý `CFC27.0001`; năm 2026 không bị ảnh hưởng; người dùng sửa YY thành `27` hoặc `25` vẫn lưu được và đếm riêng theo YY.
- Sau `CFC26.9999` số kế tiếp là `CFC26.10000`; regex chấp nhận 4 chữ số trở lên.
- Số cũ không đúng định dạng vẫn mở/sửa được.
- 07/02 lưu → `formNumber` bằng số Phiếu 1 dù client gửi số khác; bản ghi cũ không bị ghi đè.
- Quyền: chỉ ADMIN/RECEPTIONIST gọi được `next-number`; TECHNICIAN 403.
- FE: tạo Phiếu 1 → số được điền sẵn; mở 07 thấy dữ liệu từ Phiếu 1 đã điền; lưu 07 rồi mở 02 thấy dữ liệu từ 07; "Lấy lại" hỏi xác nhận; bố cục mục III giống bảng Word (một ô mã mẫu mỗi khối, Cd chỉ có Ruột, Auramin O có Vỏ + Ruột, phương pháp thử mỗi hàng); không có dòng "Khác"; sửa phiếu cũ không mất chỉ tiêu.
- Chạy: `mvn test` (Java 21) trong `source/be` (test đồng thời bật bằng `KCS_ISOLATED_DB=true` theo thông lệ dự án), `npm run build` trong `source/fe`.
- Chrome thật ở 390×844, 1024×768, 1440×900, 2560×952: không tràn ngang; khối 07/02 cùng giao diện với Phiếu 1; có trạng thái loading/lỗi/trống; chụp ảnh đối chiếu. Phiên Chrome phải đăng nhập RECEPTIONIST hoặc ADMIN (phiên KCS sẽ bị redirect khỏi `/admin/requests`).
- Không đánh dấu hoàn thành khi chưa có kết quả test thật; ghi rõ test chưa chạy.

## 7. Rủi ro

- Hai người cùng xem gợi ý `CFC26.0005` → người lưu sau nhận 409; UI phải tải lại gợi ý và giữ nguyên dữ liệu đã nhập.
- Số nhập tay quá lớn làm bộ đếm nhảy: chấp nhận, nhưng đặt trần hợp lý (cần xác nhận) để gõ nhầm `CFC26.9999` không làm hết dải.
- Tách dòng chỉ tiêu theo (mẫu × chỉ tiêu × nền mẫu) làm số dòng tăng; kiểm tra các màn đọc `items` (Phiếu 3 prefill, danh sách) không hiển thị lặp khó hiểu.
- Bỏ ô số riêng của 07/02: API cũ vẫn gửi `formNumber` phải không lỗi (server bỏ qua).

## 8. Ngoài phạm vi đợt này / câu hỏi mở (cần xác nhận)

- Chưa làm theo mẫu Word: mục IV mục đích dạng checkbox, mục V "Thông báo khi chậm" (UI), mục VI xác nhận khách hàng/người tiếp nhận, nhãn song ngữ Việt/Anh, export DOCX Phiếu 1. Người dùng không chọn đợt này.
- Đã chốt 19/11: vượt `9999` thì thêm chữ số; YY mặc định theo năm hiện hành và sửa được; không có dòng "Khác". Còn mở: trần cho số nhập tay để gõ nhầm không làm nhảy bộ đếm.
- Đã chốt 19/11: mã mẫu do nhân viên đặt ở Phiếu 3, không cần quy tắc mã ở Phiếu 1 (ghi chú cuối bảng Word về mã riêng cho Vỏ/Ruột được áp dụng ở bước Phiếu 3, ngoài phạm vi plan này).
- Nhánh khách tự lấy mẫu (không 07/02) giữ nguyên: 07/02 vẫn tùy chọn.
