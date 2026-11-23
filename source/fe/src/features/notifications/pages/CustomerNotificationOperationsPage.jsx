import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { getCustomerNotificationOverview, listCustomerDeliveries, pauseCustomerNotifications } from '../api/customerNotificationApi';
import './customerNotificationOperations.css';

const statuses = {
  QUEUED: 'Chờ xử lý', SENDING: 'Đang gửi', RETRY_WAIT: 'Chờ thử lại', ACCEPTED: 'Zalo đã nhận yêu cầu',
  DELIVERED: 'Đã giao tới thiết bị', SIMULATED: 'Đã mô phỏng', UNKNOWN: 'Cần đối soát',
  BLOCKED: 'Chưa đủ điều kiện', FAILED_FINAL: 'Gửi chưa thành công', CANCELLED: 'Không gửi',
};
const warnings = {
  TOKEN_NOT_PROVISIONED: 'Chưa cấp token quản lý cho OA.', TOKEN_NEEDS_AUTH: 'Cần cấp quyền hoặc cấp lại cặp token.',
  TOKEN_REJECTED_BY_PROVIDER: 'Zalo từng từ chối token ở một lần gửi. Kiểm tra quyền OA và token trước khi thử lại phiếu lỗi.',
  TOKEN_EXPIRED: 'Access token đã hết hạn.', TOKEN_REFRESHING: 'Đang làm mới token.',
  TOKEN_ENCRYPTION_UNAVAILABLE: 'Không mở được kho token. Kiểm tra khóa mã hóa trên máy chủ.',
  TOKEN_EXPIRING: 'Access token sắp hết hạn. Kiểm tra tiến trình làm mới token.',
  REFRESH_TOKEN_EXPIRING: 'Refresh token sắp hết hạn. Cần kiểm tra quyền truy cập OA.',
  DELIVERY_TRACKING_DISABLED: 'Chưa bật theo dõi trạng thái giao tin.', QUOTA_OR_BALANCE_ERRORS: 'Có lỗi hạn mức hoặc số dư. Kiểm tra trước khi gửi tiếp.',
  UNKNOWN_REQUIRES_RECONCILIATION: 'Có lần gửi chưa rõ kết quả. Đối soát với Zalo trước khi xử lý tiếp.',
  WEBHOOK_REQUIRES_REVIEW: 'Có callback không khớp với lịch sử gửi. Cần kiểm tra cấu hình App/OA.',
};
const date = (value) => value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : 'Chưa có';
const modes = { OFF: 'Đang tắt', FAKE: 'Mô phỏng', ZALO_DEVELOPMENT: 'Zalo thử nghiệm', ZALO_LIVE: 'Zalo gửi thật' };

