import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/hooks/useAuth';
import { getAdminDashboard, getReceptionistDashboard, listReceipts } from '../api/intakeApi';

const statusLabels = {
  RECEIVED: 'Đã tiếp nhận', ACCEPTED_WITH_CONDITION: 'Có điều kiện', REJECTED: 'Từ chối',
  TRANSFER_PENDING: 'Chờ KCS nhận', TRANSFERRED: 'Đã chuyển KCS', IN_TESTING: 'Đang thử nghiệm',
  AWAITING_REVIEW: 'Chờ kiểm tra', TEST_REVIEWED: 'Đã kiểm tra', DRAFT: 'Bản nháp',
};

const sortOptions = [
  ['receivedAt,desc', 'Ngày nhận: mới nhất'], ['receivedAt,asc', 'Ngày nhận: cũ nhất'],
  ['receiptCode,asc', 'Mã phiếu: A → Z'], ['receiptCode,desc', 'Mã phiếu: Z → A'],
  ['customerName,asc', 'Khách hàng: A → Z'], ['customerName,desc', 'Khách hàng: Z → A'],
  ['sampleName,asc', 'Tên mẫu: A → Z'], ['status,asc', 'Trạng thái: A → Z'],
  ['expectedDate,asc', 'Hạn trả: gần nhất'],
];

const emptyPage = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };
const emptyDashboard = { totalReceipts: 0, totalSamples: 0, accepted: 0, conditional: 0, rejected: 0, awaitingTransfer: 0, recentReceipts: [] };

