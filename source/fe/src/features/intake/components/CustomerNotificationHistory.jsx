import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../auth/hooks/useAuth';
import { getCustomerNotificationDeliveries, retryCustomerNotification } from '../api/intakeApi';
import './customerNotifications.css';

const statusLabels = {
  QUEUED: 'Chờ xử lý', SENDING: 'Đang xử lý', RETRY_WAIT: 'Chờ thử lại',
  ACCEPTED: 'Zalo đã nhận yêu cầu', DELIVERED: 'Đã giao tới thiết bị', SIMULATED: 'Đã mô phỏng · chưa gửi Zalo',
  UNKNOWN: 'Chưa rõ kết quả · cần đối soát', BLOCKED: 'Chưa đủ điều kiện gửi', FAILED_FINAL: 'Gửi chưa thành công', CANCELLED: 'Không gửi',
};
const reasonLabels = {
  OUTSIDE_TEST_ALLOWLIST: 'Số nhận nằm ngoài danh sách thử nghiệm.', CONFIGURATION_INCOMPLETE: 'Cấu hình Zalo chưa đầy đủ.',
  TEST_SCOPE_CHANGED: 'Cấu hình thử nghiệm đã thay đổi. Tin cũ được giữ lại để kiểm tra.',
  TEMPLATE_DATA_INVALID: 'Thông tin phiếu chưa khớp với các trường trong mẫu tin.',
  TEST_CALL_LIMIT: 'Đã hết số lần gửi của đợt thử nghiệm.', ATTEMPT_LIMIT: 'Đã đạt giới hạn thử lại của phiếu.',
  SENDING_DISABLED: 'Tính năng gửi đang tắt.', LEASE_EXPIRED: 'Lần xử lý bị gián đoạn. Cần kiểm tra trước khi gửi tiếp.',
  TRANSPORT_OR_RESPONSE_ERROR: 'Chưa xác định Zalo đã nhận yêu cầu hay chưa. Cần đối soát trước khi gửi tiếp.',
};
const waiting = new Set(['QUEUED', 'SENDING', 'RETRY_WAIT', 'ACCEPTED', 'UNKNOWN']);
const date = (value) => new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' });

function requestId() {
  return Array.from(crypto.getRandomValues(new Uint8Array(16)), (value) => value.toString(16).padStart(2, '0')).join('');
}

export function CustomerNotificationHistory({ receiptId }) {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  const [busyId, setBusyId] = useState(null);
  const [reasons, setReasons] = useState({});
  const retryIds = useRef(new Map());

  useEffect(() => {
    let active = true;
    let timer;
    let polls = 0;
    setLoading(true); setError(''); setItems([]);
    async function load() {
      try {
        const page = await getCustomerNotificationDeliveries(receiptId);
        if (!active) return;
        setItems(page.content ?? []);
        if (page.content?.some((item) => waiting.has(item.status)) && polls++ < 24) timer = window.setTimeout(load, 5000);
      } catch (exception) { if (active) setError(exception.message); }
      finally { if (active) setLoading(false); }
    }
    load();
    return () => { active = false; window.clearTimeout(timer); };
  }, [receiptId, reload]);

  async function retry(item) {
    const reason = (reasons[item.id] || '').trim();
    if (!reason) { setError('Nhập lý do trước khi thử gửi lại.'); return; }
    const key = `${receiptId}:${item.id}`;
    if (!retryIds.current.has(key)) retryIds.current.set(key, requestId());
    setBusyId(item.id); setError('');
    try {
      await retryCustomerNotification(receiptId, item.id, { reason, requestId: retryIds.current.get(key) });
      retryIds.current.delete(key);
      setReload((current) => current + 1);
    } catch (exception) { setError(exception.message); }
    finally { setBusyId(null); }
  }

  return <section className="panel-card intake-form-card customer-notification-panel" aria-label="Lịch sử thông báo tiếp nhận">
    <div className="customer-notification-heading"><h2>Thông báo tiếp nhận</h2>
      <button type="button" className="outline-button" disabled={loading || busyId !== null} onClick={() => setReload((current) => current + 1)}>Làm mới</button></div>
    {loading && <p role="status">Đang tải lịch sử gửi…</p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {!loading && !error && items.length === 0 && <p className="muted">Phiếu này chưa có yêu cầu gửi thông báo.</p>}
    {items.map((item) => <article className="customer-delivery-card" key={item.id}>
      <strong>{item.requestNumber} · {item.recipientMasked}</strong>
      <p role="status">{statusLabels[item.status] || 'Đang cập nhật trạng thái'}</p>
      {item.reasonCode && item.reasonCode !== '0' && item.reasonCode !== 'FAKE' && <p className="field-help">{reasonLabels[item.reasonCode] || 'Admin cần kiểm tra điều kiện gửi và cấu hình Zalo.'}</p>}
      {item.mode === 'FAKE' && <p className="field-help">Kết quả mô phỏng, không có tin gửi đến tài khoản Zalo.</p>}
      {item.status === 'ACCEPTED' && <p className="field-help">Chưa xác nhận người nhận đã nhận hoặc đọc tin.</p>}
      {item.attempts?.length > 0 && <ol className="customer-delivery-attempts">{item.attempts.map((attempt) => <li key={attempt.id}>
        Lần {attempt.number} · {date(attempt.createdAt)} · {statusLabels[attempt.status] || attempt.status}
        {attempt.deliveredAt && <span> · Thiết bị nhận lúc {date(attempt.deliveredAt)}</span>}
      </li>)}</ol>}
      {user?.role === 'ADMIN' && item.status === 'FAILED_FINAL' && !['-139', '-141'].includes(item.reasonCode) && <div className="customer-delivery-retry">
        <label>Lý do thử gửi lại<input maxLength={255} value={reasons[item.id] || ''} onChange={(event) => setReasons((current) => ({ ...current, [item.id]: event.target.value }))} /></label>
        <button type="button" className="outline-button" disabled={busyId !== null || !(reasons[item.id] || '').trim()} onClick={() => retry(item)}>{busyId === item.id ? 'Đang yêu cầu…' : 'Thử gửi lại'}</button>
      </div>}
    </article>)}
  </section>;
}
