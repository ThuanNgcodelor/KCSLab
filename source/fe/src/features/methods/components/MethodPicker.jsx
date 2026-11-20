import { useEffect, useId, useState } from 'react';
import { listTestMethods } from '../api/testMethodsApi';

const PAGE_SIZE = 30;

/** Phương pháp thử của một mẫu được lưu thành chuỗi "CFC-TN-16-2026, CFC-TN-17-2026". */
export function splitMethodCodes(value) {
  return [...new Set((value || '').split(/[,;\n]/).map((item) => item.trim()).filter(Boolean))];
}

/**
 * Checklist phương pháp thử lấy từ danh mục (chỉ mục đang dùng). Danh mục có thể vài trăm mục nên luôn tìm
 * trên máy chủ, hiện tối đa 30 kết quả trong vùng cuộn; mục đã chọn hiện thành thẻ phía trên để không bị lạc.
 * Mã cũ không còn trong danh mục vẫn được giữ và hiện thành thẻ.
 */
export function MethodPicker({ value, onChange, required = false }) {
  const selected = splitMethodCodes(value);
  const [query, setQuery] = useState('');
  const [result, setResult] = useState({ content: [], totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const listId = useId();

  useEffect(() => {
    let active = true;
    setLoading(true);
    const timer = window.setTimeout(() => {
      listTestMethods({ q: query.trim(), active: true, size: PAGE_SIZE, sort: 'code,asc' })
        .then((page) => { if (active) { setResult(page); setError(''); } })
        .catch((exception) => { if (active) setError(exception.message); })
        .finally(() => { if (active) setLoading(false); });
    }, 250);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query]);

  const toggle = (code) => onChange((selected.includes(code) ? selected.filter((item) => item !== code) : [...selected, code]).join(', '));

  return <div className="method-picker">
    <div className="method-picker-chips" aria-live="polite">
      {selected.length === 0 ? <span className="method-picker-empty">Chưa chọn phương pháp</span>
        : selected.map((code) => <span className="method-chip" key={code}>{code}<button type="button" aria-label={`Bỏ ${code}`} onClick={() => toggle(code)}>×</button></span>)}
    </div>
    <input type="search" className="method-picker-search" value={query} onChange={(event) => setQuery(event.target.value)}
      placeholder="Tìm mã hoặc tên phương pháp…" aria-controls={listId} />
    {/* Ô ẩn để form không gửi được khi bắt buộc mà chưa chọn. */}
    {required && <input className="method-picker-required" tabIndex={-1} aria-hidden="true" value={selected.join(', ')} onChange={() => {}} required />}
    <div className="method-picker-list" id={listId} role="group" aria-label="Danh mục phương pháp thử">
      {error ? <p className="form-error">{error}</p>
        : loading && !result.content.length ? <p className="method-picker-note">Đang tải danh mục…</p>
          : result.content.length === 0 ? <p className="method-picker-note">Không có phương pháp phù hợp{query ? ` với “${query}”` : ''}.</p>
            : result.content.map((method) => <label className={`method-option ${selected.includes(method.code) ? 'checked' : ''}`} key={method.id}>
              <input type="checkbox" checked={selected.includes(method.code)} onChange={() => toggle(method.code)} />
              <span><strong>{method.code}</strong>{method.name && <small>{method.name}</small>}</span>
            </label>)}
    </div>
    {result.totalElements > result.content.length && <p className="method-picker-note">Đang hiện {result.content.length}/{result.totalElements} phương pháp. Gõ để tìm thêm.</p>}
  </div>;
}
