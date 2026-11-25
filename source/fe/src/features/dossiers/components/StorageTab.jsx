import { useState } from 'react';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import * as api from '../api/dossierApi';
import { bytes, count, date } from '../dossierFormat';

const health = { ONLINE: 'Sẵn sàng', OFFLINE: 'Chưa truy cập được', VOLUME_CHANGED: 'Cần kiểm tra lại', EMPTY: 'Chưa có file' };
const legacyPaths = { RECEIPTS: 'Thư mục tiếp nhận hiện tại', RESULTS: 'Thư mục kết quả hiện tại' };

function Usage({ label, free, total }) {
  return <><meter min="0" max={total || 1} value={Math.max(0, total - free)} aria-label={`Dung lượng đã dùng của ${label}`} /><p>Trống {bytes(free)} / {bytes(total)}</p></>;
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

  const active = data?.locations.find((h) => h.location.id === data.settings.locationId);
  return <>
    <div className="panel-card dossier-storage-intro">
      <div><p className="section-kicker">Nơi lưu upload mới</p><h2>{active?.location.label || 'Theo thư mục tiếp nhận / kết quả hiện tại'}</h2>
        <p>Đổi nơi ghi file mới khi ổ gần đầy. File cũ vẫn đọc từ ổ đã lưu và xuất chung trong hồ sơ; đổi nơi ghi không làm trống ổ cũ.</p></div>
      <button type="button" className="outline-button" disabled={busy || loading} onClick={() => { setError(''); setNotice(''); onReload(); }}>Kiểm tra lại</button>
    </div>
    {loading && <p role="status">Đang kiểm tra các ổ lưu trữ…</p>}
    {(error || loadError) && <p className="form-error" role="alert">{error || loadError}</p>}
    {notice && <p className="dossier-notice" role="status">{notice}</p>}
    {data && <>
      <div className="dossier-grid">{data.locations.map((h) => {
        const { location: l } = h; const receiving = data.settings.locationId === l.id;
        const warn = h.warning || h.status === 'OFFLINE';
        return <article className="panel-card dossier-card" key={l.id}>
          <div className="dossier-toolbar"><h2>{l.label}</h2><span className={`dossier-badge ${warn ? 'warning' : ''}`}>{h.status === 'ONLINE' && h.warning ? 'Gần đầy' : health[h.status] || h.status}</span></div>
          <p className="dossier-path">{l.rootPath || legacyPaths[l.legacyKey]}</p>
          {h.totalBytes > 0 && <Usage label={l.label} free={h.freeBytes} total={h.totalBytes} />}
          <p>Hồ sơ đang lưu: {count(h.files)} file · {bytes(h.bytes)}</p>
          {h.status === 'VOLUME_CHANGED' && <p>Thiết bị của ổ này đã đổi. File cũ vẫn đọc được; bấm “Kiểm tra đọc/ghi” trước khi nhận upload tiếp.</p>}
          {h.status === 'OFFLINE' && h.files > 0 && <p>File trên ổ này tạm thời chưa mở được, không phải đã bị xóa. Kiểm tra kết nối ổ.</p>}
          {l.legacyKey ? <p className="dossier-muted">Vị trí của file cũ{data.settings.locationId == null ? ', đang nhận upload' : ', chỉ đọc'}.</p>
            : <><small>Kiểm tra ghi gần nhất: {date(l.validatedAt)}</small>
              <div className="dossier-actions">
                <button type="button" className="outline-button" disabled={busy} onClick={() => action(() => api.validateStorage(l.id), 'Đã kiểm tra đọc/ghi và dung lượng.')}>Kiểm tra đọc/ghi</button>
                <button type="button" className="primary-button" disabled={busy || receiving} onClick={() => setTarget(l)}>{receiving ? 'Đang nhận upload' : 'Dùng cho upload mới'}</button>
              </div></>}
        </article>;
      })}</div>
      <section className="panel-card dossier-areas" aria-label="Vùng hệ thống"><h2>Vùng hệ thống cần theo dõi</h2>
        <p className="dossier-muted">Đổi nơi lưu upload không giải phóng các vùng này. Dung lượng ổ chứa MySQL cần theo dõi riêng trên máy chủ.</p>
        <div className="dossier-grid">{data.areas.map((a) => <div key={a.label}><strong>{a.label}</strong> {a.warning && <span className="dossier-badge warning">{a.status === 'ONLINE' ? 'Gần đầy' : 'Chưa truy cập được'}</span>}
          {a.status === 'ONLINE' && <Usage label={a.label} free={a.freeBytes} total={a.totalBytes} />}</div>)}</div>
      </section>
      <form className="panel-card dossier-register" onSubmit={async (e) => {
        e.preventDefault();
        if (await action(() => api.registerStorage(form), 'Đã đăng ký và kiểm tra nơi lưu. Bạn có thể chọn dùng cho upload mới.')) setForm({ label: '', path: '' });
      }}>
        <h2>Thêm nơi lưu</h2><p>Ổ hoặc NAS cần được gắn sẵn vào máy chủ, có quyền đọc/ghi cho ứng dụng. Với Docker, dùng đường dẫn bên trong container đã được mount.</p>
        <p className="dossier-muted">Vùng máy chủ cho phép: {data.allowedRoots.join(', ') || 'chưa cấu hình'}. Thư mục phải có sẵn và nằm riêng với các nơi đã đăng ký.</p>
        <div className="dossier-register-fields">
          <label>Tên hiển thị<input required maxLength={120} value={form.label} placeholder="Ổ hồ sơ B" onChange={(e) => setForm({ ...form, label: e.target.value })} /></label>
          <label>Đường dẫn trên máy chủ<input required maxLength={700} value={form.path} placeholder="/app/storage/dossiers-b" onChange={(e) => setForm({ ...form, path: e.target.value })} /></label>
          <button className="primary-button" disabled={busy}>Đăng ký và kiểm tra</button>
        </div>
      </form>
    </>}
    {target && <ConfirmDialog title={`Chuyển upload mới sang ${target.label}?`} description="Upload bắt đầu sau khi chuyển sẽ lưu ở nơi này. File cũ giữ nguyên vị trí. Ổ cũ cần tiếp tục kết nối để đọc và xuất hồ sơ; lượt upload đang chạy vẫn hoàn tất ở ổ cũ."
      confirmLabel="Chuyển nơi upload" onCancel={() => setTarget(null)}
      onConfirm={() => { const { id } = target; setTarget(null); action(() => api.activateStorage(id, data.settings.version), 'Đã chuyển nơi nhận upload mới. File cũ vẫn giữ nguyên vị trí.'); }} />}
  </>;
}
