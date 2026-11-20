# Ca kiểm thử CFC Lab từ đầu đến cuối

Dùng môi trường thử và tài khoản Admin, Tiếp nhận, KCS riêng. Lấy số yêu cầu hợp lệ từ màn hình. Ghi lại số đó, mã mẫu nhập tay, số chuyển mẫu, số hồ sơ kết quả và mã tra cứu để đối chiếu. File thử nghiệm phải có thông tin phù hợp mẫu test.

## TC01 · Lấy mẫu hiện trường đến phát hành

| Vai trò | Thao tác | Kết quả cần thấy |
|---|---|---|
| Tiếp nhận | Tạo yêu cầu cho `Khách hàng QA`, mẫu `Sầu riêng QA`, chọn Cadimi và Auramine O rồi hoàn tất | Có một yêu cầu, hai chỉ tiêu |
| Tiếp nhận | Lập kế hoạch, ghi lấy mẫu thực tế, nhập ngày, người lấy và niêm phong mẫu `MS-QA-01` nếu có | Hai biểu mẫu dùng cùng số yêu cầu |
| Tiếp nhận | Tại **Tiếp nhận mẫu**, chọn yêu cầu, đối chiếu dữ liệu được điền sẵn. Nhập mã mẫu `QA-SR-001` hoặc mã mới, seal container `CS-QA-01`, bao bì và tình trạng. Thêm ảnh/file rồi lưu | Thông tin nguồn và hai số niêm phong ở đúng vị trí, mã mẫu có thể chọn khi chuyển |
| Tiếp nhận | Lập **Chuyển mẫu**, kiểm tra số lượng, khối lượng, chỉ tiêu, phương pháp thử và ngày hạn trả. Lưu nháp, kiểm tra chi tiết, gửi bàn giao | Nháp ẩn với KCS. Sau khi gửi, KCS thấy phiếu đang chờ |
| KCS | Mở phiếu từ thông báo. So khớp mã mẫu, tình trạng, yêu cầu và ảnh, xác nhận nhận | Phiếu hiện ở danh sách đã nhận. Tiếp nhận có thông báo, avatar hiển thị nếu KCS đã tải ảnh |
| KCS | **Thử nghiệm → Tạo biên bản**, chế độ **Nhập kết quả**: kiểm tra 10 mục thông tin mẫu tự điền, nhập Đơn vị/Kết quả/Ghi chú, bấm **Tạo biên bản & gửi Admin duyệt** | Mở hồ sơ kết quả trạng thái chờ duyệt. Xem trước PDF và tải Word cùng bố cục mẫu KQKN, có trang ảnh mẫu nếu phiếu tiếp nhận có ảnh |
| Admin (khác người lập) | Mở hồ sơ, xem trước và bấm **Phát hành** | Số phiếu = Số/No. (ví dụ `CFC26.0001`), có mã tra cứu và QR trong ô "Dán/in mã QR" |
| Khách | Tra bằng mã/QR và bốn số cuối điện thoại đúng | Mở và tải được PDF phát hành. Thử sai bốn số và kiểm tra hệ thống chặn |

## TC02 · Giao mẫu trực tiếp, từ chối bàn giao

1. Tiếp nhận lập và hoàn tất yêu cầu giao mẫu trực tiếp, chuyển tới **Tiếp nhận mẫu** rồi lưu mã và ảnh. Dữ liệu khách/mẫu được lấy từ yêu cầu.
2. Lập phiếu chuyển đã gửi với một mẫu. Thử đưa mẫu đang được giữ bởi phiếu đã gửi vào một phiếu khác. Hệ thống phải từ chối.
3. KCS từ chối vì “Niêm phong bị mở”. Kiểm tra trạng thái, lý do và thông báo phản hồi ở tài khoản Tiếp nhận.

## TC03 · Tải DOCX và xử lý lỗi

1. Với mẫu KCS đã nhận, thử tải sai định dạng, quá 10 MB rồi tải DOCX hợp lệ.
2. Hệ thống báo lỗi với file sai và cho thử lại. DOCX hợp lệ được chuyển PDF khi backend có LibreOffice.
3. Đối chiếu tên mẫu, chỉ tiêu, số phiếu và bố cục PDF trước khi phát hành. Thu hồi phiếu vừa phát hành rồi thử tra cứu lại.

## TC03b · Tạo lại PDF và địa chỉ QR

1. Admin mở `/admin/results`, xem khung **Mã QR trên phiếu kết quả** (địa chỉ hiện tại, cảnh báo nếu là IP nội bộ).
2. Bấm **Tạo lại PDF** ở một phiếu đã phát hành, rồi bấm tạo lại hàng loạt. Số phiếu và mã tra cứu không đổi; khách tải về bản mới. Tài khoản KCS không gọi được hai thao tác này.

## TC04 · Quyền, giao diện, thông báo và PWA

1. Lặp ở ba vai trò trên 390px, 1024px, 1440px và 2560px. Kiểm tra thanh điều hướng, hộp thoại, nút, ô ngày, trường tìm kiếm, hàng bảng và menu thông báo. Mở thêm ở 320px hoặc ngang điện thoại.
2. Kiểm tra Admin quản lý được **Nhân viên**, Tiếp nhận có hồ sơ tiếp nhận/chuyển mẫu và KCS nhận mẫu. Dùng một đường dẫn ngoài quyền để kiểm tra UI/API chặn.
3. Đổi avatar, thử hủy xóa bằng bàn phím, rồi xác nhận xóa. Ảnh lỗi hoặc đã xóa chuyển về chữ cái. Tin cũ thiếu thông tin người thực hiện vẫn có biểu tượng.
4. Mở hai tab cùng tài khoản. Tạo một sự kiện bàn giao bằng tài khoản còn lại. Mỗi tab nhận một tin, có số chưa đọc đúng. Đánh dấu đã đọc ở một tab để kiểm tra tab kia cập nhật.
5. Ngắt WebSocket hoặc tắt mạng rồi kết nối lại. Đối chiếu danh sách với API, kiểm tra lỗi đánh dấu đọc và thao tác thử lại.
6. Mở bản HTTPS trên Safari iPhone và Chrome Android, cài lên màn hình chính và đăng nhập. Thử chọn ảnh/chụp. Tắt mạng, tải lại và kiểm tra trang báo mất mạng. Bật mạng và thử lại.
