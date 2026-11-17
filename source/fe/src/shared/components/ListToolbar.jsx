import { useEffect, useState } from 'react';
import { DateInput } from './DateInput';

/**
 * Search, date range, extra selects and sort for server-side lists. The search box is debounced; sorting and
 * filtering are sent to the backend, never applied to the rows already on screen.
 */
export function ListToolbar({ q = '', onQ, searchLabel = 'Tìm kiếm', placeholder = 'Nhập từ khóa…',
  from = '', to = '', onFrom, onTo, dateLabel = 'Ngày',
  sort, sortOptions, onSort, filters = [], onReset, children }) {
  const [text, setText] = useState(q);
  useEffect(() => { setText(q); }, [q]);
  useEffect(() => {
    if (!onQ || text.trim() === q) return undefined;
    const timer = window.setTimeout(() => onQ(text.trim()), 350);
    return () => window.clearTimeout(timer);
  }, [text, q, onQ]);

  return <section className="panel-card list-toolbar" aria-label="Tìm kiếm, lọc và sắp xếp">
    {onQ && <label className="list-toolbar-search"><small>{searchLabel}</small>
      <input type="search" value={text} onChange={(event) => setText(event.target.value)} placeholder={placeholder} /></label>}
    {onFrom && <label><small>{dateLabel} từ</small><DateInput max={to || undefined} value={from} onChange={(event) => onFrom(event.target.value)} /></label>}
    {onTo && <label><small>{dateLabel} đến</small><DateInput min={from || undefined} value={to} onChange={(event) => onTo(event.target.value)} /></label>}
    {filters.map((filter) => <label key={filter.label}><small>{filter.label}</small>
      <select value={filter.value} onChange={(event) => filter.onChange(event.target.value)}>
        {filter.options.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label>)}
    {sortOptions && <label><small>Sắp xếp</small>
      <select value={sort} onChange={(event) => onSort(event.target.value)}>
        {sortOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label>}
    {children}
    {onReset && <button className="filter-reset" type="button" onClick={onReset}>Xóa bộ lọc</button>}
  </section>;
}

/** Page navigation for a PagedResponse from the backend. */
export function Pagination({ data, onPage, loading = false, noun = 'mục' }) {
  if (!data || data.totalElements === 0) return null;
  const first = data.page * data.size + 1;
  const last = Math.min((data.page + 1) * data.size, data.totalElements);
  return <div className="list-pagination">
    <p><strong>{first}–{last}</strong> / {data.totalElements} {noun} · trang {data.page + 1}/{Math.max(1, data.totalPages)}</p>
    <nav aria-label="Phân trang">
      <button className="pagination-button" type="button" disabled={data.first || loading} onClick={() => onPage(data.page - 1)}>← Trước</button>
      <button className="pagination-button" type="button" disabled={data.last || loading} onClick={() => onPage(data.page + 1)}>Sau →</button>
    </nav>
  </div>;
}

export const emptyPage = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };
