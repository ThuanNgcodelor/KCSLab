# Quản lý hồ sơ 10 năm và export theo năm/tháng/Số CFC

Ngày lập: 25/11/2026. **Trạng thái 25/11/2026: A1, A2, A3.1, A3.2 đã commit (`8a286d1`) và người dùng đã chạy với dữ liệu thật, vì vậy không sửa V24/V25 nữa. Bản chỉnh giao diện và tách folder hồ sơ trùng số (cùng ngày) đã kiểm thử cô lập, chưa commit/triển khai.** A0 (quét dữ liệu thật), A4 (di chuyển file cũ) và A5 (chọn folder trực tiếp) chưa làm. Phần khóa thông báo đã chạy V23 trước khi lập plan này.

## 0. Kết quả triển khai

| Bước | Trạng thái | Ghi chú |
|---|---|---|
| A0 | Chưa chạy trên dữ liệu thật | Trang Admin có tổng hồ sơ/file/byte theo năm, tháng và theo ổ. Xuất với “Cho phép thiếu file” ghi từng file `THIEU_FILE`, `KHAC_CHECKSUM`, `DOC_DO_DANG` trong danh mục gói |
| A1 | Đã code, đã thử | `/admin/dossiers`: cây năm/tháng, tìm Số/No., chi tiết file, xem ảnh, tải file |
| A2 | Đã code, đã thử | Hàng đợi MySQL, lease 5 phút, 1 job/lúc trên luồng riêng, ZIP chia phần, hủy, tải range, hết hạn 7 ngày, audit |
| A3.1 | Đã code, đã thử Linux/Docker | Vị trí từng file, đổi đích upload không cần restart, cảnh báo dung lượng |
| A3.2 | Đã code, đã thử Linux/Docker | File mới trên ổ đã đăng ký nằm ở `năm/tháng/CFC/(intake, handover, results)`. Khi chưa chọn ổ, upload vẫn vào hai thư mục cũ |
| A4, A5 | Chưa làm | A4 cần backup và buổi bảo trì; A5 cần HTTPS |

Khác với đề xuất ban đầu:

- Ổ được nhận diện bằng file `.cfc-storage-id`. Tên thiết bị đổi (ví dụ sda → sdb sau reboot) vẫn cho đọc, chỉ chặn upload đến khi Admin bấm “Kiểm tra đọc/ghi”.
- Dữ liệu có cấu trúc nằm ở `05-du-lieu/`. Mỗi hồ sơ có `danh-muc-file.csv`; mỗi gói có `danh-muc-goi-N.csv` tên riêng để giải nén chung thư mục không ghi đè.
- Mã tra cứu khách (`lookup_code`) và đường dẫn máy chủ bị loại khỏi JSON/CSV xuất. QR trong file PDF đã phát hành vẫn chứa mã như cũ.
- Tạo lại PDF khi ổ chứa ảnh mất kết nối sẽ báo lỗi 503 thay vì âm thầm ra phiếu thiếu ảnh. Ảnh đã mất hẳn vẫn bỏ qua như trước.
- Hồ sơ cũ trùng Số/No. (V16 cho phép) dùng folder riêng `CFC…-r<id>`; chỉ hồ sơ giữ số dùng tên folder không hậu tố. Trước đó xuất tháng có hai hồ sơ trùng số sẽ lỗi.
- Giao diện dùng bộ thành phần chung (`intake-command-strip`, `status-chips`, `ListToolbar`, `staff-table`, `StatusBadge`, `Pagination`, `ConfirmDialog`); chi tiết hồ sơ là trang riêng `/admin/dossiers/:id`.
- Giới hạn 10 lượt chờ toàn hệ thống và 5 lượt mỗi Admin; job làm sập tiến trình 3 lần thì dừng.

