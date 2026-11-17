import { statusMeta } from '../status';

/** The only status pill in the app: same shape, dot, size and colours everywhere. */
export function StatusBadge({ domain, value, size = 'normal', className = '' }) {
  const { label, tone } = statusMeta(domain, value);
  return <span className={`status-badge status-badge--${tone} ${size === 'large' ? 'status-badge--large' : ''} ${className}`.trim()}>{label}</span>;
}
