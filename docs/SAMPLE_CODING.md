# Quy trình tiếp nhận hồ sơ và mã hóa mẫu

**Phiên bản:** 0.2 · **Cập nhật:** 13/11/2026 · **Trạng thái:** CFC-TN-14 đã được xác nhận là quy trình chuẩn; runtime chưa triển khai đầy đủ

## Nguồn đã đối chiếu

- `CFC14Quy trình mã hóa mẫu trái cây.doc` — quy trình CFC-TN-14 được người dùng xác nhận là nguồn chuẩn cho mã hóa mẫu.
- `3. Phiếu tiếp nhận và bàn giao mẫu.docx` — hồ sơ tiếp nhận nguồn.
- `1. Phiếu KQKN-DV.docx` — mẫu Phiếu kết quả thử nghiệm.
- `KQKN-SR-CT001-092026.docx` — ví dụ phiếu kết quả đã điền.

## Ba mã phải tách biệt

| Mã | Thời điểm sinh | Mục đích | Ví dụ/định hướng |
|---|---|---|---|
| Mã hồ sơ tiếp nhận | Khi tạo hồ sơ | Theo dõi phiếu giấy/file và nhóm mẫu | Runtime hiện dùng `TN-YYYYMMDD-RANDOM` |
| Mã số mẫu | Sau kiểm tra hồ sơ và chấp nhận mẫu | Nhận diện mẫu xuyên suốt vòng đời | CFC-TN-14: `CT001-SR-23092026-001` |
| Số phiếu kết quả | Khi phát hành kết quả | Định danh một phiên bản tài liệu phát hành | Ví dụ thực tế: `SR092026-001`, quy tắc chưa được LAB chốt |

Không dùng một mã thay cho cả ba. QR trên mẫu Phiếu kết quả chỉ được tạo khi phát hành và không được chứa ID database tuần tự.

## Quy tắc CFC-TN-14 đã xác nhận từ tài liệu

Mã gốc gồm `SOURCE-PRODUCT-DDMMYYYY-SEQUENCE`:

- `SOURCE`: 2 chữ cái viết hoa + 3 số, ví dụ `CT001`.
- `PRODUCT`: `SR` hoặc `MIT`.
- Ngày là ngày tiếp nhận tại Việt Nam, định dạng `DDMMYYYY`.
- Số thứ tự gồm 3 chữ số, bắt đầu từ `001` mỗi ngày và không tái sử dụng.
- Mẫu nguyên trái có mã gốc; khi tách nền, phần vỏ thêm `-V`, phần ruột/thịt thêm `-R`.
- Mẫu QC có định danh riêng: mẫu trắng `BL`, thêm chuẩn `SP`, mẫu lặp `D`; phải liên kết về lần thử nghiệm/mẫu nguồn.
- Mã mù là mã thứ hai, bảng đối chiếu được lưu riêng và người phân tích không được truy cập trước khi mở mù.
- Sau mã hóa, kiểm nghiệm viên chỉ dùng mã mẫu trên hồ sơ trung gian và dữ liệu thiết bị.

## Quyết định áp dụng và sai lệch tài liệu

CFC-TN-14 là nguồn chuẩn, vì vậy hệ thống dùng `CT001-SR-23092026-001`. Phiếu kết quả ví dụ ghi `SR-CT001-23092026-001` không khớp quy trình và được xem là dữ liệu/biểu mẫu cần rà soát; không dùng sai lệch này để đổi generator hoặc tự sửa ngược dữ liệu lịch sử.

Quy tắc số phiếu kết quả `SR092026-001` cũng mới chỉ suy ra từ một ví dụ, chưa đủ căn cứ triển khai generator.

## Trách nhiệm theo CFC-TN-14

- **Nhân viên tiếp nhận:** kiểm tra thông tin và tình trạng mẫu, thực hiện mã hóa, ghi Sổ theo dõi mẫu, in/viết và dán nhãn.
- **Người đối chiếu độc lập:** phải khác người mã hóa; kiểm tra nhãn, Sổ theo dõi mẫu và phiếu yêu cầu trước khi chuyển mẫu đi chuẩn bị/phân tích.
- **Kiểm nghiệm viên:** chỉ sử dụng đúng mã mẫu đã xác minh trong chuẩn bị, chiết, đo, sổ tay, phiếu trung gian và dữ liệu thô thiết bị.
- **Trưởng/Phó phòng QLCLSP:** kiểm soát, phê duyệt thay đổi và lưu trữ hồ sơ mã hóa; quản lý quyền truy cập mapping mã–thông tin gốc.

## Quy trình TO-BE đề xuất

1. **Tạo hồ sơ nháp:** nhập khách hàng/người giao, file phiếu yêu cầu hoặc biên bản lấy mẫu và thông tin mẫu gốc; chưa sinh mã mẫu.
2. **Kiểm tra hồ sơ và mẫu:** kiểm tra tên/nguồn mẫu, người và ngày lấy mẫu, chỉ tiêu, số lượng, tình trạng, bao bì/niêm phong; ghi quyết định chấp nhận, có điều kiện hoặc từ chối.
3. **Gán mã nguyên tử:** hệ thống cấp sequence theo ngày nghiệp vụ `Asia/Ho_Chi_Minh` trong transaction; không dùng `COUNT(*) + 1`.
4. **In/dán nhãn:** nhãn phải rõ ràng, không phai; dán lên bao bì mẫu gốc và từng lọ sau đồng nhất. Khi chuẩn bị mẫu, sinh mã phần `-V/-R` và nhãn tương ứng.
5. **Đối chiếu độc lập:** người thứ hai, khác người mã hóa, đối chiếu phiếu nguồn, bản ghi điện tử và nhãn; chỉ mẫu đã xác minh mới được chuyển phòng thử nghiệm.
6. **Khóa liên kết:** mọi chỉnh sửa mã tạo bản ghi hiệu chỉnh gồm mã cũ/mới, lý do, người thực hiện và người duyệt; không xóa dấu vết hoặc tái sử dụng mã cũ.
7. **Giới hạn dữ liệu phân tích:** kỹ thuật viên nhận mã mẫu, chỉ tiêu và thông tin kỹ thuật cần thiết; không mặc định nhận danh tính khách hàng.
8. **QC và mã mù:** tạo trong ngữ cảnh lần thử nghiệm, liên kết về mẫu/lô nhưng kiểm soát quyền xem bảng đối chiếu.

Trạng thái đề xuất cho riêng bước mã hóa: `PENDING_CODING → PENDING_VERIFICATION → VERIFIED`; `CORRECTION_REQUIRED` dùng khi đối chiếu không đạt. Trạng thái này không thay thế trạng thái vòng đời mẫu như `TRANSFERRED` hoặc `IN_TESTING`.

## Dữ liệu còn thiếu để lập Phiếu kết quả

Mẫu Phiếu kết quả yêu cầu dữ liệu có cấu trúc, không ghép vào trường mô tả tự do:

- Người lấy mẫu và ngày lấy mẫu.
- Mã vùng trồng; tách biệt với mã nguồn nội bộ kiểu `CT001`.
- Mã cơ sở đóng gói.
- Biển số xe, biển số moóc, số container và seal container.
- Tình trạng/lượng mẫu tại thời điểm tiếp nhận.
- Ngày thử nghiệm và danh sách chỉ tiêu, đơn vị, phương pháp, kết quả, LOD/LOQ/ghi chú.
- Người kiểm tra, người duyệt/ký, ngày phát hành và số phiếu kết quả.

## Thay đổi dữ liệu dự kiến

- Bổ sung trường hồ sơ nguồn cho `receipts`/`samples`: người/ngày lấy mẫu, mã vùng trồng, cơ sở đóng gói và thông tin vận chuyển/container.
- Dùng bộ cấp số nguyên tử theo ngày, có unique constraint trên ngày + sequence và unique toàn hệ thống cho mã hoàn chỉnh.
- Thêm `sample_parts` cho `-V/-R`.
- Thêm lịch sử gán/xác minh/hiệu chỉnh mã với actor và timestamp.
- Thêm bản ghi QC và bảng đối chiếu mã mù có quyền truy cập riêng.
- Mọi business date dùng timezone `Asia/Ho_Chi_Minh`; timestamp audit vẫn lưu UTC.

## Lưu trữ và bảo mật

- Sổ theo dõi mẫu giấy/điện tử là nguồn liên kết mã mẫu với thông tin gốc, chỉ người có thẩm quyền được truy cập.
- Bảng đối chiếu mã mù lưu riêng khỏi khu vực phân tích và chỉ mở khi cần đối chiếu kết quả cuối cùng.
- Hồ sơ được lưu theo thời hạn của quy trình kiểm soát hồ sơ chung; thời hạn cụ thể vẫn cần LAB cung cấp.
- Khi sai mã, không tẩy xóa hoặc ghi đè. Bản điện tử tạo correction event append-only gồm mã cũ, mã đúng, lý do, người sửa, người phê duyệt và thời điểm.
- Mã đã cấp không được tái sử dụng kể cả mẫu bị hủy hoặc kết quả không đạt.

## Acceptance criteria

- Hai request tiếp nhận đồng thời không thể nhận cùng sequence.
- Tiếp nhận lúc giao ngày theo giờ Việt Nam sinh đúng ngày trong mã.
- Người mã hóa không thể tự xác minh bản ghi của mình.
- Mẫu chưa `VERIFIED` không thể lập phiếu chuyển mẫu.
- Mã gốc, `-V/-R`, QC và mã mù truy xuất được về đúng hồ sơ nguồn.
- Sửa mã không xóa mã/lịch sử cũ và mã cũ không bao giờ được cấp lại.
- Nhãn in ra thể hiện đúng mã đã xác minh, đọc được và có thể in lại mà vẫn để lại dấu vết actor/thời điểm.
- Kỹ thuật viên không nhận thông tin khách hàng nếu không có quyền nghiệp vụ.
- Phiếu kết quả lấy dữ liệu từ trường có cấu trúc và snapshot phát hành, không ghép chuỗi thủ công.
