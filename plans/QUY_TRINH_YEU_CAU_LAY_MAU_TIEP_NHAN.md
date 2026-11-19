# Kế hoạch: từ yêu cầu kiểm nghiệm đến bàn giao mẫu

> Cập nhật 19/11/2026: P0–P4 đã có implementation trong source; chưa nghiệm thu production và chưa coi toàn bộ suite test là đạt. Các lựa chọn còn chờ LAB xác nhận được ghi ở cuối. Ưu tiên yêu cầu mới của sếp, nhưng không tự kết luận thay đổi quy trình chất lượng đã được phê duyệt.

## Trạng thái triển khai P0–P4

- **P0 — Đã chốt ở mức triển khai:** luồng 1 → (7/2 tùy chọn) → 3 → 4 → 5; một Phiếu 1 có nhiều Phiếu 3/mẫu; mã nhập tay tại Phiếu 3; KCS nhận/từ chối sau bàn giao; ADMIN phát hành file kết quả; không có duyệt thứ hai trước phát hành.
- **P1 — Đã triển khai:** migration `V11__test_request_sampling_flow.sql`, entity/repository/service/controller cho Phiếu 1, Phiếu 7 và Phiếu 2; API và UI tìm/chọn Phiếu 1, tự điền dữ liệu sang Phiếu 3.
- **P2 — Đã triển khai một phần:** đã có hồ sơ kế hoạch lấy mẫu và lấy mẫu hiện trường liên kết Phiếu 1, nhưng chưa có export DOCX theo mẫu và chưa chốt toàn bộ validation nghiệp vụ với LAB.
- **P3 — Đã triển khai:** Phiếu 3 nhận liên kết Phiếu 1/2, tách `sampleSealNumber` và `containerSealNumber`, nhập mã thủ công, kiểm tra trùng và ghi lịch sử chỉnh mã; mã thủ công được coi là sẵn sàng cho bàn giao theo quy trình mới.
- **P4 — Đã triển khai:** Phiếu 4 lấy đúng mẫu đã mã hóa, KCS nhận/từ chối; Phiếu 5 có luồng TECHNICIAN/ADMIN tải file kết quả DOCX/PDF để tạo hồ sơ, ADMIN phát hành trực tiếp, không bắt nhập các bảng II–VII.
- **Chưa nghiệm thu:** Word thật cho export Phiếu 3, Chrome E2E bằng tài khoản thật, file thật qua toàn bộ flow, notification/WebSocket hai phiên, migration trên database làm việc và quy định LAB về format mã/seal/metadata.

### Bằng chứng kiểm tra gần nhất — 19/11/2026

- Backend compile bằng Java 21/Docker: `BUILD SUCCESS`.
- Frontend: `npm run build` — `✓ built in 951ms`.
- Backend full suite trên MySQL test cô lập: lượt đầu ngày 19/11 `Tests run: 85, Failures: 2, Errors: 1, Skipped: 3` (test cũ `SampleCodingServiceTest` của quy trình xác minh hai người); lượt sau, ghi ở `docs/ai/CONTEXT.md`, là `Failures: 0, Errors: 0, Skipped: 3`, `BUILD SUCCESS`. Lấy lượt sau làm mốc hiện hành, vẫn cần chạy lại trước khi nghiệm thu.
- Không chỉnh sửa V1–V4, `.env` hoặc secrets; không tạo commit trong phiên này.

### Quyết định người dùng đã xác nhận ngày 18/11/2026