Kiểm thử ngày 25/11/2026: toàn suite backend 176 ca, 0 lỗi, 1 bỏ qua (MySQL/Redis cô lập); `npm run build` đạt; Chrome thật với backend/DB tạm ở 390/1024/1440/2560 px không tràn ngang, gồm đổi ổ, gỡ ổ, xuất chặt và xuất có cảnh báo; nâng V23 → V25 trên schema có dữ liệu đạt; xuất 2,4 GB với heap 256 MB và tắt backend giữa chừng hoàn tất, hai ZIP đọc được.

Chưa kiểm thử: Windows, NAS thật, HTTPS, đầy đĩa thật (mới giả lập ngưỡng), tài khoản và dữ liệu thật. V24/V25 chỉ nâng lên, muốn lùi phải phục hồi backup.

## 1. Yêu cầu và hiện trạng đã xác minh

Người dùng muốn Admin có mục navbar riêng, duyệt hồ sơ theo năm → tháng → Số/No., xem mọi ảnh/tài liệu của hồ sơ và xuất theo tháng/năm qua queue; chọn nơi lưu, giữ cây thư mục trong bản xuất. Dữ liệu nội bộ dự kiến lưu 10 năm.

Bổ sung 25/11/2026: **Admin có thể đổi nơi lưu upload mới khi ổ gần đầy**, ví dụ sang ổ khác vào năm thứ 9. File đã lưu ở ổ trước vẫn phải xem/tải/export được. Đây là cấu hình lưu file gốc trên máy chủ, độc lập với nơi lưu bản export.

Hiện tại file chưa nằm chung trong một folder hồ sơ:

| Dữ liệu | Nơi lưu mặc định | Liên kết nghiệp vụ |
|---|---|---|
| Ảnh tiếp nhận, scan Phiếu 3, ảnh lô, bằng chứng chuyển mẫu | `storage/receipts/` | `receipt_attachments.receipt_id` |
| File kết quả, DOCX tải lên, PDF trước/sau phát hành, phiên bản cũ | `storage/results/` | `result_release_files` → release → biên bản → mẫu chuyển → phiếu tiếp nhận |
| Avatar nhân viên | `storage/avatars/` | Tài khoản, không phải file hồ sơ kiểm nghiệm |
| Yêu cầu, kế hoạch, hiện trường, chỉ tiêu, biên bản/QC… | MySQL | Dữ liệu có cấu trúc; không mặc định tồn tại dưới dạng file riêng |

Docker hiện mount `source/be/storage` trên máy chủ thành `/app/storage`. Vì vậy ảnh tiếp nhận mặc định ở `source/be/storage/receipts` trên máy chủ; chỉ sao chép folder này sẽ thiếu file kết quả và dữ liệu MySQL. Cần giữ liên kết trong DB, không tổ chức hồ sơ chỉ bằng quét tên file.

Nguồn code đã rà: `ReceiptService.attach/loadAttachment`, `ResultReleaseService.store/open`, các entity attachment/release và luồng `TestingRecordService`. File tiếp nhận đang dùng UUID + tên gốc, kết quả dùng UUID. API tải hiện dùng ID, có thể giữ URL ổn định khi đổi cách lưu vật lý.

Hiện root được lấy từ cấu hình khi khởi tạo service; **chưa có chức năng Admin đổi nơi upload lúc đang chạy**. Chỉ thay `storage/receipts` sang một thư mục trống sẽ khiến cách đọc hiện tại tìm sai chỗ các file cũ. Cần bổ sung metadata vị trí từng file và resolver trước khi cho phép chuyển đích ghi.

## 2. Cây hồ sơ đề xuất

Mục navbar **Hồ sơ lưu trữ**, route dự kiến `/admin/dossiers`, chỉ ADMIN.

