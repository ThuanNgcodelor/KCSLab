import { useState } from 'react';

export function UserAvatar({ user, className = '', alt = '' }) {
  const [failedUrl, setFailedUrl] = useState(null);
  const avatarUrl = user?.avatarUrl;
  const initial = Array.from((user?.displayName || user?.email || '').trim())[0]?.toLocaleUpperCase('vi') || '?';
  return <span className={`user-avatar ${className}`} aria-hidden={alt ? undefined : 'true'}>
    {avatarUrl && avatarUrl !== failedUrl
      ? <img src={avatarUrl} alt={alt} width="40" height="40" onError={() => setFailedUrl(avatarUrl)} />
      : <span>{initial}</span>}
  </span>;
}