- Mỗi Phiếu 1 có một Phiếu 7 và một Phiếu 2. Mỗi phiếu có số riêng và tham chiếu số Phiếu 1. Nhánh khách tự lấy mẫu chưa chốt.
- Chị Thúy mã hóa và nhập mã thủ công ở Phiếu 3; bỏ bước xác minh hai người trên hệ thống. Vẫn cần chốt format mã, quyền sửa, xử lý trùng và thời điểm áp dụng cho dữ liệu cũ; đối chiếu/cập nhật CFC-TN-14 theo quy trình phê duyệt của LAB.
- Phiếu 4 **chính là bước bàn giao mẫu** sau khi tiếp nhận đã mã hóa; KCS nhận/từ chối bàn giao. Không tạo thêm biểu mẫu/biên bản nhận mẫu riêng theo yêu cầu hiện tại.
- Tại bước 5 dùng chính file kết quả `2026.11.16.docx` làm căn cứ, tải lên thay cho nhập lại thiết bị, hóa chất, đường chuẩn/công thức, QC và số liệu từng dòng trên màn hình. Người dùng chốt **bỏ bước người khác kiểm tra/duyệt trước phát hành**; không tự bỏ các bước bàn giao mẫu hoặc kiểm tra tệp hợp lệ.

### Bổ sung người dùng ngày 19/11/2026

- Một Phiếu 1 có thể có một hoặc nhiều Phiếu 3, mẫu/lô; một mẫu không được nằm trong nhiều Phiếu 4. Quy định trước đó về một Phiếu 7 và một Phiếu 2 cho mỗi Phiếu 1 vẫn giữ; nếu đi thẳng tới Phiếu 3 thì hai phiếu này có thể không có.
- Khách tự lấy mẫu: người dùng có thể lựa chọn thực hiện Phiếu 7/2 hoặc đi từ Phiếu 1 thẳng đến Phiếu 3; không ép tạo Phiếu 7/2 giả.
- Chỉ ADMIN được sửa mã mẫu; chị Thúy là người nhập/mã hóa. Cần phân biệt quyền nhập ban đầu với quyền chỉnh sửa về sau, không suy ra chị Thúy là ADMIN.
- ADMIN là role phát hành; câu trả lời về quản lý phiếu là ADMIN, nhưng không suy ra tiếp nhận hay KCS mất quyền thực hiện các bước đã xác nhận. Quyền tạo/sửa/hoàn tất cụ thể từng phiếu cần làm rõ khi thiết kế RBAC.
- Số niêm phong và chỉ tiêu bám theo các file DOCX; số phiếu cấp tự động hay nhập tay chưa chốt. Câu 9 về thay đổi yêu cầu/version để sau, chưa xác định chính sách.

## 1. Mục tiêu và phạm vi

Luồng mong muốn khi CFC lấy mẫu hiện trường: **Phiếu 1 yêu cầu kiểm nghiệm → Phiếu 7 kế hoạch lấy mẫu → Phiếu 2 lấy mẫu hiện trường → Phiếu 3 tiếp nhận và bàn giao → Phiếu 4 chuyển mẫu → bước 5 tải file kết quả → phát hành**, không có bước người khác duyệt trước phát hành.

- Tìm bằng số Phiếu yêu cầu để chọn đúng hồ sơ, hiển thị trước các thông tin kế thừa, nhân viên kiểm tra/chỉnh các thông tin thực tế được phép sửa rồi lưu.
- Mã mẫu do chị Thúy mã hóa và nhập tay ở Phiếu 3; bỏ xác minh hai người trên hệ thống theo xác nhận người dùng, đồng thời sửa các gate tương ứng sau khi thống nhất cách chuyển đổi an toàn.
- Rà soát riêng số niêm phong mẫu, số seal container và chỉ tiêu yêu cầu; Phiếu 4 là bước bàn giao sau tiếp nhận, không thêm biên bản riêng.
- Hạn chế thay đổi phần thử nghiệm, kết quả và tra cứu ngoài những điểm chịu ảnh hưởng trực tiếp; giữ dữ liệu cũ.

## 2. Đối chiếu mẫu và hiện trạng

