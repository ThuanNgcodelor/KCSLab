import { useCallback, useEffect, useState } from 'react';
import { createTestMethod, deleteTestMethod, listTestMethods, updateTestMethod } from '../api/testMethodsApi';
import { PageLoader } from '../../../shared/components/Loading';
import { ListToolbar, Pagination, emptyPage } from '../../../shared/components/ListToolbar';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { useListState } from '../../../shared/hooks/useListState';
import { Icon } from '../../../shared/components/Icon';

const defaults = { q: '', active: '', sort: 'code,asc' };
const sortOptions = [['code,asc', 'Mã: A → Z'], ['code,desc', 'Mã: Z → A'], ['updatedAt,desc', 'Sửa gần nhất'], ['createdAt,desc', 'Mới thêm nhất']];
const emptyForm = { code: '', name: '', active: true };

/** Danh mục phương pháp thử (ADMIN): thêm, sửa mã/tên (vd. đổi đuôi năm), ngưng dùng, xóa. */
export function TestMethodsPage() {
  const [list, setList, resetList] = useListState(defaults);
  const [data, setData] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [confirm, setConfirm] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await listTestMethods(list)); setError(''); }
    catch (exception) { setError(exception.message); }
    finally { setLoading(false); }
  }, [list]);
  useEffect(() => { load(); }, [load]);

  function startEdit(method) {
    setEditingId(method.id); setForm({ code: method.code, name: method.name ?? '', active: method.active }); setMessage(''); setError('');
  }
  function cancelEdit() { setEditingId(null); setForm(emptyForm); }

  async function save(event) {
    event.preventDefault();
    if (!form.code.trim()) { setError('Vui lòng nhập mã phương pháp.'); return; }
    setSaving(true); setError(''); setMessage('');
    try {
      const payload = { code: form.code.trim(), name: form.name.trim() || null, active: form.active };
      const saved = editingId ? await updateTestMethod(editingId, payload) : await createTestMethod(payload);
      setMessage(editingId ? `Đã cập nhật ${saved.code}.` : `Đã thêm ${saved.code}.`);
      cancelEdit(); await load();
    } catch (exception) { setError(exception.message); }
    finally { setSaving(false); }
  }

  async function run(action, done) {
    setError(''); setMessage('');
    try { await action(); setMessage(done); await load(); } catch (exception) { setError(exception.message); }
  }

  return <section className="staff-page test-methods-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Không gian quản trị / Danh mục</p><h1>Phương pháp thử</h1><p className="heading-copy">Danh mục mã phương pháp dùng khi lập Phiếu chuyển mẫu. Đổi đuôi năm (vd. 2026 → 2027) bằng cách thêm mã mới hoặc sửa mã cũ; phiếu đã lập vẫn giữ mã lúc lập.</p></div></header>
    {message && <p className="success-note toast-note" role="status"><Icon name="checkCircle" size={14} /> {message}</p>}
    {error && <p className="form-error toast-note" role="alert">{error}</p>}

    <form className="panel-card method-form" onSubmit={save}>
      <h2>{editingId ? 'Sửa phương pháp' : 'Thêm phương pháp'}</h2>
      <label>Mã phương pháp<input value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} placeholder="CFC-TN-16-2027" maxLength={80} required /></label>
      <label>Tên / mô tả (không bắt buộc)<input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Ví dụ: Auramine O – LC-MS/MS" maxLength={255} /></label>
      <label className="switch-label"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /><span className="fake-switch" />Đang dùng</label>
      <div className="method-form-actions">
        {editingId && <button type="button" className="outline-button" onClick={cancelEdit}>Hủy</button>}
        <button className="primary-button" disabled={saving}>{saving ? 'Đang lưu…' : editingId ? 'Lưu thay đổi' : '＋ Thêm'}</button>
      </div>
    </form>

    <section className="panel-card">
      <ListToolbar q={list.q} onQ={(q) => setList({ q })} placeholder="Tìm theo mã hoặc tên…" sort={list.sort} sortOptions={sortOptions} onSort={(sort) => setList({ sort })}
        filters={[{ label: 'Trạng thái', value: list.active, onChange: (active) => setList({ active }), options: [['', 'Tất cả'], ['true', 'Đang dùng'], ['false', 'Ngưng dùng']] }]}
        onReset={resetList} />
      {loading ? <PageLoader label="Đang tải danh mục…" rows={3} /> : data.content.length === 0
        ? <div className="empty-intake"><h3>Chưa có phương pháp phù hợp</h3><p>Thử xóa bộ lọc hoặc thêm phương pháp mới.</p></div>
        : <div className="table-wrap"><table className="staff-table method-table"><thead><tr><th>Mã phương pháp</th><th>Tên / mô tả</th><th>Trạng thái</th><th /></tr></thead><tbody>
          {data.content.map((method) => <tr key={method.id} className={editingId === method.id ? 'editing-row' : ''}>
            <td data-label="Mã"><strong>{method.code}</strong></td>
            <td data-label="Tên">{method.name || <span className="muted">—</span>}</td>
            <td data-label="Trạng thái"><span className={`method-status ${method.active ? 'on' : 'off'}`}>{method.active ? 'Đang dùng' : 'Ngưng dùng'}</span></td>
            <td data-label="Thao tác"><div className="method-row-actions">
              <button type="button" className="table-action" onClick={() => startEdit(method)}>Sửa</button>
              <button type="button" className="table-action" onClick={() => run(() => updateTestMethod(method.id, { code: method.code, name: method.name, active: !method.active }), `${method.code}: ${method.active ? 'đã ngưng dùng' : 'đã dùng lại'}.`)}>{method.active ? 'Ngưng' : 'Dùng lại'}</button>
              <button type="button" className="table-action danger-action" onClick={() => setConfirm(method)}>Xóa</button>
            </div></td>
          </tr>)}
        </tbody></table></div>}
      <Pagination data={data} loading={loading} noun="phương pháp" onPage={(page) => setList({ page })} />
    </section>
    {confirm && <ConfirmDialog title={`Xóa ${confirm.code}?`} description="Phiếu chuyển mẫu đã lập vẫn giữ mã này. Nếu chỉ muốn không chọn được nữa, hãy dùng “Ngưng”." confirmLabel="Xóa" tone="danger"
      onCancel={() => setConfirm(null)} onConfirm={() => { const target = confirm; setConfirm(null); run(() => deleteTestMethod(target.id), `Đã xóa ${target.code}.`); }} />}
  </section>;
}