```text
2026/
  11/
    CFC26.0003/
      01-tiep-nhan/        ảnh mẫu, ảnh lô, scan và file tiếp nhận
      02-ban-giao/         bằng chứng chuyển/nhận mẫu
      03-ket-qua/
        mau-<sample-id>/
          phien-ban-1/     file KQKN và đính kèm đã lưu
          phien-ban-2/
      04-lich-su/          file đã thay thế/gỡ, nếu còn lưu; đánh dấu rõ
      ho-so.json           dữ liệu có cấu trúc và liên kết giữa các phiếu
      danh-muc-file.csv    loại, ID, tên gốc, số byte, checksum, ngày tải
```

- Năm/tháng dự kiến theo **ngày tiếp nhận** (`receivedAt`), múi giờ Việt Nam. Không suy từ `26` trong số phiếu và không đổi folder theo ngày tải bổ sung. Trước khi code cần LAB chốt mốc tính 10 năm: ngày nhận hay ngày đóng hồ sơ; cấu hình lưu một mốc ổn định.
- Số/No. là nhãn tra cứu; receipt ID là khóa kỹ thuật. Hồ sơ cũ có số chứa ký tự không hợp lệ dùng tên folder đã làm sạch kèm ID; `ho-so.json` giữ số gốc, tránh trùng tên sau khi làm sạch.
- Tên file xuất có ID/version để không ghi đè khi hai file trùng tên, kể cả trên Windows không phân biệt hoa/thường.
- Một Số/No. nhiều mẫu vẫn một folder; các mẫu/phiên bản kết quả nằm bên trong.
- Hồ sơ trước tiếp nhận chưa có `receivedAt`: trang này ban đầu chỉ quản lý phiếu đã tiếp nhận. Không tạo giả ngày nhận hoặc ép các yêu cầu chưa tiếp nhận vào tháng bất kỳ.

## 3. Triển khai giao diện trước, thay đổi vật lý sau

Khuyến nghị làm **cây hồ sơ logic + export trước**. Có thể gom được file cũ đang ở hai root mà không phải di chuyển hàng loạt ngay. Màn hình năm/tháng/CFC và cây trong file export vẫn đúng yêu cầu ngay ở bước đầu.

Trang Admin gồm:

- Năm/tháng, tìm Số/No., phân trang phía server; tổng số hồ sơ, số file, dung lượng.
- Chi tiết từng hồ sơ: ảnh xem trước, file tải, phân loại, người tải/ngày tải, checksum và phiên bản hiện hành/cũ; link về phiếu gốc.
- Chọn một hồ sơ, một tháng hoặc cả năm; xem ước tính file/dung lượng trước khi tạo export.
- Tab **Lượt xuất**: đang chờ, chuẩn bị danh mục, đang đóng gói, hoàn tất, có cảnh báo, lỗi, hủy, hết hạn. Có tiến độ file/byte và tải từng phần.
- Tải/xem/export đều kiểm ADMIN ở backend; không mở thư mục tĩnh hoặc đưa file nội bộ vào API khách tra cứu.
- Giai đoạn đầu không có nút xóa hồ sơ/đổi Số CFC tùy ý. Chỉnh phân loại/mô tả sau này cần audit, giữ quyền nghiệp vụ các khâu hiện có.

## 4. Upload theo folder vật lý và đổi ổ lưu trữ

### 4.1. Cấu trúc folder và địa chỉ từng file

Sau khi trang hồ sơ và export ổn định, upload mới có thể ghi chung dưới một root đã đăng ký:

```text
<storage-location-root>/2026/11/CFC26.0003/
  intake/<uuid>.<ext>
  handover/<uuid>.<ext>
  results/<sample-id>/<release-id>/<uuid>.<ext>
```

Ví dụ root đầu tiên là `storage/dossiers`; root tiếp theo có thể nằm trên ổ/NAS khác. Cây năm/tháng/CFC là cách tổ chức hồ sơ, không quyết định hồ sơ phải nằm trên một ổ duy nhất.

