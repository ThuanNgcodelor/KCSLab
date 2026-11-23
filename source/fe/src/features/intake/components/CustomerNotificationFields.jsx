import { useEffect, useState } from 'react';
import { getCustomerNotificationSettings } from '../api/intakeApi';
import './customerNotifications.css';

export function CustomerNotificationFields({ value, defaultPhone, onChange }) {
  const [settings, setSettings] = useState(null);
  const [error, setError] = useState('');
  const [reload, setReload] = useState(0);
  useEffect(() => {
    let active = true;
    setError('');
    getCustomerNotificationSettings().then((next) => { if (active) setSettings(next); })
      .catch(() => { if (active) setError('Chưa kiểm tra được tính năng nhận tin. Bạn vẫn có thể lưu phiếu.'); });
    return () => { active = false; };
  }, [reload]);

  const ready = Boolean(settings?.ready);
  const simulated = settings?.mode === 'FAKE';
  return <section className="panel-card intake-form-card customer-notification-panel" aria-labelledby="customer-notification-heading">
    <h2 id="customer-notification-heading">Thông báo tiếp nhận qua Zalo</h2>
    {!settings && !error && <p role="status">Đang kiểm tra tính năng nhận tin…</p>}
    {error && <p role="alert">{error} <button type="button" className="link-button" onClick={() => setReload((current) => current + 1)}>Thử lại</button></p>}
    {settings && <p className="field-help">{simulated
      ? 'Đang mô phỏng để kiểm thử. Không gửi tin đến Zalo.'
      : ready ? settings.mode === 'ZALO_LIVE' ? 'Gửi xác nhận cho từng phiếu khi khách đồng ý nhận tin.' : 'Đang thử nghiệm. Chỉ gửi tới số điện thoại test đã được cho phép.'
        : 'Tính năng gửi Zalo chưa sẵn sàng. Phiếu tiếp nhận vẫn được lưu bình thường.'}</p>}
    <label className="customer-notification-check">
      <input type="checkbox" checked={value.enabled} disabled={!ready || Boolean(error)}
        onChange={(event) => onChange({ ...value, enabled: event.target.checked, consentAccepted: false })} />
      <span>{simulated ? 'Thử thông báo cho phiếu này' : 'Gửi xác nhận tiếp nhận cho phiếu này'}</span>
    </label>
    {value.enabled && <>
      <label>Số điện thoại nhận tin
        <input type="tel" inputMode="tel" autoComplete="tel" maxLength={30} value={value.recipientPhone ?? defaultPhone}
          onChange={(event) => onChange({ ...value, recipientPhone: event.target.value, consentAccepted: false })}
          aria-describedby="notification-phone-help" required />
        <small id="notification-phone-help" className="field-help">Kiểm tra đúng người nhận. Mỗi phiếu được gửi một tin riêng.</small>
      </label>
      <label className="customer-notification-check">
        <input type="checkbox" checked={value.consentAccepted} required
          onChange={(event) => onChange({ ...value, consentAccepted: event.target.checked })} />
        <span>{settings?.consentText || 'Tôi đồng ý nhận xác nhận tiếp nhận qua Zalo tại số đã chọn.'}</span>
      </label>
    </>}
  </section>;
}