| Mẫu | Dữ liệu đáng chú ý | Hiện trạng hệ thống |
|---|---|---|
| 1 — Yêu cầu kiểm nghiệm | Số/No., ngày, khách và liên hệ, lô/giống/nguồn gốc, đơn vị lấy mẫu, từng mẫu/nền mẫu/chỉ tiêu/phương pháp, hạn và xác nhận khách | Chưa thấy chức năng lập và quản lý Phiếu 1 riêng; Phiếu 3 đang nhập `requestNumber` thủ công, bắt buộc, chưa chứng minh có liên kết Phiếu 1. |
| 7 — Kế hoạch lấy mẫu | Số/No., **Số giấy đề nghị**, nhân sự, thời gian/địa điểm, lô, vùng trồng/đóng gói, phương án và điều kiện vận chuyển | Chưa thấy chức năng lập kế hoạch riêng. Mẫu được cung cấp dành riêng cho sầu riêng; không mặc định áp dụng mọi loại hàng. |
| 2 — Lấy mẫu hiện trường | Số/No., **Số giấy đề nghị**, thời gian, người lấy mẫu, lô, số xe/moóc/container, mẫu, phương pháp lấy mẫu, khối lượng, chỉ tiêu và số niêm phong | Chưa thấy chức năng lập Phiếu 2 riêng. Cần phân biệt phương pháp **lấy mẫu** và phương pháp **thử nghiệm**. |
| 3 — Tiếp nhận và bàn giao | Số/No., **Phiếu yêu cầu số**, mã mẫu, người giao/nhận, số niêm phong mẫu, tình trạng, điều kiện, chỉ tiêu và quyết định tiếp nhận | Có Phiếu 3, mẫu, ảnh/file; số yêu cầu đang là chuỗi nhập tay. Hiện có một `sealNumber`; chưa thấy hai trường riêng cho seal container và niêm phong mẫu. Template Phiếu 3 người dùng đang sửa là thay đổi hiện có, cần giữ nguyên khi triển khai. |
| 4 — Chuyển mẫu | Số/No., **Phiếu yêu cầu số**, từng mẫu, khối lượng, số lượng, tình trạng, chỉ tiêu, phương pháp, hạn trả, bàn giao | Có Phiếu 4; một số màn hình gắn nhãn “Phiếu yêu cầu” nhưng hiển thị `receiptCode` (mã Phiếu 3), cần sửa sau khi có số yêu cầu thật. |
| 5 — Biên bản thử nghiệm | Mẫu đã bàn giao, file kết quả DOCX/PDF, người tải, thời điểm và liên kết hồ sơ | Màn hình tạo mới chỉ chọn mẫu và tải file; dữ liệu thiết bị/hóa chất/QC/số liệu nằm trong file do phòng phân tích lập. |

File `2026.11.16.docx` được cung cấp là **Phiếu kết quả thử nghiệm / Test Report**: có thông tin mẫu, các chỉ tiêu, phương pháp và kết quả để trả khách. Nội dung đọc được không có bảng thiết bị, hóa chất, QC, đường chuẩn hay số liệu thô như mẫu Biên bản thử nghiệm số 5. Theo quyết định người dùng, hệ thống dùng chính file kết quả này ở bước 5; không mô tả nó như thể chứa những dữ liệu không có trong file. Cần cập nhật mô tả quy trình và biểu mẫu thực tế theo quyết định này.

Trong mẫu 1 có cả lựa chọn **Công ty lấy mẫu** và **Khách hàng tự lấy mẫu**. Với khách tự lấy mẫu, người dùng có thể lựa chọn 1 → 7 → 2 → 3 hoặc đi trực tiếp 1 → 3; với tuyến hiện trường thông thường theo 1 → 7 → 2 → 3.

Trước P3, mã mẫu do hệ thống sinh và có trạng thái `PENDING_CODING → PENDING_VERIFICATION → VERIFIED` làm gate chuyển/thử nghiệm/phát hành. P3 đã chuyển sang nhập tay và bỏ xác minh hai người (xem trạng thái P0–P4 ở đầu file); dữ liệu cũ mang trạng thái `VERIFIED/legacy` vẫn cần được giữ truy xuất, không tự ghép lại.

