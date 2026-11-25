// One source of truth for status wording and colour across every page.
// Tones: neutral (draft/not started), info (in progress), success (done/OK), warning (waiting for someone), danger (rejected/blocked).
const DOMAINS = {
  receipt: {
    DRAFT: ['Bản nháp', 'neutral'], RECEIVED: ['Đã tiếp nhận', 'info'], ACCEPTED_WITH_CONDITION: ['Có điều kiện', 'warning'],
    REJECTED: ['Từ chối', 'danger'], TRANSFER_PENDING: ['Chờ KCS nhận', 'warning'], TRANSFERRED: ['Đã chuyển KCS', 'success'],
    IN_TESTING: ['Đang thử nghiệm', 'info'], AWAITING_REVIEW: ['Chờ kiểm tra', 'warning'], TEST_REVIEWED: ['Đã kiểm tra', 'success'],
  },
  transfer: {
    DRAFT: ['Bản nháp', 'neutral'], PENDING: ['Chờ nhận', 'warning'], ACCEPTED: ['Đã nhận', 'success'], REJECTED: ['Đã từ chối', 'danger'],
  },
  testing: {
    DRAFT: ['Bản nháp', 'neutral'], SUBMITTED: ['Chờ kiểm tra', 'warning'], REVIEWED: ['Đã kiểm tra', 'success'], RETURNED: ['Cần bổ sung', 'danger'],
  },
  release: {
    DRAFT: ['Bản nháp', 'neutral'], SUBMITTED: ['Chờ Admin phát hành', 'warning'], RETURNED: ['Đã trả lại', 'danger'], PUBLISHED: ['Đã phát hành', 'success'], REVOKED: ['Đã thu hồi', 'danger'],
  },
  coding: {
    PENDING_CODING: ['Chờ mã hóa', 'neutral'], PENDING_VERIFICATION: ['Chờ đối chiếu', 'warning'], VERIFIED: ['Đã mã hóa', 'success'], CORRECTION_REQUIRED: ['Cần hiệu chỉnh', 'danger'],
  },
  testRequest: { DRAFT: ['Bản nháp', 'neutral'], COMPLETED: ['Đã hoàn tất', 'success'] },
  dossierExport: {
    QUEUED: ['Đang chờ', 'neutral'], PREPARING: ['Chuẩn bị danh mục', 'info'], PROCESSING: ['Đang đóng gói', 'info'],
    COMPLETED: ['Hoàn tất', 'success'], COMPLETED_WITH_WARNINGS: ['Hoàn tất, có cảnh báo', 'warning'], FAILED: ['Có lỗi', 'danger'],
    CANCEL_REQUESTED: ['Đang hủy', 'warning'], CANCELLED: ['Đã hủy', 'neutral'], EXPIRED: ['Đã hết hạn', 'neutral'],
  },
  storage: {
    ONLINE: ['Sẵn sàng', 'success'], LOW: ['Gần đầy', 'warning'], EMPTY: ['Chưa có file', 'neutral'],
    VOLUME_CHANGED: ['Cần kiểm tra lại', 'warning'], OFFLINE: ['Chưa truy cập được', 'danger'],
  },
  account: { ACTIVE: ['Đang hoạt động', 'success'], DISABLED: ['Đã khóa', 'danger'] },
};

export function statusMeta(domain, value) {
  const [label, tone] = DOMAINS[domain]?.[value] ?? [value ?? '—', 'neutral'];
  return { label, tone };
}

/** { VALUE: 'Label' } for filters and plain-text usages. */
export function labelsOf(domain) {
  return Object.fromEntries(Object.entries(DOMAINS[domain] ?? {}).map(([value, [label]]) => [value, label]));
}
