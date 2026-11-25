export function bytes(n = 0) {
  if (!n) return '0 B';
  const power = Math.min(4, Math.floor(Math.log(n) / Math.log(1024)));
  return `${(n / 1024 ** power).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} ${['B', 'KB', 'MB', 'GB', 'TB'][power]}`;
}

export const date = (v) => (v ? new Date(v).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—');
export const day = (v) => (v ? new Date(v).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—');
export const count = (n = 0) => n.toLocaleString('vi-VN');

export const runningStatuses = ['QUEUED', 'PREPARING', 'PROCESSING', 'CANCEL_REQUESTED'];
export const readyStatuses = ['COMPLETED', 'COMPLETED_WITH_WARNINGS'];

export const exportErrors = {
  SOURCE_UNAVAILABLE: 'Có file không đọc được (thiếu file hoặc ổ lưu mất kết nối). Kiểm tra mục Ổ lưu trữ rồi tạo lượt mới, hoặc chọn “Cho phép thiếu file”.',
  SOURCE_MODIFIED: 'Có file khác với checksum đã lưu. Cần đối soát với bản sao lưu; chọn “Cho phép thiếu file” nếu chỉ cần danh sách file sai.',
  QUOTA_EXCEEDED: 'Phạm vi xuất vượt giới hạn dung lượng một lượt. Xuất theo từng tháng.',
  DISK_RESERVE: 'Vùng gói xuất tạm không đủ dung lượng trống.',
  EXPORT_ROOT_UNAVAILABLE: 'Vùng gói xuất tạm trên máy chủ chưa truy cập được.',
  INTERRUPTED_OR_LEASE_LOST: 'Lượt xuất bị gián đoạn. Tạo lượt mới.',
  TOO_MANY_ATTEMPTS: 'Lượt xuất dừng giữa chừng 3 lần. Thử phạm vi nhỏ hơn.',
};

export const categories = {
  FORM_3: 'Phiếu tiếp nhận', SAMPLE_PHOTO: 'Ảnh mẫu', LOT_PHOTO: 'Ảnh lô', PACKAGING_PHOTO: 'Ảnh bao bì', SEAL_PHOTO: 'Ảnh niêm phong',
  DAMAGE_PHOTO: 'Ảnh hư hỏng', LABEL_PHOTO: 'Ảnh nhãn', TRANSFER_EVIDENCE: 'Bằng chứng bàn giao', OTHER: 'Tài liệu khác',
  REPORT_SOURCE: 'Nguồn phiếu kết quả', REPORT_PDF: 'PDF kết quả', GENERATED_REPORT: 'PDF đã phát hành', UPLOAD: 'Đính kèm kết quả',
};

export function scopeLabel(scope) {
  const period = scope.receiptId ? `Hồ sơ ${scope.label || `#${scope.receiptId}`}`
    : scope.year ? (scope.month ? `Tháng ${String(scope.month).padStart(2, '0')}/${scope.year}` : `Năm ${scope.year}`) : 'Tất cả các năm';
  return scope.q ? `${period} · “${scope.q}”` : period;
}
