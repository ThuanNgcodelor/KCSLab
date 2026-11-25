import { useState } from 'react';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { Icon } from '../../../shared/components/Icon';
import { PageLoader } from '../../../shared/components/Loading';
import { StatusBadge } from '../../../shared/components/StatusBadge';
import * as api from '../api/dossierApi';
import { bytes, count, date } from '../dossierFormat';

const legacyPaths = { RECEIPTS: 'Thư mục tiếp nhận cũ', RESULTS: 'Thư mục kết quả cũ' };
const statusOf = (h) => (h.status === 'ONLINE' && h.warning ? 'LOW' : h.status);

function Space({ label, free, total }) {
  if (!total) return <span className="muted-cell">—</span>;
  return <span className="intake-cell-content"><strong>Trống {bytes(free)}</strong><meter min="0" max={total} value={Math.max(0, total - free)} aria-label={`Dung lượng đã dùng của ${label}`} /><small>Tổng {bytes(total)}</small></span>;
}

export function StorageTab({ data, loading, error: loadError, onChange, onReload }) {
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [target, setTarget] = useState(null);
  const [form, setForm] = useState({ label: '', path: '' });

  async function action(work, message) {
    if (busy) return false;
    setBusy(true); setError(''); setNotice('');
    try { onChange(await work()); setNotice(message); return true; }
    catch (e) { setError(e.message); return false; } finally { setBusy(false); }
  }

  const legacy = data?.settings.locationId == null;
  return <>
    {notice && <p className="success-note toast-note" role="status"><Icon name="checkCircle" size={14} /> {notice}</p>}
    {(error || loadError) && <p className="form-error toast-note" role="alert">{error || loadError}</p>}
    {!data && loading && <PageLoader label="Đang kiểm tra các ổ lưu trữ…" rows={3} />}
    {data && <>
      <section className="panel-card intake-list-card dossier-group">
        <div className="panel-heading"><div><p className="section-kicker">Nơi lưu file hồ sơ</p><h2>Ổ lưu trữ</h2><p className="dossier-note">Đổi nơi nhận upload khi ổ gần đầy. File cũ vẫn đọc từ ổ đã lưu, vì vậy ổ cũ cần tiếp tục kết nối.</p></div>
          <button type="button" className="outline-button small-button" disabled={busy || loading} onClick={() => { setError(''); setNotice(''); onReload(); }}>↻ Kiểm tra lại</button></div>
        <div className="table-wrap"><table className="staff-table intake-table" aria-label="Ổ lưu trữ"><thead><tr><th scope="col">Ổ lưu</th><th scope="col">Trạng thái</th><th scope="col">Dung lượng</th><th scope="col">Đang lưu</th><th scope="col">Kiểm tra ghi</th><th scope="col"><span className="dossier-hidden">Thao tác</span></th></tr></thead><tbody>{data.locations.map((h) => {
          const { location: l } = h; const receiving = legacy ? Boolean(l.legacyKey) : data.settings.locationId === l.id;
          return <tr key={l.id}>
            <td data-label="Ổ lưu"><span className="intake-cell-content"><strong>{l.label}</strong><small>{l.rootPath || legacyPaths[l.legacyKey]}</small></span></td>
            <td data-label="Trạng thái"><span className="dossier-badges"><StatusBadge domain="storage" value={statusOf(h)} />{receiving && <span className="status-badge status-badge--info">Đang nhận upload</span>}</span>
              {h.status === 'VOLUME_CHANGED' && <small className="dossier-note">Thiết bị đã đổi. File cũ vẫn đọc được; cần kiểm tra lại trước khi nhận upload.</small>}
              {h.status === 'OFFLINE' && h.files > 0 && <small className="dossier-note">File tạm thời chưa mở được, không phải đã bị xóa.</small>}</td>
            <td data-label="Dung lượng"><Space label={l.label} free={h.freeBytes} total={h.totalBytes} /></td>
            <td data-label="Đang lưu"><span className="intake-cell-content"><strong>{count(h.files)} file</strong><small>{bytes(h.bytes)}</small></span></td>
            <td data-label="Kiểm tra ghi" className="muted-cell">{l.legacyKey ? '—' : date(l.validatedAt)}</td>
            <td data-label="Thao tác">{l.legacyKey ? <span className="muted-cell">{receiving ? 'Mặc định' : 'Chỉ đọc'}</span> : <div className="method-row-actions">
              <button type="button" className="table-action" disabled={busy} onClick={() => action(() => api.validateStorage(l.id), 'Đã kiểm tra đọc/ghi và dung lượng.')}>Kiểm tra</button>
              {!receiving && <button type="button" className="table-action" disabled={busy} onClick={() => setTarget(l)}>Dùng cho upload</button>}
            </div>}</td>
          </tr>;
        })}
        {data.areas.map((a) => <tr key={a.label}>
          <td data-label="Vùng"><span className="intake-cell-content"><strong>{a.label}</strong><small>Vùng hệ thống, không đổi theo nơi upload</small></span></td>
          <td data-label="Trạng thái"><StatusBadge domain="storage" value={statusOf(a)} /></td>
          <td data-label="Dung lượng"><Space label={a.label} free={a.freeBytes} total={a.totalBytes} /></td>
          <td data-label="Đang lưu" className="muted-cell">—</td><td data-label="Kiểm tra ghi" className="muted-cell">—</td><td data-label="Thao tác" className="muted-cell">—</td>
        </tr>)}</tbody></table></div>
        <p className="dossier-note">Ổ chứa MySQL cần theo dõi riêng trên máy chủ.</p>
      </section>
      <form className="panel-card dossier-register" onSubmit={async (e) => {
        e.preventDefault();
        if (await action(() => api.registerStorage(form), 'Đã đăng ký và kiểm tra nơi lưu. Bạn có thể chọn dùng cho upload.')) setForm({ label: '', path: '' });
      }}>
        <div className="panel-heading"><div><p className="section-kicker">Sau khi gắn ổ hoặc NAS vào máy chủ</p><h2>Thêm nơi lưu</h2><p className="dossier-note">Thư mục phải có sẵn, ghi được và nằm trong: {data.allowedRoots.join(', ') || 'chưa cấu hình'}. Với Docker, nhập đường dẫn bên trong container.</p></div></div>
        <div className="list-toolbar dossier-register-fields">
          <label><small>Tên hiển thị</small><input required maxLength={120} value={form.label} placeholder="Ổ hồ sơ B" onChange={(e) => setForm({ ...form, label: e.target.value })} /></label>
          <label><small>Đường dẫn trên máy chủ</small><input required maxLength={700} value={form.path} placeholder="/app/storage/dossiers-b" onChange={(e) => setForm({ ...form, path: e.target.value })} /></label>
          <button className="primary-button" disabled={busy}>Đăng ký và kiểm tra</button>
        </div>
      </form>
    </>}
    {target && <ConfirmDialog title={`Chuyển upload mới sang ${target.label}?`} description="Upload bắt đầu sau khi chuyển sẽ lưu ở nơi này. File cũ giữ nguyên vị trí. Ổ cũ cần tiếp tục kết nối để đọc và xuất hồ sơ; lượt upload đang chạy vẫn hoàn tất ở ổ cũ."
      confirmLabel="Chuyển nơi upload" onCancel={() => setTarget(null)}
      onConfirm={() => { const { id } = target; setTarget(null); action(() => api.activateStorage(id, data.settings.version), 'Đã chuyển nơi nhận upload mới. File cũ vẫn giữ nguyên vị trí.'); }} />}
  </>;
}
