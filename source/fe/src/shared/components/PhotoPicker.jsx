import { useEffect, useId, useMemo } from 'react';
import { Icon } from './Icon';

/**
 * Picks photos/files with an explicit camera button (opens the camera on phones) and a gallery/file button.
 * Selected files accumulate, show as thumbnails inside the form, and can be removed one by one.
 */
export function PhotoPicker({ label, hint, files, onChange, accept = 'image/*', camera = true, multiple = true, maxSizeMb = 10 }) {
  const cameraId = useId();
  const galleryId = useId();
  const previews = useMemo(() => files.map((file) => ({
    file, url: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
  })), [files]);
  useEffect(() => () => previews.forEach((item) => item.url && URL.revokeObjectURL(item.url)), [previews]);

  function add(event) {
    const picked = Array.from(event.target.files ?? []);
    event.target.value = ''; // allow choosing the same file again
    if (!picked.length) return;
    onChange(multiple ? [...files, ...picked] : picked.slice(0, 1));
  }
  const tooBig = files.filter((file) => file.size > maxSizeMb * 1024 * 1024);

  return <div className="photo-picker">
    <div className="photo-picker-head"><div><strong>{label}</strong>{hint && <small className="hint">{hint}</small>}</div>{files.length > 0 && <span className="photo-count">{files.length} file</span>}</div>
    <div className="photo-picker-actions">
      {camera && <label htmlFor={cameraId}><Icon name="camera" /> Chụp ảnh
        <input id={cameraId} type="file" accept="image/*" capture="environment" onChange={add} /></label>}
      <label htmlFor={galleryId}><Icon name="image" /> {camera ? 'Chọn từ máy' : 'Chọn file'}
        <input id={galleryId} type="file" accept={accept} multiple={multiple} onChange={add} /></label>
    </div>
    {previews.length > 0 && <div className="photo-grid" role="list" aria-label={`${label} đã chọn`}>
      {previews.map(({ file, url }, index) => <figure className="photo-tile" role="listitem" key={`${file.name}-${file.lastModified}-${index}`}>
        {url ? <img src={url} alt={file.name} /> : <span className="file-chip">{file.name}</span>}
        <figcaption>{file.name}</figcaption>
        <button type="button" aria-label={`Bỏ ${file.name}`} onClick={() => onChange(files.filter((_, position) => position !== index))}>×</button>
      </figure>)}
    </div>}
    {tooBig.length > 0 && <p className="form-error" role="alert">{tooBig.map((file) => file.name).join(', ')} vượt quá {maxSizeMb} MB.</p>}
  </div>;
}
