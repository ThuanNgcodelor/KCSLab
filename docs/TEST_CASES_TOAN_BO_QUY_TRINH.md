# Bộ case kiểm thử toàn bộ quy trình CFC Lab

**Cập nhật:** 17/11/2026  
**Phạm vi:** đăng nhập → Phiếu 3 → mã hóa/dán nhãn → Phiếu 4 → KCS nhận và xác minh → Phiếu 5 → duyệt biên bản → hồ sơ KQKN-DV → phát hành → tra cứu công khai.

> Chạy các case tạo dữ liệu trên môi trường thử nghiệm hoặc database đã chuẩn bị cho kiểm thử. Không tạo phiếu hoặc phát hành kết quả trên dữ liệu vận hành thật chỉ để thử.

## 1. Vai trò và chuẩn bị

| Vai trò | Dùng để kiểm tra |
|---|---|
| `RECEPTIONIST` | Tạo Phiếu 3, mã hóa/dán nhãn, lập và gửi Phiếu 4. |
| `TECHNICIAN` | Xem Phiếu 4, nhận/từ chối, xác minh mã sau khi nhận, lập và gửi Phiếu 5. |
| `ADMIN A` | Kiểm tra Phiếu 5; có thể lập hồ sơ kết quả. |
| `ADMIN B` | Duyệt/phát hành hồ sơ do Admin A hoặc Technician lập. Phải khác người lập và người phân tích. |
| Khách tra cứu | Không cần đăng nhập; dùng mã tra cứu và 4 số cuối điện thoại. |

Trước khi chạy:

1. Khởi động MySQL và Redis đang dùng cho môi trường test.
2. Chạy backend bằng `./scripts/run-be-docker.sh` để có LibreOffice chuyển DOCX sang PDF.
3. Chạy frontend bằng `cd source/fe && npm run dev`.
4. Chuẩn bị Technician và hai Admin đang hoạt động. Nếu chỉ có một Admin, chỉ kiểm tra được tới trạng thái **Chờ duyệt**.
5. Dùng số điện thoại test riêng, không dùng số khách thật.

### Dữ liệu test gợi ý

Mỗi lần chạy dùng một **Số phiếu yêu cầu** riêng do người kiểm thử tự đặt, ví dụ `E2E-20261117-01`. Đây là trường bắt buộc trên form Phiếu 3, dùng làm số tham chiếu của yêu cầu kiểm nghiệm; không phải mã Phiếu 3 `TN-…` do hệ thống sinh. Nếu test theo hồ sơ giấy thật, nhập đúng số trên hồ sơ đó.

```text
Khách hàng: CFC Lab Test 20261117-01
Số điện thoại: 0900000017
Số phiếu yêu cầu: E2E-20261117-01
Người giao: Người giao mẫu test
Người lấy mẫu: Nhân viên lấy mẫu test
Ngày lấy mẫu: ngày test
Vùng trồng / nguồn mẫu: CT001 – Vườn test
Cơ sở đóng gói: Cơ sở test
Biển số xe: 65A-000.17
Số móc: MOOC-TEST-01
Số container: CONT-TEST-01
Số niêm phong: SEAL-TEST-01
Trạng thái niêm phong: Còn nguyên
Phương tiện / điều kiện vận chuyển: Xe tải kín, bảo quản mát
Tên mẫu: Sầu riêng test quy trình
Loại sản phẩm: Sầu riêng
Mã nguồn: CT001
Số lượng/khối lượng: 1 mẫu / 1 kg
Bao gói: Túi PE
Tình trạng mẫu: Nguyên vẹn
Chỉ tiêu yêu cầu: Cadimi
Email/địa chỉ: có thể bỏ trống trong case này
Hạn trả: chọn ngày hợp lệ trên form Phiếu 3/Phiếu 4
```

Các mục trong phần **“Hồ sơ nguồn và lô hàng”** trên ảnh form là dữ liệu nguồn/vận chuyển (vùng trồng, cơ sở đóng gói, biển số xe, số móc, container, niêm phong, điều kiện vận chuyển); không phải “Số phiếu yêu cầu”.

Ghi lại mã hệ thống sinh ra: Phiếu 3 `TN-…`, mã mẫu `CT001-SR-…`, Phiếu 4 `CM-…`, biên bản `BBTN-…`, số phiếu KQKN và mã tra cứu.

## 2. Luồng thành công từ đầu đến cuối

### E2E-01 — Tiếp nhận, bàn giao, thử nghiệm, phát hành, tra cứu

