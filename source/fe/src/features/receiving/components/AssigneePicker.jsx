import { useEffect, useMemo, useState } from 'react';
import { assignReceivingTransfer, listReceivingTechnicians } from '../api/receivingApi';
import { UserAvatar } from '../../profile/components/UserAvatar';

/** Tag one or more technicians on a transfer. People newly tagged are notified by the backend. */
export function AssigneePicker({ transferId, assignees, onSaved }) {
  const [technicians, setTechnicians] = useState([]);
  const [selected, setSelected] = useState(() => assignees.map((person) => person.id));
  const [query, setQuery] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { listReceivingTechnicians().then(setTechnicians).catch((exception) => setError(exception.message)); }, []);
  useEffect(() => { setSelected(assignees.map((person) => person.id)); }, [assignees]);

  const savedKey = assignees.map((person) => person.id).sort().join(',');
  const dirty = [...selected].sort().join(',') !== savedKey;
  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return needle ? technicians.filter((person) => person.displayName.toLowerCase().includes(needle)) : technicians;
  }, [technicians, query]);
  const toggle = (personId) => { setMessage(''); setSelected((current) => (current.includes(personId) ? current.filter((value) => value !== personId) : [...current, personId])); };

  async function save() {
    setSaving(true); setError(''); setMessage('');
    try {
      const saved = await assignReceivingTransfer(transferId, selected);
      onSaved(saved);
      setMessage(saved.length ? `Đã giao cho ${saved.map((person) => person.displayName).join(', ')}. Người mới được tag sẽ nhận thông báo.` : 'Đã bỏ hết người được giao.');
    } catch (exception) { setError(exception.message); } finally { setSaving(false); }
  }

  return <section className="panel-card assignee-card" aria-label="Giao cho kỹ thuật viên thử nghiệm">
    <div className="receiving-section-heading"><div><h2>Giao cho kỹ thuật viên thử nghiệm</h2><p>Chọn một hoặc nhiều kỹ thuật viên sẽ thử nghiệm các mẫu này; họ nhận thông báo ngay.</p></div>
      <span className="receiving-count">{assignees.length} người</span></div>
    <div className="assignee-chips" aria-live="polite">
      {assignees.length === 0 ? <span className="assignee-empty">Chưa giao cho ai</span>
        : assignees.map((person) => <span className="assignee-chip" key={person.id}><UserAvatar user={person} className="assignee-avatar" />{person.displayName}</span>)}
    </div>
    <input type="search" className="assignee-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm kỹ thuật viên…" aria-label="Tìm kỹ thuật viên" />
    <div className="assignee-list" role="group" aria-label="Danh sách kỹ thuật viên">
      {visible.length === 0 ? <p className="assignee-empty">{technicians.length ? 'Không có kỹ thuật viên phù hợp.' : 'Đang tải danh sách…'}</p>
        : visible.map((person) => <label className={`assignee-option ${selected.includes(person.id) ? 'checked' : ''}`} key={person.id}>
          <input type="checkbox" checked={selected.includes(person.id)} onChange={() => toggle(person.id)} />
          <UserAvatar user={person} className="assignee-avatar" /><span>{person.displayName}</span></label>)}
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {message && <p className="success-note" role="status">✓ {message}</p>}
    <div className="assignee-actions">
      <button type="button" className="primary-button" onClick={save} disabled={!dirty || saving}>{saving ? 'Đang lưu…' : 'Lưu người được giao'}</button>
      {dirty && <button type="button" className="outline-button" onClick={() => setSelected(assignees.map((person) => person.id))} disabled={saving}>Hoàn tác</button>}
    </div>
  </section>;
}
