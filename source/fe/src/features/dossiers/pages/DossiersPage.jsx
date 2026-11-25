import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import * as api from '../api/dossierApi';
import { CatalogTab } from '../components/CatalogTab';
import { ExportsTab } from '../components/ExportsTab';
import { StorageTab } from '../components/StorageTab';
import './dossiers.css';

const tabs = [['files', 'Hồ sơ'], ['exports', 'Lượt xuất'], ['storage', 'Ổ lưu trữ']];

export function DossiersPage() {
  const [search, setSearch] = useSearchParams();
  const tab = tabs.some(([key]) => key === search.get('tab')) ? search.get('tab') : 'files';
  const changeTab = (next) => setSearch(next === 'files' ? {} : { tab: next });

  // Loaded for every tab: a disk that is filling up must be visible before an upload fails.
  const [storage, setStorage] = useState({ data: null, loading: true, error: '' });
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const controller = new AbortController(); setStorage((s) => ({ ...s, loading: true, error: '' }));
    api.getStorage(controller.signal).then((data) => setStorage({ data, loading: false, error: '' }))
      .catch((e) => { if (e.name !== 'AbortError') setStorage((s) => ({ ...s, loading: false, error: e.message })); });
    return () => controller.abort();
  }, [reload]);

  const { data } = storage;
  const receiving = data?.locations.filter((h) => (data.settings.locationId == null ? h.location.legacyKey : h.location.id === data.settings.locationId)) ?? [];
  const alert = receiving.some((h) => h.status === 'OFFLINE' || h.status === 'VOLUME_CHANGED') ? 'Nơi đang nhận upload chưa sẵn sàng, nhân viên có thể không tải được file.'
    : receiving.some((h) => h.warning) ? 'Nơi đang nhận upload sắp đầy. Nên chuẩn bị ổ mới và chuyển nơi lưu.'
      : data?.locations.some((h) => h.status === 'OFFLINE' && h.files > 0) ? 'Một ổ đang giữ hồ sơ cũ chưa truy cập được.'
        : data?.areas.some((a) => a.warning) ? 'Một vùng hệ thống của máy chủ sắp đầy.' : '';

  return <section className="admin-page dossier-page">
    <header className="admin-heading"><div><p className="section-kicker">Quản trị / Hồ sơ nội bộ</p><h1>Hồ sơ lưu trữ</h1><p className="heading-copy">Tìm hồ sơ, xuất tài liệu và quản lý nơi lưu file của phòng lab.</p></div></header>
    {alert && <div className="dossier-alert" role="status"><span>{alert}</span>{tab !== 'storage' && <button type="button" className="outline-button" onClick={() => changeTab('storage')}>Xem ổ lưu trữ</button>}</div>}
    <nav className="dossier-tabs" aria-label="Quản lý hồ sơ">
      {tabs.map(([key, label]) => <button key={key} type="button" aria-current={tab === key ? 'page' : undefined} className={tab === key ? 'current' : ''} onClick={() => changeTab(key)}>{label}</button>)}
    </nav>
    {tab === 'files' && <CatalogTab onExported={() => changeTab('exports')} />}
    {tab === 'exports' && <ExportsTab />}
    {tab === 'storage' && <StorageTab {...storage} onChange={(next) => setStorage({ data: next, loading: false, error: '' })} onReload={() => setReload((n) => n + 1)} />}
  </section>;
}
