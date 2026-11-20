# Hướng dẫn sử dụng CFC Lab

## Từ yêu cầu đến kết quả

1. **Tiếp nhận** mở **Yêu cầu kiểm nghiệm** rồi tạo phiếu. Điền thông tin khách hàng, mẫu và chỉ tiêu từ hồ sơ giấy. Dùng **Số/No. của yêu cầu** để tra cứu xuyên suốt.
2. Nếu có lấy mẫu tại hiện trường, mở yêu cầu đã lưu, lập **Kế hoạch lấy mẫu**, rồi ghi **Lấy mẫu hiện trường**. Hai biểu mẫu dùng chung số yêu cầu. Với mẫu giao trực tiếp, mở yêu cầu đã hoàn tất và chọn tiếp nhận.
3. Ở **Tiếp nhận mẫu**, tìm số yêu cầu để lấy dữ liệu có sẵn. Đối chiếu thông tin thực tế, nhập mã từng mẫu, chọn chỉ tiêu, ghi tình trạng, niêm phong mẫu và seal container vào hai trường riêng. Tải hồ sơ hoặc dùng **Chụp ảnh** rồi lưu.
4. Mở **Chuyển mẫu**, chọn hồ sơ tiếp nhận và các mẫu cần bàn giao. Kiểm tra khối lượng, số lượng, tình trạng, chỉ tiêu, phương pháp và hạn trả kết quả. Lưu nháp, kiểm tra chi tiết rồi bấm **Gửi bàn giao**.
5. **KCS** mở **Nhận mẫu**, chọn phiếu đang chờ, đối chiếu mẫu thực tế với hồ sơ và ảnh, rồi xác nhận nhận. Nếu có sai lệch, chọn **Từ chối** và ghi lý do.
6. **KCS hoặc Admin** mở **Thử nghiệm → Tạo biên bản**, chọn mẫu đã được KCS nhận.
   - **Nhập kết quả trên hệ thống** (mặc định): thông tin mẫu tự điền từ phiếu tiếp nhận và phiếu chuyển. Chỉ cần nhập **Đơn vị, Kết quả, Ghi chú** cho từng chỉ tiêu, rồi bấm **Tạo biên bản & gửi Admin duyệt** hoặc **Lưu nháp**.
   - **Tải file KQKN có sẵn:** chọn file DOCX/PDF đã soạn.
7. Ở hồ sơ kết quả có **Xem trước PDF** và **Tải file Word (DOCX)**. Hai file có cùng bố cục mẫu KQKN, kèm trang **Hình ảnh mẫu** lấy từ ảnh của phiếu tiếp nhận.
8. **Admin** mở hồ sơ, kiểm tra rồi chọn **Phát hành** (hoặc **Trả lại** kèm lý do). Số phiếu là Số/No. của phiếu tiếp nhận, PDF có QR. Khách dùng mã tra cứu/QR và bốn số cuối điện thoại để tải phiếu đã phát hành.

## Tài khoản và thông báo

- **Thông tin cá nhân** cho phép đổi tên hiển thị, ảnh đại diện và mật khẩu. Sau khi đổi mật khẩu, đăng nhập lại. Quản trị viên cấp tài khoản bằng email và quản lý vai trò, trạng thái tại **Nhân viên**.
- Chuông hiển thị tối đa ba thông báo mới nhất. Trang **Thông báo** có tìm kiếm, lọc và đánh dấu đã đọc. Tin mới có avatar hoặc chữ cái của người thực hiện. Tin cũ thiếu người thực hiện hiện biểu tượng của sự kiện.
- Khi tạm mất kết nối trực tiếp, các bản ghi đã nhận vẫn có thể xem. Hệ thống tự thử kết nối lại và lấy dữ liệu mới qua API khi còn mạng. Nếu mở tin hoặc đánh dấu đã đọc thất bại, thao tác giữ trạng thái hiện tại và hiển thị lỗi để thử lại.

## Cài lên màn hình chính

- **iPhone/iPad.** Mở địa chỉ HTTPS của CFC Lab bằng Safari. Chọn **Chia sẻ → Thêm vào MH chính**. Bật **Mở dưới dạng ứng dụng web** nếu có, rồi chọn **Thêm**.
- **Android.** Mở bằng Chrome. Trong menu ⋮, chọn **Cài đặt ứng dụng** hoặc **Thêm vào màn hình chính**.
- Mở ứng dụng từ biểu tượng vừa thêm. Khi mất mạng và tải lại trang, ứng dụng hiện hướng dẫn thử lại. Lưu biểu mẫu, tải file và xem hồ sơ cần có mạng. Thông báo nội bộ hiện trong ứng dụng đang mở.

Các biểu mẫu gốc nằm trong [template](template/). Xem [ca kiểm thử](TEST_CASES_TOAN_BO_QUY_TRINH.md) để chạy thử bằng dữ liệu giả lập.
