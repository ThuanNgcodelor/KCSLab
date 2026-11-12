<!-- CODEGRAPH_START -->
## CodeGraph

Nếu repository có thư mục `.codegraph/`, hãy dùng CodeGraph trước khi tìm kiếm hoặc đọc nhiều file để định vị symbol và luồng gọi.
<!-- CODEGRAPH_END -->

# Hướng dẫn cho AI Agent

- Giai đoạn 0 đã hoàn tất; hiện triển khai theo phạm vi được người dùng yêu cầu. Chỉ tạo code trong module liên quan, không mở rộng sang nghiệp vụ ngoài phạm vi.
- Khi bắt đầu phiên, đọc `docs/ai/STATE.md` và `plans/CURRENT.md`.
- Chỉ đọc tài liệu và module liên quan trực tiếp đến nhiệm vụ.
- Kiểm tra cấu trúc hiện hữu trước khi sửa; không tự ý di chuyển, xóa hoặc ghi đè mã nguồn.
- Backend dùng package-by-feature; frontend dùng feature-based khi bắt đầu có mã nguồn.
- Giữ nguyên hành vi, API contract, framework và package manager hiện có nếu chưa được yêu cầu đổi.
- Chỉ dùng skill phù hợp với tác vụ; không tải toàn bộ skill hoặc dependency không cần thiết.
- Sau thay đổi có ý nghĩa, chạy kiểm tra phù hợp và cập nhật Project Memory.
- Không tiết lộ, thay đổi secrets hoặc thực hiện thao tác destructive.
- Báo rõ những gì đã kiểm tra, chưa kiểm tra và bước tiếp theo.

Tài liệu chi tiết:

- Frontend: `source/fe/AGENTS.md`
- Backend: `source/be/AGENTS.md`
- Bản đồ module: `docs/ai/MODULE_MAP.md`
- Quyết định kiến trúc: `docs/ai/DECISIONS.md`
