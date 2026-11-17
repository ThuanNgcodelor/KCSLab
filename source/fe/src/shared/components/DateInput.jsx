import { useEffect, useRef, useState } from 'react';
import { Icon } from './Icon';

/**
 * Date field that always shows dd/mm/yyyy (the native <input type="date"> follows the browser locale, e.g. mm/dd/yyyy).
 * `value` and the emitted `event.target.value` stay ISO yyyy-mm-dd (or '') so callers and the API are unchanged.
 * A calendar button opens the native picker for people who prefer clicking.
 */
export function toDisplay(iso) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
  return match ? `${match[3]}/${match[2]}/${match[1]}` : '';
}

export function toIso(text) {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(text);
  if (!match) return '';
  const [, dd, mm, yyyy] = match;
  const date = new Date(Number(yyyy), Number(mm) - 1, Number(dd));
  const valid = date.getFullYear() === Number(yyyy) && date.getMonth() === Number(mm) - 1 && date.getDate() === Number(dd);
  return valid ? `${yyyy}-${mm}-${dd}` : '';
}

function mask(raw) {
  const digits = raw.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
}

export function DateInput({ value = '', onChange, min, max, required, disabled, className = '', id, name, ...rest }) {
  const [text, setText] = useState(toDisplay(value));
  const picker = useRef(null);
  const field = useRef(null);

  // Follow external changes (reset, presets) without clobbering what the person is typing.
  useEffect(() => { if (toIso(text) !== (value || '')) setText(toDisplay(value)); }, [value]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const iso = toIso(text);
    let message = '';
    if (text && !iso) message = 'Nhập ngày theo dạng dd/mm/yyyy.';
    else if (iso && min && iso < min) message = `Ngày phải từ ${toDisplay(min)} trở đi.`;
    else if (iso && max && iso > max) message = `Ngày phải trước hoặc bằng ${toDisplay(max)}.`;
    field.current?.setCustomValidity(message);
  }, [text, min, max]);

  function emit(next) { onChange?.({ target: { value: next, name } }); }
  function handleText(event) {
    const next = mask(event.target.value);
    setText(next);
    const iso = toIso(next);
    if (iso || next === '') emit(iso);
    else if (value) emit('');
  }
  function openPicker() {
    const input = picker.current;
    if (!input || disabled) return;
    try { input.showPicker(); } catch { input.focus(); input.click(); }
  }

  return <span className={`date-input ${className}`.trim()}>
    <input ref={field} id={id} name={name} type="text" inputMode="numeric" autoComplete="off" placeholder="dd/mm/yyyy" maxLength={10}
      pattern="\d{2}/\d{2}/\d{4}" value={text} onChange={handleText} required={required} disabled={disabled} {...rest} />
    <button type="button" className="date-input-button" onClick={openPicker} disabled={disabled} aria-label="Chọn ngày từ lịch" tabIndex={-1}><Icon name="calendar" size={16} /></button>
    <input ref={picker} className="date-input-native" type="date" tabIndex={-1} aria-hidden="true" min={min} max={max} value={value || ''}
      onChange={(event) => { setText(toDisplay(event.target.value)); emit(event.target.value); }} />
  </span>;
}