export function IntakePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [receiptPage, setReceiptPage] = useState(emptyPage);
  const [dashboard, setDashboard] = useState(emptyDashboard);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState('receivedAt,desc');
  const [searchText, setSearchText] = useState('');
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState({ from: '', to: '', status: '', productType: '' });
  const [refreshKey, setRefreshKey] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const timer = window.setTimeout(() => { setQuery(searchText.trim()); setPage(0); }, 350);
    return () => window.clearTimeout(timer);
  }, [searchText]);

  useEffect(() => {
    let active = true;
    setLoading(true); setError('');
    const dashboardRequest = user?.role === 'ADMIN'
      ? getAdminDashboard({ from: filters.from, to: filters.to })
      : getReceptionistDashboard({ from: filters.from, to: filters.to });
    Promise.all([
      listReceipts({
        page, size: 20, sort, q: query, ...filters,
        receivedBy: user?.role === 'RECEPTIONIST' ? user.id : '',
      }),
      dashboardRequest,
    ])
      .then(([list, summary]) => { if (active) { setReceiptPage(list); setDashboard(summary); } })
      .catch((exception) => { if (active) setError(exception.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [filters, page, query, refreshKey, sort, user?.id, user?.role]);

  const updateFilter = (field) => (event) => {
    setFilters((current) => ({ ...current, [field]: event.target.value }));
    setPage(0);
  };
  const resetFilters = () => { setSearchText(''); setQuery(''); setFilters({ from: '', to: '', status: '', productType: '' }); setPage(0); };
  const receipts = receiptPage.content;
  const firstItem = receiptPage.totalElements === 0 ? 0 : receiptPage.page * receiptPage.size + 1;
  const lastItem = Math.min((receiptPage.page + 1) * receiptPage.size, receiptPage.totalElements);
  const periodLabel = filters.from || filters.to ? 'Trong khoảng đã chọn' : 'Trong 7 ngày gần nhất';

  return <section className="intake-page">
    <header className="admin-heading"><div><p className="section-kicker">Vận hành / Phiếu số 3</p><h1>Tiếp nhận mẫu</h1><p className="heading-copy">Không gian làm việc của {user?.role === 'ADMIN' ? 'quản trị viên' : 'người nhận mẫu'}: ghi nhận hồ sơ, ảnh hiện trường và theo dõi bàn giao.</p></div><button className="primary-button" onClick={() => navigate('/admin/intake/new')}>＋ Tạo phiếu tiếp nhận</button></header>
    {location.state?.message && <p className="success-note intake-toast" role="status">✓ {location.state.message}</p>}
    {error && <p className="form-error intake-toast" role="alert">{error}</p>}

    <div className="intake-guide"><div className="guide-step current"><span>01</span><div><strong>Tiếp nhận</strong><small>Phiếu số 3</small></div></div><div className="guide-line" /><div className="guide-step"><span>02</span><div><strong>Chuyển mẫu</strong><small>Phiếu số 4 → KCS</small></div></div><div className="guide-line" /><div className="guide-step"><span>03</span><div><strong>Thử nghiệm</strong><small>Hồ sơ phân tích</small></div></div></div>

    <section className="intake-command-strip" aria-label="Tổng quan tiếp nhận">
      <article><span>Phiếu tiếp nhận</span><strong>{loading ? '…' : dashboard.totalReceipts}</strong><small>{periodLabel}</small></article>
      <article><span>Số mẫu đã ghi nhận</span><strong>{loading ? '…' : dashboard.totalSamples}</strong><small>Đã gắn mã mẫu</small></article>
      <article><span>Chờ chuyển KCS</span><strong>{loading ? '…' : dashboard.awaitingTransfer}</strong><small>Cần lập Phiếu số 4</small></article>
      <article><span>Có điều kiện / từ chối</span><strong>{loading ? '…' : dashboard.conditional + dashboard.rejected}</strong><small>Cần chú ý hồ sơ</small></article>
    </section>

    <section className="panel-card intake-filter-card">
      <div className="intake-search-field"><span aria-hidden="true">⌕</span><label><small>Tìm nhanh</small><input value={searchText} onChange={(event) => setSearchText(event.target.value)} placeholder="Mã phiếu, mã mẫu, khách hàng, số điện thoại…" /></label></div>
      <label><small>Từ ngày</small><input type="date" max={filters.to || undefined} value={filters.from} onChange={updateFilter('from')} /></label>
      <label><small>Đến ngày</small><input type="date" min={filters.from || undefined} value={filters.to} onChange={updateFilter('to')} /></label>
      <label><small>Trạng thái</small><select value={filters.status} onChange={updateFilter('status')}><option value="">Tất cả trạng thái</option>{Object.entries(statusLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>
      <label><small>Loại sản phẩm</small><input value={filters.productType} onChange={updateFilter('productType')} placeholder="Ví dụ: Sầu riêng" /></label>
      <button className="filter-reset" type="button" onClick={resetFilters}>Xóa bộ lọc</button>
    </section>

    <section className="panel-card intake-list-card"><div className="panel-heading intake-list-heading"><div><p className="section-kicker">Phân trang từ máy chủ · 20 phiếu/trang</p><h2>Danh sách phiếu tiếp nhận</h2></div><div className="intake-list-tools"><label>Sắp xếp<select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}>{sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><button className="outline-button small-button" onClick={() => setRefreshKey((value) => value + 1)}>↻ Làm mới</button></div></div>
      {loading ? <div className="intake-loading"><i /><p>Đang tải phiếu từ hệ thống…</p></div> : receipts.length === 0
        ? <div className="empty-intake"><span>⌕</span><h3>Không tìm thấy phiếu phù hợp</h3><p>Thử xóa bộ lọc hoặc tạo phiếu tiếp nhận mới.</p><button className="outline-button" onClick={resetFilters}>Xóa bộ lọc</button></div>
        : <><div className="table-wrap"><table className="staff-table intake-table"><thead><tr><th>Mã phiếu / khách hàng</th><th>Mẫu</th><th>Người tiếp nhận</th><th>Trạng thái</th><th>Ngày nhận</th><th /></tr></thead><tbody>{receipts.map((receipt) => <tr key={receipt.id}><td data-label="Phiếu"><span className="intake-cell-content"><strong className="receipt-code">{receipt.receiptCode}</strong><small>{receipt.customerName} · {receipt.customerPhone}</small></span></td><td data-label="Mẫu"><span className="intake-cell-content"><strong>{receipt.samples?.length ?? 0} mẫu · {receipt.sampleName}</strong><small>{receipt.samples?.map((sample) => sample.sampleCode).join(', ')}</small></span></td><td data-label="Tiếp nhận"><span className="intake-cell-content"><strong>{receipt.receivedByName}</strong><small>Nhận từ {receipt.deliveredBy}</small></span></td><td data-label="Trạng thái"><span className={`status-pill ${receipt.status === 'REJECTED' ? 'disabled' : 'active'}`}><i />{statusLabels[receipt.status] ?? receipt.status}</span></td><td data-label="Ngày nhận" className="muted-cell">{new Date(receipt.receivedAt).toLocaleDateString('vi-VN')}</td><td data-label="Hồ sơ"><button className="table-action" onClick={() => navigate(`/admin/intake/${receipt.id}`)}>Xem hồ sơ</button></td></tr>)}</tbody></table></div><div className="list-pagination"><p>Hiển thị <strong>{firstItem}–{lastItem}</strong> trong <strong>{receiptPage.totalElements}</strong> phiếu · 20 phiếu/trang</p><nav aria-label="Phân trang phiếu tiếp nhận"><button type="button" className="pagination-button" disabled={receiptPage.first || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}>← Trước</button>{pageWindow(receiptPage.page, receiptPage.totalPages).map((pageNumber) => <button type="button" className={`pagination-button page-number ${pageNumber === receiptPage.page ? 'current' : ''}`} aria-current={pageNumber === receiptPage.page ? 'page' : undefined} key={pageNumber} onClick={() => setPage(pageNumber)}>{pageNumber + 1}</button>)}<button type="button" className="pagination-button" disabled={receiptPage.last || loading} onClick={() => setPage((value) => value + 1)}>Sau →</button></nav></div></>}
    </section>
  </section>;
}

function pageWindow(currentPage, totalPages) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index);
  const start = Math.min(Math.max(currentPage - 2, 0), totalPages - 5);
  return Array.from({ length: 5 }, (_, index) => start + index);
}