export function CustomerNotificationOperationsPage() {
  const [overview, setOverview] = useState(null);
  const [results, setResults] = useState(null);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [reload, setReload] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reason, setReason] = useState('');
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const operation = useRef(null);

  useEffect(() => {
    let active = true;
    setLoading(true); setError('');
    Promise.all([getCustomerNotificationOverview(), listCustomerDeliveries({ status, q: query, page })])
      .then(([next, rows]) => { if (active) { setOverview(next); setResults(rows); } })
      .catch((exception) => { if (active) setError(exception.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [status, query, page, reload]);

  async function changePause() {
    if (busy) return;
    const payload = { paused: !overview.paused, reason: reason.trim() };
    const fingerprint = JSON.stringify(payload);
    if (operation.current?.fingerprint !== fingerprint) {
      operation.current = { fingerprint, requestId: Array.from(crypto.getRandomValues(new Uint8Array(16)), (n) => n.toString(16).padStart(2, '0')).join('') };
    }
    setConfirm(false); setBusy(true); setError(''); setNotice('');
    try {
      await pauseCustomerNotifications({ ...payload, requestId: operation.current.requestId });
      operation.current = null;
      setReason(''); setNotice(payload.paused ? 'Đã tạm dừng lấy tin mới. Lần gửi đang chạy có thể vẫn hoàn tất.' : 'Đã cho phép xử lý hàng đợi theo cấu hình hiện tại.');
      setReload((n) => n + 1);
    } catch (exception) { setError(exception.message); }
    finally { setBusy(false); }
  }

  return <section className="admin-page customer-operations">
    <header className="admin-heading"><div><p className="section-kicker">Hệ thống / Thông báo khách hàng</p><h1>Thông báo Zalo</h1>
      <p className="heading-copy">Theo dõi từng phiếu tiếp nhận và xử lý các lần gửi cần kiểm tra.</p></div>
      <button type="button" className="outline-button" disabled={loading || busy} onClick={() => setReload((n) => n + 1)}>Làm mới</button></header>
    {loading && <p role="status">Đang tải trạng thái…</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {notice && <p role="status">{notice}</p>}
    {overview && <>
      <div className="customer-operations-summary">
        <article className="panel-card"><span>Chế độ</span><strong>{modes[overview.mode] || overview.mode}</strong><small>{overview.paused ? 'Đang tạm dừng gửi' : 'Xử lý theo cấu hình'}</small></article>
        <article className="panel-card"><span>Đã giao tới thiết bị</span><strong>{overview.counts.DELIVERED || 0}</strong><small>Không đồng nghĩa đã đọc</small></article>
        <article className="panel-card"><span>Cần đối soát</span><strong>{overview.counts.UNKNOWN || 0}</strong><small>Chưa tự gửi lại</small></article>
        <article className="panel-card"><span>Lượt gọi trong đợt</span><strong>{overview.sessionCalls} / {overview.sessionLimit}</strong><small>Mỗi lần gửi tính một lượt</small></article>
      </div>
      {overview.warnings.length > 0 && <section className="panel-card customer-operations-alerts" aria-label="Cảnh báo vận hành"><h2>Cần kiểm tra</h2>
        <ul>{overview.warnings.map((code) => <li key={code}>{warnings[code] || 'Cần kiểm tra trạng thái dịch vụ Zalo.'}</li>)}</ul></section>}
      <section className="panel-card customer-operations-control"><div><h2>Điều khiển gửi tin</h2>
        <p>Hết hạn access token: {date(overview.token.accessExpiresAt)}. Hết hạn refresh token: {date(overview.token.refreshExpiresAt)}.</p>
        <p>Tạm dừng giữ lại hàng đợi để kiểm tra; bật lại sẽ tiếp tục các tin còn đủ điều kiện.</p></div>
        <form onSubmit={(event) => { event.preventDefault(); setConfirm(true); }}>
          <label htmlFor="customer-pause-reason">Lý do {overview.paused ? 'tiếp tục' : 'tạm dừng'}<input id="customer-pause-reason" maxLength={255} required value={reason} disabled={busy} onChange={(event) => setReason(event.target.value)} /></label>
          <button type="submit" className="outline-button" disabled={loading || busy || !reason.trim()}>{busy ? 'Đang cập nhật…' : overview.paused ? 'Tiếp tục xử lý' : 'Tạm dừng gửi'}</button>
        </form></section>
    </>}
    <section className="panel-card customer-operations-list" aria-label="Lịch sử thông báo khách hàng">
      <h2>Lịch sử theo phiếu</h2>
      <form className="customer-operations-filters" onSubmit={(event) => { event.preventDefault(); setQuery(search); setPage(0); }}>
        <label htmlFor="customer-delivery-search">Tìm Số/No.<input id="customer-delivery-search" type="search" maxLength={80} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Nhập số phiếu…" /></label>
        <label htmlFor="customer-delivery-status">Trạng thái<select id="customer-delivery-status" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}><option value="">Tất cả trạng thái</option>{Object.entries(statuses).map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select></label>
        <button type="submit" className="outline-button" disabled={loading}>Tìm kiếm</button>
      </form>
      {!loading && !error && results?.content.length === 0 && <p>Chưa có thông báo phù hợp.</p>}
      {!loading && !error && results?.content.map((item) => <article className="customer-operations-row" key={item.id}>
        <div><Link to={`/admin/intake/${item.receiptId}`}>{item.requestNumber}</Link><small>{item.recipientMasked} · {date(item.createdAt)}</small></div>
        <div><strong>{statuses[item.status] || item.status}</strong><small>{modes[item.mode] || item.mode} · {item.attempts} lần xử lý{item.reasonCode ? ` · Mã: ${item.reasonCode}` : ''}</small></div>
        <Link to={`/admin/intake/${item.receiptId}`}>Xem phiếu</Link>
      </article>)}
      {results && results.totalPages > 1 && <nav className="notification-pagination" aria-label="Phân trang thông báo khách hàng">
        <button className="pagination-button" disabled={loading || page === 0} onClick={() => setPage((n) => n - 1)}>← Trước</button><span>Trang {page + 1}/{results.totalPages} · {results.totalElements} thông báo</span>
        <button className="pagination-button" disabled={loading || page + 1 >= results.totalPages} onClick={() => setPage((n) => n + 1)}>Sau →</button></nav>}
    </section>
    {confirm && <ConfirmDialog title={overview.paused ? 'Tiếp tục xử lý thông báo?' : 'Tạm dừng gửi thông báo?'}
      description={overview.paused ? 'Các tin trong hàng đợi sẽ tiếp tục được kiểm tra và gửi theo cấu hình hiện tại.' : 'Hệ thống ngừng lấy tin mới. Các lần gửi đã bắt đầu có thể vẫn hoàn tất.'}
      confirmLabel={overview.paused ? 'Tiếp tục xử lý' : 'Tạm dừng gửi'} onCancel={() => setConfirm(false)} onConfirm={changePause} />}
  </section>;
}