- Thêm resolver lưu trữ dùng chung cho các feature thực sự cần. Mỗi file lưu `storage_location_id` và `relative_path`, cùng ID, tên gốc, checksum, số byte và phiên bản. Resolver đọc đúng vị trí của file, không dùng root đang nhận upload mới để tìm mọi file cũ.
- Đăng ký hai vị trí cũ riêng: `LEGACY_RECEIPTS` → root receipts hiện tại, `LEGACY_RESULTS` → root results hiện tại. Migration gán vị trí theo loại file, giữ nguyên tên/đường dẫn tương đối, không di chuyển dữ liệu. Triển khai tương thích metadata cũ trong thời gian chuyển đổi.
- Danh mục `storage_locations` quản lý ID ổn định, nhãn, root trên máy chủ, trạng thái cho đọc/ghi, định danh volume và kết quả kiểm tra. Cấu hình đích ghi hiện hành tách riêng; một đích có thể nhận cả ảnh tiếp nhận/bàn giao và file kết quả mới. File cũ vẫn giữ vị trí riêng.
- Giữ khóa ID/URL tải và quyền hiện có. Root được đăng ký trong vùng máy chủ cho phép; xác thực đường dẫn đã normalize, cấm `..`, symlink vượt root và tên không hợp lệ trên Windows. Không trả root vật lý qua API công khai.
- `result_release_files.storage_name` hiện chỉ 80 ký tự: nếu dùng cho đường dẫn mới phải có migration tăng độ dài phù hợp trước khi ghi. Không âm thầm nhét đường dẫn dài vào cột hiện có. Có thể thêm `relative_path` riêng và giữ storage_name cũ trong giai đoạn chuyển đổi.
- Đường dẫn năm/tháng được xác định/lưu ổn định theo hồ sơ. Upload sau nhiều năm vẫn giữ nhánh năm/tháng/CFC đó **trên đích ghi hiện hành**; không đổi nhãn hồ sơ khi đổi ổ hoặc chỉ sửa mô tả.
- File viết vào tạm trên cùng filesystem đích → kiểm tra định dạng/size/hash → rename nguyên tử → lưu metadata. Nếu ghi file hoặc transaction DB lỗi, trả lỗi và xử lý bù/dọn file mồ côi có đối chiếu DB; không báo upload thành công khi file chưa lưu đủ. Không xóa dựa trên tuổi file đơn thuần.
- Nếu chuyển file cũ: job copy từng file → so checksum → chuyển con trỏ metadata bằng transaction ngắn; chỉ dọn bản cũ trong buổi bảo trì sau kiểm tra backup. Có dry-run, nhật ký, tiếp tục sau restart và rollback. Không dùng `mv` cả folder khi backend còn trỏ tên cũ.
- File KQKN được tái tạo/thu hồi giữ version; không ghi đè PDF đã phát hành. Export nội bộ không thay đổi trạng thái công khai của file.

### 4.2. Admin đổi nơi lưu upload khi ổ gần đầy

Tab **Ổ lưu trữ** trong trang Hồ sơ lưu trữ hiển thị tên ổ, dung lượng trống, lần kiểm tra gần nhất, trạng thái kết nối và đích đang nhận upload. Luồng chuyển:

