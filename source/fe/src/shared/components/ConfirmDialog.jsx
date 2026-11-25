import { useEffect, useId, useRef } from 'react';
import { Icon } from './Icon';

/** A shared, accessible confirmation dialog for actions across the application. */
export function ConfirmDialog({ title, description, confirmLabel = 'Xác nhận', cancelLabel = 'Quay lại',
  tone = 'default', confirmDisabled = false, children, onConfirm, onCancel }) {
  const dialog = useRef(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const element = dialog.current;
    if (element && !element.open) element.showModal();
    return () => { if (element?.open) element.close(); };
  }, []);

  return <dialog ref={dialog} className={`cfc-confirm cfc-confirm--${tone}`}
    aria-labelledby={titleId} aria-describedby={descriptionId}
    onCancel={(event) => { event.preventDefault(); onCancel(); }}>
    <div className="cfc-confirm__mark" aria-hidden="true">
      <Icon name={tone === 'danger' ? 'alert' : 'check'} size={20} />
    </div>
    <div className="cfc-confirm__content">
      <p className="cfc-confirm__eyebrow">CFC LAB · XÁC NHẬN THAO TÁC</p>
      <h2 id={titleId}>{title}</h2>
      <p id={descriptionId} className="cfc-confirm__description">{description}</p>
      {children}
    </div>
    <div className="cfc-confirm__actions">
      <button type="button" className="cfc-confirm__cancel" autoFocus onClick={onCancel}>{cancelLabel}</button>
      <button type="button" className="cfc-confirm__submit" disabled={confirmDisabled} onClick={onConfirm}>{confirmLabel}</button>
    </div>
  </dialog>;
}