## 3. Số hồ sơ và liên kết — thiết kế đề xuất

- `request_id` là khóa nội bộ; **số Phiếu 1** là mã nghiệp vụ có thể tra cứu, duy nhất theo quy tắc LAB duyệt. Chuẩn hóa khoảng trắng/chữ hoa khi tìm; chống trùng và cấp số đồng thời nếu chọn cấp tự động. Không dùng số điện thoại/email làm mã liên kết.
- **Số/No. của từng phiếu là số riêng**; **Số giấy đề nghị/Phiếu yêu cầu số** tham chiếu số Phiếu 1. Quy tắc định dạng và cấp số còn chờ xác nhận.
- Một Phiếu 1 có tối đa một Phiếu 7 và một Phiếu 2 khi các bước này được chọn; Phiếu 2 liên kết kế hoạch tương ứng khi có. Một Phiếu 1 có thể có một hoặc nhiều Phiếu 3, mẫu/lô. Phiếu 3 chọn yêu cầu và bản ghi hiện trường nếu có; Phiếu 4 chọn Phiếu 3/các mẫu liên quan; mỗi mẫu chỉ được thuộc một Phiếu 4; Phiếu 5 chọn mẫu đã bàn giao. Cần xác định cách xử lý nếu Phiếu 4 bị từ chối/hủy trước khi áp dụng ràng buộc dữ liệu.
- Không join lỏng chỉ bằng text `requestNumber`: tìm số → xác nhận hồ sơ → lưu khóa liên kết. Không tự ghép các hồ sơ cũ chỉ vì trùng chuỗi; đưa ra danh sách đối chiếu/migration riêng.
- Giữ snapshot dữ liệu khi hoàn tất/gửi từng phiếu; sửa nguồn về sau không âm thầm thay nội dung hồ sơ đã xác nhận. Có lịch sử người sửa, thời điểm và lý do cho chỉnh sửa có ảnh hưởng.

## 4. Tự điền theo nguồn

| Đích | Gợi ý điền | Nhân viên phải kiểm tra / bổ sung |
|---|---|---|
| Phiếu 7 từ 1 | Số yêu cầu, khách, địa chỉ, liên hệ, mẫu/lô, giống, chỉ tiêu | Người lấy mẫu, thời gian, địa điểm, lô thực tế, phương án, điều kiện; không suy đoán dữ liệu thiếu. |
| Phiếu 2 từ 1 + 7 | Số yêu cầu, khách, kế hoạch, mẫu, vùng trồng, cơ sở đóng gói, xe/moóc/container, chỉ tiêu dự kiến | Thời gian và cách lấy thực tế, khối lượng/số lượng, tình trạng, bảo quản, niêm phong mẫu, ghi chú sai khác. |
| Phiếu 3 từ 1 + 2 (nếu có) | Số yêu cầu, khách, mẫu/lô, người và thời điểm lấy, vùng trồng, đóng gói, xe/moóc/container, số niêm phong mẫu, chỉ tiêu, hạn trả khi có | Người giao/nhận, giờ nhận, số lượng và khối lượng thực nhận, bao bì, tình trạng niêm phong và mẫu, nhiệt độ, kết luận; ghi sai khác, không tự coi là đã kiểm tra. Đi thẳng từ 1 thì không suy đoán dữ liệu Phiếu 2. |
| Phiếu 4 từ 3 + 1 | Số yêu cầu thật, mã mẫu nhập tay, tên, số lượng, khối lượng, tình trạng, chỉ tiêu; gợi ý phương pháp thử và hạn từ yêu cầu nếu có | Đối chiếu trước khi gửi, phương pháp thử hợp lệ, người giao, ngày chuyển, hạn, yêu cầu lưu/trả mẫu và thay đổi. |
| Bước 5 từ 4 | Số yêu cầu thật, mã/tên mẫu, số lượng, ngày nhận, hạn, chỉ tiêu/phương pháp được bàn giao | Chọn mẫu, tải file kết quả; ADMIN phát hành. Metadata tối thiểu cần chốt; không bắt nhập các bảng II–VII. |

