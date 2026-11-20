# Mã mẫu và định danh hồ sơ

## Luồng trên hệ thống

- **Số yêu cầu kiểm nghiệm** liên kết các tài liệu trong một hồ sơ. Phiếu tiếp nhận, phiếu chuyển và phiếu kết quả vẫn có số nhận diện riêng.
- **Mã mẫu** là mã do nhân viên tiếp nhận nhập cho từng mẫu và ghi lên nhãn. Mã phải duy nhất trong hệ thống. Admin có thể hiệu chỉnh theo quyền. Các định dạng cũ vẫn đọc được trên hồ sơ đã lưu.
- **Số niêm phong mẫu** ghi trên bao bì mẫu. **Seal container** ghi trên container hoặc lô hàng.
- Khi chuyển mẫu, KCS so khớp mã mẫu và tình trạng thực tế với phiếu bàn giao và hồ sơ đính kèm. Xác nhận nhận hoặc từ chối kèm lý do. Luồng trên ứng dụng đã bỏ bước xác minh mã hai người.
- **Số phiếu kết quả và mã tra cứu** do hồ sơ kết quả quản lý. Giữ hai giá trị này tách khỏi mã mẫu và số yêu cầu.

## Tài liệu nghiệp vụ

[CFC-TN-14](<template/CFC14Quy trình mã hóa mẫu trái cây.doc>) mô tả mã có thứ tự nguồn, sản phẩm, ngày và số thứ tự cùng quy tắc cho mẫu QC, mã mù và mẫu tách phần. Đây là tài liệu nghiệp vụ để đối chiếu. Khi LAB sửa quy trình giấy, cần chốt cách đồng bộ với quy trình nhập mã thủ công và bàn giao đang chạy.
