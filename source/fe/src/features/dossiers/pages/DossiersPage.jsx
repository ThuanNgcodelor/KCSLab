import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Icon } from '../../../shared/components/Icon';
import * as api from '../api/dossierApi';
import { CatalogTab } from '../components/CatalogTab';
import { ExportsTab } from '../components/ExportsTab';
import { StorageTab } from '../components/StorageTab';
import './dossiers.css';

const tabs = [['files', 'Hồ sơ'], ['exports', 'Lượt xuất'], ['storage', 'Ổ lưu trữ']];

function storageAlert(data) {
  if (!data) return '';
  const receiving = data.locations.filter((h) => (data.settings.locationId == null ? h.location.legacyKey : h.location.id === data.settings.locationId));
  if (receiving.some((h) => h.status === 'OFFLINE' || h.status === 'VOLUME_CHANGED')) return 'Nơi đang nhận upload chưa sẵn sàng, nhân viên có thể không tải được file.';
  if (receiving.some((h) => h.warning)) return 'Nơi đang nhận upload sắp đầy. Nên chuẩn bị ổ mới và chuyển nơi lưu.';
  if (data.locations.some((h) => h.status === 'OFFLINE' && h.files > 0)) return 'Một ổ đang giữ hồ sơ cũ chưa truy cập được.';
  return data.areas.some((a) => a.warning) ? 'Một vùng hệ thống của máy chủ sắp đầy.' : '';
}

export function DossiersPage() {
  const [search, setSearch] = useSearchParams();
  const tab = tabs.some(([key]) => key === search.get('tab')) ? search.get('tab') : 'files';
  const changeTab = (next) => setSearch(next === 'files' ? {} : { tab: next });

  const [periods, setPeriods] = useState(null);
  useEffect(() => {
    const controller = new AbortController();
    api.getPeriods(controller.signal).then(setPeriods).catch((e) => { if (e.name !== 'AbortError') setPeriods([]); });
    return () => controller.abort();
  }, []);

  // Loaded for every tab: a disk that is filling up must be visible before an upload fails.
  const [storage, setStorage] = useState({ data: null, loading: true, error: '' });
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const controller = new AbortController(); setStorage((s) => ({ ...s, loading: true, error: '' }));
    api.getStorage(controller.signal).then((data) => setStorage({ data, loading: false, error: '' }))
      .catch((e) => { if (e.name !== 'AbortError') setStorage((s) => ({ ...s, loading: false, error: e.message })); });
    return () => controller.abort();
  }, [reload]);

  const alert = storageAlert(storage.data);
  return <section className="staff-page dossiers-page">
    <header className="admin-heading staff-heading"><div><p className="section-kicker">Hệ thống / Hồ sơ lưu trữ</p><h1>Hồ sơ lưu trữ</h1><p className="heading-copy">Tìm hồ sơ theo năm, tháng và Số/No., xuất tài liệu và quản lý nơi lưu file.</p></div></header>
    {alert && <div className="dossier-alert" role="status"><Icon name="alert" size={18} /><span>{alert}</span>{tab !== 'storage' && <button type="button" className="outline-button small-button" onClick={() => changeTab('storage')}>Xem ổ lưu trữ</button>}</div>}
    <nav className="notification-tabs dossier-tabs" aria-label="Quản lý hồ sơ">
      {tabs.map(([key, label]) => <button key={key} type="button" aria-current={tab === key ? 'page' : undefined} className={tab === key ? 'active' : ''} onClick={() => changeTab(key)}>{label}</button>)}
    </nav>
    {tab === 'files' && <CatalogTab periods={periods} storage={storage.data} onExported={() => changeTab('exports')} />}
    {tab === 'exports' && <ExportsTab />}
    {tab === 'storage' && <StorageTab {...storage} onChange={(next) => setStorage({ data: next, loading: false, error: '' })} onReload={() => setReload((n) => n + 1)} />}
  </section>;
}
