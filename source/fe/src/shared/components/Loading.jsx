import { useEffect, useState, useSyncExternalStore } from 'react';
import { getLoadingSnapshot, subscribeLoading } from '../loading/loadingStore';

const LOGO = '/favicon.png';

/** CFC brand spinner: the stork emblem inside a rotating ring. */
export function CfcLoader({ size = 64, label }) {
  return <span className="cfc-loader" style={{ '--cfc-loader-size': `${size}px` }} role={label ? 'status' : undefined} aria-label={label}>
    <i className="cfc-loader__glow" aria-hidden="true" />
    <i className="cfc-loader__ring" aria-hidden="true" />
    <i className="cfc-loader__ring cfc-loader__ring--reverse" aria-hidden="true" />
    <img src={LOGO} alt="" width={size} height={size} />
  </span>;
}

/** In-page loading block with the brand loader and optional skeleton rows. */
export function PageLoader({ label = 'Đang tải dữ liệu…', rows = 0, compact = false }) {
  return <div className={`cfc-page-loader${compact ? ' cfc-page-loader--compact' : ''}`} role="status" aria-live="polite">
    <CfcLoader size={compact ? 44 : 68} />
    <p className="cfc-page-loader__label">{label}</p>
    {rows > 0 && <div className="cfc-skeleton-list" aria-hidden="true">
      {Array.from({ length: rows }, (_, index) => <span className="cfc-skeleton" key={index} style={{ '--w': `${96 - index * 9}%` }} />)}
    </div>}
  </div>;
}

/** Full-screen branded splash used while the session is being restored. */
export function SplashScreen({ label = 'Đang khởi tạo phiên làm việc…' }) {
  return <div className="cfc-splash" role="status" aria-live="polite">
    <div className="cfc-splash__inner">
      <CfcLoader size={112} />
      <strong>CFC Lab</strong>
      <small>Management system</small>
      <span className="cfc-splash__bar" aria-hidden="true"><i /></span>
      <p>{label}</p>
    </div>
  </div>;
}

/**
 * Global request indicator: a top progress bar after 250 ms, and a "still working" pill
 * after 1.2 s, so fast requests never flicker and slow ones are always visible.
 */
export function GlobalProgress() {
  const { pending } = useSyncExternalStore(subscribeLoading, getLoadingSnapshot, getLoadingSnapshot);
  const [showBar, setShowBar] = useState(false);
  const [showPill, setShowPill] = useState(false);

  useEffect(() => {
    if (pending === 0) {
      const hide = window.setTimeout(() => { setShowBar(false); setShowPill(false); }, 260);
      return () => window.clearTimeout(hide);
    }
    const barTimer = window.setTimeout(() => setShowBar(true), 250);
    const pillTimer = window.setTimeout(() => setShowPill(true), 1200);
    return () => { window.clearTimeout(barTimer); window.clearTimeout(pillTimer); };
  }, [pending]);

  return <>
    <div className={`cfc-topbar${showBar ? ' is-active' : ''}${pending === 0 ? ' is-done' : ''}`} aria-hidden="true"><i /></div>
    {showPill && pending > 0 && <div className="cfc-busy-pill" role="status" aria-live="polite"><CfcLoader size={22} /> Đang xử lý…</div>}
  </>;
}