Thông tin khách có thể cần cho nhân viên tạo Phiếu 1/2/3; KCS tiếp tục dùng projection giới hạn theo RBAC. File gốc được phép xem có thể chứa thông tin khách: xác nhận chính sách với LAB trước khi mở thêm quyền.

## 5. Các điểm nghiệp vụ phải thiết kế kỹ

1. **Hai loại seal:** mẫu 2 có niêm phong “Có, số / Không”; mẫu 3 có số niêm phong và trạng thái “Còn nguyên / Không có/đã mở”; mẫu 7 yêu cầu ghi seri niêm phong vào Phiếu 2 và nêu niêm phong còn nguyên là điều kiện coi là mẫu do Công ty lấy. Đề xuất tách `sampleSealNumber` và `containerSealNumber` theo yêu cầu người dùng; các mẫu 1/7/2/3/4 được đọc không nêu riêng trường số seal container. Cần xác định cách ghi seal container theo yêu cầu thực tế, số lượng và xử lý thiếu/sai; không tự kết luận từ biểu mẫu. Dữ liệu `sealNumber` cũ chưa rõ nghĩa: không tự chia hoặc ghi đè.
2. **Chỉ tiêu/nền mẫu:** lưu theo từng mẫu và từng nền mẫu khi cần; phân biệt giá trị khách yêu cầu, thực lấy, tiếp nhận và bàn giao. Bất đồng phải hiện cảnh báo/ghi lý do và quyền phê duyệt theo quy định đã duyệt; chưa mặc định chặn hay tự chọn. `-V/-R` và QC/mã mù không tự bỏ khỏi quy trình CFC-TN-14.
3. **Mã thủ công:** chị Thúy nhập mã cho từng mẫu; chỉ ADMIN sửa mã sau đó. Kiểm tra rỗng, độ dài, định dạng do LAB chốt, uniqueness phía DB và khi nhiều người nhập cùng lúc; ghi người nhập, thời điểm, lịch sử và lý do sửa. Chốt quyền nhập ban đầu, thời điểm gắn nhãn và xử lý mã đã dùng; không ghi mặc định tên một cá nhân thay cho tài khoản thật.
4. **Bỏ check mã hóa:** bỏ bước xác minh hai người như người dùng đã chốt; cập nhật gate chuyển mẫu, ứng viên thử nghiệm và phát hành cho quy trình mới một cách nhất quán. Giữ kiểm tra trùng/định dạng theo quy tắc được duyệt, nhật ký và truy xuất. Quy trình CFC-TN-14 trước đây yêu cầu đối chiếu độc lập; cập nhật tài liệu áp dụng theo phê duyệt LAB. Không tự chuyển toàn bộ dữ liệu cũ sang `VERIFIED`.
5. **Bàn giao:** sau Phiếu 3 và nhập mã, lập/gửi Phiếu 4; KCS nhận hoặc từ chối và ghi nhận người, thời gian, tình trạng. Không tạo thêm biên bản riêng.
6. **File và phát hành:** bước 5 dùng file kết quả do nhân viên tải lên; không yêu cầu nhập lại các bảng II–VII và không chờ người khác kiểm tra/duyệt trước phát hành. ADMIN phát hành; xác định metadata tối thiểu; kiểm tra định dạng, kích thước, quyền truy cập và nội dung file theo khả năng hệ thống; lưu nguồn, checksum, người tải/người phát hành, thời điểm, lịch sử và phiên bản để truy xuất. Không xóa dữ liệu thử nghiệm đã có hoặc tự công bố ngay khi tải file nếu hành động phát hành chưa được thực hiện.

## 6. Các giai đoạn triển khai

