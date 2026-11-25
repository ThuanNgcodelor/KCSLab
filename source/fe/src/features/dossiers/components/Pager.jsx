export function Pager({ page, total, size, onChange, disabled = false }) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (pages === 1) return null;
  return <div className="dossier-pager">
    <button type="button" className="outline-button" disabled={page === 0 || disabled} onClick={() => onChange(page - 1)}>Trước</button>
    <span>Trang {page + 1} / {pages}</span>
    <button type="button" className="outline-button" disabled={page + 1 >= pages || disabled} onClick={() => onChange(page + 1)}>Sau</button>
  </div>;
}