| Bước | Vai trò và thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Receptionist đăng nhập và tạo Phiếu 3. Điền người giao, số điện thoại, **Số phiếu yêu cầu**, khách hàng/đơn vị, trạng thái niêm phong và ít nhất một mẫu có tên, loại sản phẩm, mã nguồn, chỉ tiêu yêu cầu. Điền thêm các trường nguồn/lô hàng theo dữ liệu test ở trên. | Phiếu lưu thành công; người tiếp nhận là tài khoản đăng nhập; mã TN do hệ thống sinh riêng với số phiếu yêu cầu; dữ liệu nguồn/lô hàng và mẫu được lưu đúng. |
| 2 | Đính kèm ảnh mẫu/ảnh lô hàng nếu có. | Ảnh được lưu, xem lại được và nằm đúng nhóm hồ sơ. |
| 3 | Sinh mã mẫu, xác nhận mã hóa/dán nhãn và gửi đối chiếu. | Mã gắn với đúng mẫu, trạng thái mã `PENDING_VERIFICATION`. |
| 4 | Receptionist lập Phiếu 4, nhập phương pháp, ngày chuyển, hạn trả, thông tin bàn giao; lưu nháp rồi gửi KCS. | Mẫu `PENDING_VERIFICATION` vẫn chuyển được; Phiếu 4 thành `PENDING`; Technician thấy phiếu trong hàng chờ. |
| 5 | Technician mở Phiếu 4, đối chiếu mã, số lượng, khối lượng, ngày, hạn, ghi chú và file/ảnh. | Thông tin khớp hồ sơ; file tải được. Technician không được xem API Phiếu 3 đầy đủ hoặc thông tin liên hệ khách không cần thiết. |
| 6 | Technician xác nhận đã nhận mẫu. | Phiếu 4 thành `ACCEPTED`, lưu người nhận/thời điểm; Receptionist nhận thông báo KCS đã nhận. |
| 7 | Technician đối chiếu mẫu thực tế với nhãn/hồ sơ rồi xác minh mã. | Mã thành `VERIFIED`, có lịch sử xác minh. Người xác minh khác người mã hóa theo rule hiện tại. |
| 8 | Technician lập Phiếu 5, nhập thời gian, thiết bị, hóa chất, QC và số liệu; lưu rồi gửi kiểm tra. | Tạo biên bản `BBTN-…` gắn đúng Phiếu 3/4/mã mẫu, trạng thái chờ kiểm tra. |
| 9 | Admin A kiểm tra biên bản rồi chọn **Xác nhận đạt**. | Biên bản thành `REVIEWED`, lưu người kiểm tra và thời điểm. |
| 10 | Admin A hoặc Technician tạo hồ sơ kết quả từ biên bản đã kiểm tra. | Tạo được bản nháp kết quả gắn đúng biên bản. |
| 11 | Chọn `docs/template/KQKN-SR-CT001-092026.docx`, bấm **Tải phiếu lên**; không nhập bảng chỉ tiêu hoặc thêm file phụ. | DOCX chuyển thành PDF; file hiện tên/dung lượng/checksum và xem trước được. Khi đã có file phiếu, bảng nhập tay và file phụ là tùy chọn. |
| 12 | Người lập kiểm tra PDF rồi bấm **Gửi duyệt**. | Hồ sơ thành `SUBMITTED`/Chờ duyệt; người lập không sửa file trong lúc chờ. |
| 13 | Admin B (khác người lập và người phân tích) mở hồ sơ, kiểm tra PDF rồi bấm **Phát hành**. | Hồ sơ thành `PUBLISHED`; lưu sự kiện, cấp mã tra cứu và PDF phát hành có QR/mã. |
| 14 | Mở `/tracuu/` bằng cửa sổ ẩn danh, nhập mã tra cứu và 4 số cuối điện thoại. | Tra cứu thành công; thấy kết quả/file công khai; không lộ email, số điện thoại đầy đủ hoặc tên người tiếp nhận trong dữ liệu public. |
| 15 | Tải PDF; thử mở QR trong PDF rồi nhập 4 số cuối điện thoại. | Tải đúng PDF phát hành. Quét QR vẫn phải nhập 4 số cuối điện thoại. |

**Đạt khi:** mã và hồ sơ liên kết đúng, trạng thái đi đúng thứ tự, thông báo tới đúng role, PDF đọc được và khách chỉ xem được hồ sơ đã phát hành.

## 3. Case lỗi và kiểm tra chặn nghiệp vụ

### IN-01 — Kiểm tra các trường bắt buộc của Phiếu 3

