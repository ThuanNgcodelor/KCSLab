export function UserAvatar({ user, className = '', alt = '' }) {
  const initial = (user?.displayName || user?.email || 'A').trim().slice(0, 1).toUpperCase();
  return <span className={`user-avatar ${className}`} aria-hidden={alt ? undefined : 'true'}>
    {user?.avatarUrl ? <img src={user.avatarUrl} alt={alt} /> : <span>{initial}</span>}
  </span>;
}