> Chú thích: `[x]` = đã có implementation trong source (chưa nghiệm thu production); `[~]` = làm một phần hoặc còn điểm chờ LAB; `[ ]` = chưa làm. Đánh số P0–P6 **của plan này**; trong `docs/ai/CONTEXT.md` "P5–P6" là dashboard Admin và ZBS/SMS, thuộc roadmap hệ thống.

- [~] **P0 — Chốt nghiệp vụ:** đã chốt luồng 1 → (7/2) → 3 → 4 → 5, mã nhập tay, bỏ xác minh hai người, ADMIN phát hành file; còn chờ LAB các điểm mục 8 (số phiếu, format mã, seal container, metadata file, cập nhật CFC-TN-14). Ghi quyết định và người xác nhận.
- [x] **P1 — Nền hồ sơ yêu cầu:** thêm bảng/khóa, migration Flyway mới không sửa migration cũ; tạo API/UI Phiếu 1, nháp/hoàn tất, số yêu cầu và tìm kiếm; quy tắc validation/trùng và actor.
- [~] **P2 — Lấy mẫu (đã có API/UI; chưa export DOCX):** tạo API/UI Phiếu 7 và 2 liên kết Phiếu 1; tách dữ liệu kế hoạch và thực tế, mẫu và chỉ tiêu theo dòng; export theo biểu mẫu được duyệt nếu LAB cần.
- [x] **P3 — Tiếp nhận:** tìm Phiếu 1, chọn Phiếu 2 phù hợp, xem trước dữ liệu tự điền; bổ sung trường, phân biệt seal, kiểm tra sai khác, nhập mã thủ công theo chính sách được duyệt, ảnh/file và lưu snapshot. Có đường xử lý yêu cầu/phiếu cũ, khách tự lấy mẫu theo quyết định P0.
- [x] **P4 — Chuyển và thử nghiệm:** nối Phiếu 4/5 với số yêu cầu thật, sửa nhãn đang hiển thị nhầm mã Phiếu 3, điền đúng từng mẫu, số lượng/khối lượng, chỉ tiêu và phương pháp; bỏ xác minh hai người và điều chỉnh gate có kiểm soát. Phiếu 4 ghi nhận bước KCS nhận/từ chối. Thêm tải file kết quả cho bước 5, xem/tải theo quyền, kiểm tra định dạng/kích thước/nội dung, lưu metadata/checksum và audit; không bắt nhập các bảng II–VII.
- [~] **P5 — Kết quả và dữ liệu cũ (ADMIN phát hành file-first đã có; migration dữ liệu cũ/báo cáo ngoại lệ, seal container và rà public chưa nghiệm thu):** sửa backend lẫn UI để ADMIN phát hành file từ bước 5 không qua người khác duyệt; thao tác phát hành rõ ràng, cập nhật trạng thái và lịch sử, giữ dữ liệu cũ. Đối chiếu ý nghĩa seal container; kiểm tra release/public tránh đưa sai seal hay thông tin khách. Migration dữ liệu cũ có báo cáo ngoại lệ và cách rollback/phục hồi, không tự ghép nếu không chắc.
- [ ] **P6 — Nghiệm thu (chưa chạy: Chrome E2E ba role, Word thật, WebSocket hai phiên, migration DB làm việc):** API/MySQL tests cho quan hệ nhiều mẫu, trùng số/mã và đồng thời, sai lệch, quyền, rollback và dữ liệu cũ; test biểu mẫu/export bằng Word thật nếu xuất DOCX; E2E từng role trên Chrome thật, desktop/mobile, đi hết 1 → 7 → 2 → 3 → 4 → 5 và nhánh khách tự lấy mẫu nếu được duyệt. Ghi rõ test chưa chạy.

## 7. Tiêu chí nghiệm thu

