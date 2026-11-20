export function WorkspaceStats({ label, stats, loading = false }) {
  return (
    <section className="stats-grid workspace-stats" aria-label={label} aria-busy={loading}>
      {stats.map((stat) => (
        <article className={`stat-card ${stat.tone}`} key={stat.label}>
          <div className="stat-label">{stat.label}<span className="stat-dot" aria-hidden="true" /></div>
          <strong>{loading ? '…' : stat.value ?? '—'}</strong>
          <span className="stat-detail">{stat.detail}</span>
        </article>
      ))}
    </section>
  );
}