1. Người vận hành gắn ổ/NAS mới vào máy chủ và cấp quyền cho backend. Với Docker, thêm bind mount/volume rồi cập nhật container có kiểm soát; với Windows, dùng đường dẫn local/UNC mà tài khoản chạy backend truy cập được. Thao tác hệ điều hành này không thể làm bằng việc nhập một đường dẫn bất kỳ trong trình duyệt.
2. Admin đăng ký vị trí trong các vùng đã được người vận hành cho phép, đặt nhãn như “Ổ hồ sơ B”. Kiểm tra đọc/ghi file thử, dung lượng dự phòng, quyền truy cập, định danh volume/mount và đường dẫn an toàn. Chỉ cho kích hoạt khi đạt; không tạo folder thay thế trên ổ hệ thống nếu NAS đang mất mount.
3. Admin chọn **Dùng cho upload mới**. Backend cập nhật con trỏ đích ghi bằng transaction, kiểm tra phiên bản để tránh hai Admin ghi đè lựa chọn của nhau; ghi audit ai đổi, lúc nào, từ đâu sang đâu. Sau khi cơ chế này được triển khai, đổi giữa các đích đã gắn sẵn không cần restart backend.
4. Upload bắt đầu sau khi chuyển dùng ổ mới. Mỗi lượt upload chốt một `storage_location_id` trước khi ghi; lượt đang chạy không đổi ổ giữa chừng. Nếu ổ cũ đã hết chỗ thì lượt đó thất bại rõ ràng, dọn phần tạm và cho tải lại vào đích mới, không tạo metadata thành công hoặc file trùng do retry.
5. Ổ cũ giữ quyền đọc để xem/tải/export hồ sơ, ngừng nhận ghi mới khi hết các lượt đang chạy. Không tự chuyển toàn bộ file và không tự xóa để lấy dung lượng. Không cho xóa đăng ký vị trí còn file tham chiếu; không đổi root của cùng ID sang một ổ trống.

Ví dụ sau 9 năm:

| Vị trí | Đường dẫn ví dụ trên máy chủ Linux | Sau khi chuyển |
|---|---|---|
| Hồ sơ A | `/mnt/lab-a/dossiers` | Giữ file cũ, chỉ đọc |
| Hồ sơ B | `/mnt/lab-b/dossiers` | Nhận upload mới |

Hai đường dẫn là ví dụ, cần mount tương ứng để container nhìn thấy. Một hồ sơ CFC có thể có ảnh cũ ở A và file bổ sung ở B; trang Admin vẫn hiển thị **một hồ sơ**, bản export vẫn gom thành một cây năm/tháng/CFC. Chuyển ngược về A cũng phải qua kiểm tra khả năng ghi, chỉ ảnh hưởng lượt upload tiếp theo.

### 4.3. Cảnh báo và xử lý hết dung lượng

- Cảnh báo sớm theo cả tỷ lệ và số byte trống, có ngưỡng dự phòng cấu hình theo giới hạn file/số lượt ghi đồng thời. Hiển thị trên trang Admin trước khi ổ đầy; không chỉ kiểm tra khi người dùng bấm upload.
- Kiểm tra dung lượng trước và trong khi ghi; xử lý lỗi hết đĩa thực tế vì số liệu dung lượng có thể thay đổi đồng thời. Trả lỗi nghiệp vụ rõ ràng, dự kiến HTTP `507`, để nhân viên biết upload chưa hoàn tất và liên hệ Admin đổi đích. Không tự chuyển sang ổ chưa được chọn.
- Vùng multipart tạm, LibreOffice, export và MySQL cũng cần dung lượng riêng được giám sát. Đổi đích upload không giải quyết trường hợp ổ hệ thống/DB đã đầy; kiểm tra các vùng này trước khi xác nhận hệ thống có thể tiếp tục nhận file.
- Ổ cũ mất kết nối: báo file tạm không truy cập được và cảnh báo Admin, không coi là đã xóa hay âm thầm tìm file cùng tên ở ổ khác. Các file trên ổ còn kết nối vẫn đọc được; export ghi rõ file thiếu theo cơ chế tại mục 5.
- Chuyển đích ghi không làm trống ổ A. Nếu muốn tháo ổ A hoặc giải phóng dung lượng, dùng job copy → kiểm checksum → đổi vị trí từng file → nghiệm thu/backup trước khi dọn bản cũ. Không tháo ổ đang là nguồn duy nhất của hồ sơ phải giữ 10 năm.

### 4.4. Phạm vi và nghiệm thu đổi ổ

- Chỉ ADMIN quản lý vị trí/đổi đích; phân quyền được kiểm tra ở backend và ghi audit. Giữ nguyên quyền nhân viên upload vào các phiếu được phép, không cho nhân viên chọn raw path.
- Backup bao gồm DB, danh mục vị trí và file trên **mọi ổ** còn tham chiếu; thử phục hồi và ánh xạ mount trước khi mở dịch vụ. Không chỉ backup đích đang nhận upload.
- Nghiệm thu: đọc file legacy trước/sau đổi ổ; file mới ghi đúng B; bổ sung file vào hồ sơ cũ và export gộp A/B; đổi đích khi có upload đang chạy; hai Admin đổi đồng thời; retry khi đầy đĩa; mất mount/NAS; không đủ quyền ghi; restart vẫn giữ đích đã chọn; chặn đường dẫn vượt root; không cho gỡ vị trí còn dữ liệu. Thử cả Linux/Docker và Windows khi triển khai môi trường tương ứng.

## 5. Export qua queue, giới hạn RAM và dung lượng đĩa

Queue chỉ giúp đưa việc nặng ra nền; vẫn phải thiết kế đọc/ghi theo luồng. Không dùng `findAll`, đọc cả thư mục vào RAM, `byte[]` của toàn ZIP hoặc `Response.blob()` cho gói lớn.

### Luồng

1. Admin chọn tháng/năm/hồ sơ và phạm vi phiên bản; gửi yêu cầu có requestId chống bấm lặp.
2. Backend kiểm quyền, tính phạm vi thời gian, ước tính số file/byte và quota. Ghi job vào MySQL, trả `202 + jobId` ngay; không đóng ZIP trong request này.
3. Worker claim job bằng lease bền vững trong DB, khởi đầu chỉ **1 job export chạy đồng thời**. Không cần thêm broker mới cho bản đầu; Redis không phải nơi duy nhất giữ trạng thái.
4. Chốt cutoff và manifest từng file/dữ liệu theo batch, keyset pagination (ví dụ 100–200 hàng). File mới tải sau cutoff không tự lọt vào export. Không giữ transaction DB mở trong suốt lượt đóng gói.
5. Mở từng file qua resolver, copy buffer nhỏ sang ZIP trên đĩa, đóng input ngay sau từng file. Dữ liệu JSON/CSV ghi theo batch. Bằng chứng kiểm thử/QC trong DB xuất thành dữ liệu có cấu trúc; không giả định đã có file gốc để sao chép.
6. Dùng ZIP64 và chia thành các ZIP độc lập theo giới hạn byte/số entry (đề xuất ban đầu 2 GB hoặc 5.000 file/phần, lấy ngưỡng đến trước; cấu hình được). Mỗi phần giữ nguyên cây năm/tháng/CFC. Giới hạn entry cần thiết vì metadata ZIP cũng dùng RAM theo số file.
7. Kiểm số lượng/size/checksum, rename `.partial` thành gói hoàn tất. Manifest ghi rõ từng phần, trạng thái và cảnh báo. Không báo thành công đầy đủ khi thiếu file hoặc hash không khớp; lỗi có thể chạy lại sau xử lý, gói có cảnh báo chỉ khi Admin chọn rõ cho phép thiếu.
8. Admin tải từng gói qua endpoint xác thực, response streaming/range. Hủy job dừng ở ranh giới file/buffer, dọn đúng file tạm của job. Restart phục hồi lease và đóng lại phần chưa hoàn tất; không nối tiếp mù ZIP đang dở.

### Guardrails vận hành