1. Thử lưu khi bỏ trống lần lượt **Số phiếu yêu cầu**, số điện thoại, người giao, khách hàng/đơn vị, trạng thái niêm phong, tên mẫu, loại sản phẩm, mã nguồn hoặc chỉ tiêu yêu cầu.
2. Thử số điện thoại không bắt đầu bằng `0` hoặc không đủ 10–11 chữ số.
3. Điền đủ dữ liệu hợp lệ rồi lưu.

Mong đợi: mỗi lần thiếu/sai dữ liệu, form báo đúng trường và không tạo phiếu; điền hợp lệ thì lưu được. Theo form hiện tại, **Số phiếu yêu cầu là bắt buộc**, dù cột lưu ở backend cho phép null.

### COD-01 — Người mã hóa không tự xác minh

1. Receptionist A tạo mã và gửi đối chiếu.
2. Cùng Receptionist A thử xác minh mã.

Mong đợi: thao tác bị từ chối; mã vẫn chờ xác minh; không ghi sự kiện `VERIFIED`. Trong luồng chuẩn, Technician nhận mẫu rồi mới xác minh.

### COD-02 — Mã chưa xác minh thì chưa lập Phiếu 5

1. Tạo mẫu, gửi mã đối chiếu nhưng chưa xác minh.
2. Lập/gửi Phiếu 4 và nhận mẫu nếu cần, rồi thử lập Phiếu 5.

Mong đợi: Phiếu 4 được bàn giao trước xác minh; mẫu chưa `VERIFIED` không thể chọn/lập biên bản thử nghiệm.

### TR-01 — Từ chối bàn giao phải có lý do

1. Tạo và gửi Phiếu 4 riêng.
2. Technician chọn **Từ chối**, để trống lý do rồi thử gửi.
3. Nhập lý do “Niêm phong không khớp thông tin trên phiếu” rồi gửi lại.

Mong đợi: thiếu lý do thì không gửi được; có lý do thì Phiếu 4 thành `REJECTED`, lưu lý do và thông báo theo thiết kế; mẫu bị từ chối không được đưa vào Phiếu 5.

### TR-02 — Hai Technician nhận đồng thời

1. Hai phiên Technician mở cùng Phiếu 4 `PENDING`.
2. Gần như đồng thời, cả hai xác nhận nhận.

Mong đợi: chỉ một thao tác thành công; thao tác còn lại nhận xung đột `409` và phải tải lại dữ liệu; chỉ ghi một người nhận và không tạo notification nhận trùng.

### TEST-01 — Không lập hồ sơ kết quả trước khi duyệt Phiếu 5

1. Technician gửi Phiếu 5 để kiểm tra.
2. Trước khi Admin xác nhận đạt, thử lập hồ sơ kết quả.

Mong đợi: bị chặn; chỉ tạo được hồ sơ sau khi biên bản thành `REVIEWED`.

### RES-01 — Upload DOCX hợp lệ, không nhập thêm

1. Trên hồ sơ `DRAFT`, tải lên DOCX KQKN thật.
2. Không nhập bảng kết quả, người ký hoặc file phụ.

Mong đợi: DOCX chuyển PDF; file báo cáo hiện trong hồ sơ; có thể gửi duyệt khi file hợp lệ. Xem trước PDF và đối chiếu nội dung, trang, chữ ký, mã trước khi phát hành thật.

### RES-02 — Upload PDF có sẵn

Mong đợi: PDF được nhận mà không cần LibreOffice, xem/tải được và không bị hỏng.

### RES-03 — File lỗi, sai định dạng hoặc quá lớn

Thử file text đổi đuôi `.docx`, PDF hỏng, file `.exe` và file lớn hơn 10 MB.

Mong đợi: nội dung/đuôi sai bị từ chối (thường `415`), file quá lớn bị từ chối (`413`); không lưu file lỗi; bản nháp vẫn chỉnh sửa được.

### RES-04 — Người lập không tự duyệt

1. Admin A hoặc Technician lập hồ sơ và gửi duyệt.
2. Người lập thử phát hành hồ sơ của mình.
3. Admin B thử phát hành.

Mong đợi: người lập bị từ chối; Admin B có thể duyệt nếu cũng khác người phân tích và các điều kiện nghiệp vụ đạt.

### RES-05 — Trả hồ sơ cần lý do

Admin B thử trả lại hồ sơ khi không nhập lý do, rồi thử lại với lý do cụ thể.

Mong đợi: thiếu lý do thì không thực hiện; có lý do thì hồ sơ thành `RETURNED`, lưu lịch sử và người lập sửa/gửi lại được.