- Chọn đúng số Phiếu 1 thì các phiếu sau lấy đúng hồ sơ liên quan, không nhầm giữa hai khách hay hai lô; không tự điền số yêu cầu từ mã Phiếu 3.
- Dữ liệu thực nhận và các chênh lệch có thể được nhân viên ghi lại, không mất lịch sử/snapshot. Số lượng và khối lượng là hai dữ liệu riêng.
- Mã mẫu nhập tay đúng chính sách đã duyệt, không trùng; trạng thái/gate phù hợp quyết định LAB và hồ sơ cũ vẫn truy xuất được.
- Số niêm phong mẫu và seal container không bị trộn; chỉ tiêu, nền mẫu và phương pháp hiển thị đúng từ nguồn được xác nhận.
- Tải file kết quả tại bước 5 không đòi nhập bảng thiết bị/hóa chất/QC/số liệu và không cần người khác duyệt để phát hành; vẫn phân quyền, ghi người thực hiện và giữ lịch sử phát hành.
- Phân quyền và bảo vệ thông tin khách không bị nới ngoài quyết định LAB; chạy test và ghi kết quả thật trước khi đánh dấu hoàn thành.

## 8. Câu hỏi cần LAB/sếp xác nhận

1. Đã chốt: Phiếu 1 có thể có một hoặc nhiều Phiếu 3, mẫu/lô; mỗi mẫu không thuộc nhiều Phiếu 4. Cần làm rõ trường hợp Phiếu 4 bị từ chối/hủy trước khi thiết kế ràng buộc.
2. Số/No. của từng loại phiếu cấp tự động hay nhân viên nhập, định dạng và phạm vi duy nhất? Người dùng chưa chắc; không tự coi câu hỏi là quyết định. Mỗi phiếu có số riêng và cùng tham chiếu số Phiếu 1.
3. Đã chốt: khách tự lấy mẫu có thể chọn làm 7/2 hoặc đi thẳng 1 → 3. Cách nhập hồ sơ lịch sử còn chờ làm rõ.
4. ADMIN sửa mã; chị Thúy nhập tại Phiếu 3; bỏ xác minh hai người. Format mã thủ công, quyền nhập ban đầu và xử lý mã cũ còn cần làm rõ.
5. Người dùng yêu cầu theo DOCX: Phiếu 2 ghi niêm phong có số/không; Phiếu 3 ghi số và trạng thái; Phiếu 7 nêu điều kiện tem nguyên. File chưa quy định riêng seal container hoặc cách xử lý số sai; cần xác nhận các điểm này thay vì tự đặt quy định.
6. Đã xác nhận Phiếu 4 là bước bàn giao; không yêu cầu biên bản riêng. Cần chốt trường và thao tác KCS khi nhận/từ chối.
7. Đã chốt ADMIN phát hành, dùng file kết quả `2026.11.16.docx` tại bước 5 và bỏ người khác duyệt trước phát hành. Còn cần chốt metadata bắt buộc, loại/kích thước file và cách xử lý khi kết quả sai.
8. Người dùng trả lời ADMIN; chưa rõ ADMIN có độc quyền lập/sửa/hoàn tất tất cả phiếu hay chỉ quản lý. Giữ các bước đã chốt cho tiếp nhận và KCS; hình thức ký và sửa sau hoàn tất chưa chốt.
9. Người dùng yêu cầu tạm bỏ qua: xử lý thay đổi yêu cầu/version và định dạng xuất chưa có quyết định.

## Nguồn đã đối chiếu

- `docs/template/1. Phiếu yêu cầu kiểm nghiệm.docx`, `7.Ke_hoach_lay_mau_sau_rieng.docx`, `2. Phiếu lấy mẫu hiện trường.docx`, `3. Phiếu tiếp nhận và bàn giao mẫu.docx`, `4.Phieu chuyen mau.docx`, `5.Bien_ban_thu_nghiem.docx`.
- Source liên quan: `ReceiptService`, `SampleCodingService`, `TransferService`, `TestingRecordRepository`, `ReportPdfService` và các màn hình Phiếu 3/4/5. Đây là khảo sát để lập kế hoạch, không phải nghiệm thu toàn bộ runtime.