- Kiểm free disk trước và trong job; quota theo Admin/toàn máy, giới hạn số job chờ, không để ZIP làm đầy ổ chứa hồ sơ gốc. Nên cấu hình đĩa export riêng trên NAS khi dữ liệu lớn.
- Export tạm nằm `storage/exports/<job-id>/`, không đặt chung như file hồ sơ 10 năm. TTL đề xuất 7 ngày; trạng thái EXPIRED, dọn có kiểm tra lease và lượt tải đang chạy. TTL này chỉ áp dụng bản export tạm.
- Tải lại không đóng gói lại. Idempotency cùng requestId trả cùng job; job mới là snapshot mới. Hash khác hoặc file bị sửa trong khi đóng gói phải dừng/cảnh báo.
- Tên gói/entry đã sanitize, có ID để tránh Zip Slip/đường dẫn trùng. CSV tránh công thức khi mở bằng Excel; JSON/CSV của hồ sơ không chứa token, mật khẩu hoặc session.
- Tất cả phiên bản/file đã gỡ còn lưu được đánh dấu trong nhánh lịch sử, không lẫn với KQKN có hiệu lực. Avatar và file vận hành/secrets không thuộc export hồ sơ.
- Ghi audit tạo/hủy/tải export và hạn dùng; API trạng thái có phân trang, không trả danh sách tất cả file của một năm trong một response.

## 6. Chọn đường dẫn lưu — máy người dùng và máy chủ

Hai vị trí này khác nhau:

**Máy Admin:** bản đầu xuất ZIP giữ sẵn cây folder; tải file theo trình duyệt và giải nén vào nơi chọn. Có thể bật “hỏi nơi lưu mỗi lần tải”. Không cho backend nhận một chuỗi `D:\...` rồi coi đó là ổ trên máy Admin.