### PUB-01 — Tra cứu sai không tiết lộ hồ sơ tồn tại

Thử mã không tồn tại, mã đúng nhưng sai 4 số cuối, mã của hồ sơ chưa phát hành và mã đã thu hồi.

Mong đợi: phản hồi công khai trung tính cùng kiểu `404`, không trả dữ liệu khách hoặc trạng thái nội bộ.

### PUB-02 — Giới hạn thử mã và tải file

Nhập sai mã/SĐT nhiều lần đến khi bị giới hạn; thử tải file với mã đúng nhưng 4 số cuối sai.

Mong đợi: vượt ngưỡng nhận `429`; tải file xác thực lại mã và 4 số cuối; phản hồi kết quả/PDF có `no-store`.

## 4. Notification và phân quyền

### NOT-01 — Thông báo theo chuỗi nghiệp vụ

Chạy E2E-01 và quan sát thông báo của các role.

Mong đợi: Receptionist nhận thông báo KCS nhận/từ chối; KCS/Admin thấy sự kiện tiếp nhận/chuyển theo quyền; request nhận cạnh tranh không tạo thông báo trùng. Ghi lại notification có tên khách để LAB xác nhận chính sách.

### RBAC-01 — Role không được phép truy cập

| Thao tác | Kết quả mong đợi |
|---|---|
| Receptionist mở workspace/API receiving | Bị chặn. |
| Technician gọi API Phiếu 3 đầy đủ `/api/v1/receipts/**` | Bị chặn. |
| Receptionist mở hồ sơ kết quả nội bộ | Bị chặn theo cấu hình hiện tại. |
| Khách chưa đăng nhập mở `/tracuu/` | Trang mở được; dữ liệu chỉ trả khi hồ sơ đã phát hành và xác thực đủ. |

## 5. Kiểm tra giao diện

Kiểm tra Phiếu 3, Phiếu 4, workspace KCS, Phiếu 5, hồ sơ kết quả và tra cứu tại các kích thước:

| Viewport | Kiểm tra |
|---|---|
| 390 × 844 | Form dùng được bằng chạm; dialog vừa màn; gallery/bảng không làm tràn trang. |
| 1024 × 768 | Menu và form không che nội dung. |
| 1440 × 900 | Bố cục desktop và hành động dễ đọc. |
| 2560 × 952 | Nội dung không bị kéo quá rộng; cột/typography cân đối. |

Với gallery, thử mở/đóng bằng bàn phím, Escape, tải file và xem ảnh thật. Kiểm tra loading, lỗi, rỗng và lỗi phiên/quyền `401`/`403`.

## 6. Ghi nhận kết quả

Đánh dấu mỗi case `PASS`, `FAIL` hoặc `BLOCKED`. Không ghi PASS nếu chỉ xem giao diện mà chưa chạy thao tác.

| ID | PASS/FAIL/BLOCKED | Mã phiếu/hồ sơ | Bằng chứng hoặc lỗi |
|---|---|---|---|
| E2E-01 |  |  |  |
| IN-01 |  |  |  |
| COD-01/02 |  |  |  |
| TR-01/02 |  |  |  |
| TEST-01 |  |  |  |
| RES-01…05 |  |  |  |
| PUB-01/02 |  |  |  |
| NOT-01 / RBAC-01 |  |  |  |

Ghi ngày giờ, trình duyệt, role, môi trường/database, viewport, lỗi nguyên văn và ảnh chụp nếu có. Không ghi mật khẩu/token hoặc số điện thoại khách thật.

## 7. Các giới hạn cần ghi rõ

- Không có Admin thứ hai: dừng ở `SUBMITTED/Chờ duyệt`; phát hành và public lookup là `BLOCKED`.
- Không có môi trường test tách biệt: không chạy case tạo/từ chối/phát hành trên dữ liệu vận hành.
- PDF chuyển đổi thành công chưa đồng nghĩa bố cục đã được LAB nghiệm thu. Đối chiếu với Word, chữ ký, mã số, QR và bản in trước khi sử dụng chính thức.
- Thiếu ảnh hiện là cảnh báo theo giả định MVP; LAB cần xác nhận ảnh có bắt buộc hay không.
- Chính sách notification có tên khách, người ký, số phiếu và một số quy tắc CFC-TN-14 còn chờ LAB chốt.
- Đây là bộ test thủ công. Kết quả tự động gần nhất ghi tại `docs/SYSTEM_STATUS.md`; không dùng kết quả lịch sử thay cho lần chạy thủ công hiện tại.