Khi có HTTPS, có thể bổ sung nút **Chọn nơi lưu** dùng `showSaveFilePicker()` để stream ZIP xuống file, hoặc **Xuất thẳng ra thư mục** bằng `showDirectoryPicker()` và tải tuần tự/đồng thời có giới hạn. Đây là bước bổ sung có kiểm tra trình duyệt; cần người dùng bấm và cấp quyền, không tự ghi vào ổ tùy ý. Hai API chưa hỗ trợ đồng đều, cần secure context; địa chỉ HTTP IP LAN hiện tại chưa đáp ứng. Nguồn: [MDN Save picker](https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker), [MDN Directory picker](https://developer.mozilla.org/en-US/docs/Web/API/Window/showDirectoryPicker).

**Máy chủ/NAS:** nếu cần xuất định kỳ vào ổ máy chủ, Admin chọn **đích lưu đã cấu hình** (destinationId), ví dụ “NAS lưu trữ LAB”; backend map tới volume/đường dẫn allowlist. Không nhận đường dẫn filesystem tùy ý từ form. Docker phải mount đích đó; máy chủ Linux không tự thấy ổ D của máy Windows khác. Bước này cần tên đích, dung lượng và quyền ghi thực tế từ LAB trước triển khai.

**Nơi lưu upload gốc:** quản lý theo mục 4, có đích ghi hiện hành và vị trí riêng của từng file. Chọn nơi xuất ZIP không thay đổi nơi upload; chuyển nơi upload không chuyển các bản export hoặc file cũ.

Khuyến nghị: ZIP chia phần + tải chuẩn trước, direct folder trên máy Admin sau khi có HTTPS; xuất NAS là tùy chọn vận hành riêng.

## 7. Lưu 10 năm

- “10 năm” là yêu cầu sản phẩm do người dùng đưa ra, không phải kết luận về nghĩa vụ pháp lý. LAB cần chốt mốc bắt đầu và ngoại lệ hồ sơ đang tranh chấp/chưa đóng.
- Mỗi hồ sơ có `retention_until`/mốc giữ ổn định sau khi chốt nghiệp vụ; mặc định giai đoạn đầu **không tự xóa sau 10 năm**. Đến hạn chỉ đưa vào danh sách Admin xem xét; yêu cầu xóa cần quy trình riêng, audit và kiểm tra các bản backup.
- Chỉ tổ chức folder chưa đủ để giữ hồ sơ 10 năm. Cần backup DB + file + manifest/checksum, bản sao ở thiết bị khác và thử phục hồi định kỳ. ZIP export là bản trao đổi/lưu trữ, không thay thế backup hệ thống.
- Ước tính dung lượng: số phiếu/ngày × số file/phiếu × dung lượng trung bình × ngày/năm × 10, cộng phiên bản, bản sao dự phòng và vùng export tạm. Đo thống kê từ DB trước khi chọn ổ/NAS; chưa có số liệu thì không cam kết sức chứa.
- Có thể chia hot/cold storage theo tuổi hồ sơ sau này; trang Admin/resolver vẫn mở bằng ID, không bắt nhân viên nhớ file đã chuyển sang đĩa nào.

## 8. Các bước triển khai và nghiệm thu

| Bước | Kết quả | Điều kiện đạt |
|---|---|---|
| A0 — Rà metadata/dung lượng | Báo cáo file thiếu, hash sai, số phiếu/tháng, số byte; xác nhận mốc năm/tháng/retention | Chạy read-only, không sửa/move file |
| A1 — Trang Admin Hồ sơ lưu trữ | Navbar riêng, cây logic, tìm/phân trang, gallery/danh sách phiên bản | ADMIN 200, hai role khác 403; gom đúng file cũ receipts/results |
| A2 — Queue export | Job bền vững, ZIP chia phần, progress/hủy/tải/TTL và audit | Thử dữ liệu lớn, giới hạn RAM, hai worker không nhận trùng, phục hồi restart, ổ gần đầy |
| A3.1 — Quản lý nhiều ổ lưu trữ | Metadata vị trí từng file, resolver legacy/nhiều root, tab Admin kiểm tra và đổi đích upload đang chạy | Đọc file cũ sau chuyển; upload mới đúng ổ; restart/concurrency/đầy đĩa/mất mount xử lý đúng; không đổi URL/quyền |
| A3.2 — Upload phân folder | Upload mới đúng năm/tháng/CFC dưới đích hiện hành | Một hồ sơ trải trên hai ổ vẫn xem/export đầy đủ; file mới khớp hash; Word/PDF/QR giữ hành vi |
| A4 — Di chuyển cũ và lưu dài hạn | Copy/verify/switch theo batch, backup/restore, đích NAS nếu có | Dry-run và nghiệm thu trước dọn bản cũ; kiểm Windows/Linux |
| A5 — Chọn folder trực tiếp | File picker/streaming nếu môi trường hỗ trợ | HTTPS, thử trình duyệt thực tế, từ chối quyền/hủy tải không làm mất file gốc |

API dự kiến (chưa tạo): `GET /admin/dossiers`, `GET /admin/dossiers/{receiptId}`, `POST /admin/dossier-exports`, `GET /admin/dossier-exports/{jobId}`, endpoint hủy/tải phần. Job, manifest và audit thuộc feature mới có phạm vi Admin; không đổi quyền tải file nghiệp vụ hoặc API public hiện có.

API quản lý upload dự kiến (chưa tạo): `GET/POST /admin/storage-locations`, `POST /admin/storage-locations/{id}/validate`, `PUT /admin/storage-write-target` nhận ID vị trí đã kiểm tra và phiên bản cấu hình. Ghi audit, không nhận raw path khi chuyển đích. Việc đăng ký root phải đối chiếu vùng máy chủ cho phép; không cho xóa/đổi root của vị trí còn file tham chiếu. Dung lượng/kết nối được kiểm tra lại lúc ghi, không chỉ dựa trên lần validate trước.

Trước khi code A0–A2 cần chốt: mốc tính 10 năm; xuất mọi phiên bản hay có tùy chọn hiện hành/lịch sử; lượng dữ liệu dự kiến; nhu cầu ZIP trên máy Admin hay thêm NAS. Mặc định đề xuất: năm/tháng theo ngày nhận, giữ cả lịch sử có đánh dấu, ZIP chia phần, chưa tự xóa. Chưa cần di chuyển file cũ để bắt đầu A1–A2.
