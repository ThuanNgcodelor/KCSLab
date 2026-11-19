export const CADMIUM = 'Cadimi (Cd)';
export const AURAMINE = 'Vàng O (Auramin O)';
export const SKIN = 'Vỏ';
export const FLESH = 'Ruột';

export const today = () => new Date().toISOString().slice(0, 10);
export const emptySample = () => ({ label: '', cadmium: { flesh: false, method: '' }, auramine: { skin: false, flesh: false, method: '' }, legacy: [] });
export const emptyRequest = () => ({ requestNumber: '', requestDate: today(), customerName: '', taxCode: '', contactPerson: '', customerAddress: '', customerPhone: '', customerEmail: '', sampleName: '', lotCode: '', variety: '', origin: '', harvestDate: '', packingDate: '', samplingMode: 'COMPANY', purpose: '', expectedDate: '', resultDeliveryMethod: '', serviceConditions: '', delayNotice: '', completed: false, samples: [emptySample()] });
export const emptyPlan = () => ({ formNumber: '', formDate: today(), representative: '', representativePhone: '', sampleCollector: '', collectorPhone: '', samplingStart: '', samplingEnd: '', samplingLocation: '', growingArea: '', packingFacility: '', vehicleNumber: '', trailerNumber: '', containerNumber: '', schemeNotes: '', transportConditions: '', sampleForms: '', lotSizeBand: '', transportModes: '' });
export const emptyFieldItem = () => ({ sampleName: '', samplingMethod: '', quantity: '', parameters: '', parametersOther: '' });
export const emptyField = () => ({ ...emptyPlan(), sampleCondition: '', storageCondition: '', storageTemperature: '', sampleSealNumber: '', sealPresent: true, samplingMethod: '', sampleQuantity: '', fieldNotes: '', items: [emptyFieldItem()] });
export const SHARED_KEYS = ['representative', 'representativePhone', 'sampleCollector', 'collectorPhone', 'samplingStart', 'samplingEnd', 'samplingLocation', 'growingArea', 'packingFacility', 'vehicleNumber', 'trailerNumber', 'containerNumber'];

// Danh mục cố định theo mẫu Word số 07 và 02.
export const SAMPLE_FORMS = [
  { code: 'WHOLE', label: 'Nguyên trái', parameters: 'Tách vỏ (V), ruột (R) tại phòng', mass: '01 trái/mẫu, ≥ 1,5 kg', note: 'Mặc định; chỉ tách tại hiện trường khi khách hàng yêu cầu' },
  { code: 'FLESH', label: 'Ruột (múi)', parameters: 'Cd; Auramine O (Vàng O)', mass: '≥ 300 g/mẫu', note: 'Đóng gói riêng, không chạm mặt ngoài vỏ' },
  { code: 'SKIN', label: 'Vỏ', parameters: 'Auramine O (Vàng O)', mass: '≥ 300 g/mẫu', note: 'Đóng gói riêng' },
];
export const LOT_BANDS = [
  { code: 'LE_1', label: '≤ 1 tấn', count: '3 trái', note: 'Mức tối thiểu khuyến nghị' },
  { code: '1_5', label: '> 1 – 5 tấn', count: '5 trái', note: 'Rải đều các vị trí' },
  { code: '5_20', label: '> 5 – 20 tấn (≈ 1 container)', count: '8 trái', note: 'Rải đều các pallet/hàng' },
  { code: 'GT_20', label: '> 20 tấn', count: 'Chia lô ≤ 20 tấn', note: 'Lấy mẫu theo từng lô con' },
];
export const TRANSPORT_MODES = [
  { code: 'WHOLE_AMBIENT', label: 'Nguyên trái', condition: '≤ 30 °C, tránh nắng, tránh va đập', deadline: 'Trong 48 giờ kể từ khi lấy mẫu' },
  { code: 'FRESH_COLD', label: 'Ruột, vỏ (tươi)', condition: 'Lạnh 2–8 °C', deadline: 'Trong 24 giờ' },
  { code: 'FROZEN', label: 'Ruột, vỏ (đông lạnh)', condition: '≤ −18 °C', deadline: 'Trong 24 giờ' },
];
export const FIELD_PARAMETERS = [{ code: 'VANG_O', label: 'Vàng O' }, { code: 'CD', label: 'Cd' }, { code: 'PB', label: 'Pb' }, { code: 'AS', label: 'As' }];
export const CONDITIONS = ['Nguyên vẹn', 'Hư hỏng'];

export const csvList = (value) => (value || '').split(',').map((item) => item.trim()).filter(Boolean);
export const toggleCsv = (value, code) => { const list = csvList(value); return (list.includes(code) ? list.filter((item) => item !== code) : [...list, code]).join(','); };
export const parseCondition = (value) => { if (!value) return { choice: '', other: '' }; if (CONDITIONS.includes(value)) return { choice: value, other: '' }; return { choice: 'Khác', other: value.replace(/^Khác:?\s*/, '') }; };
export const requestParameterCodes = (sample) => [sample.auramine.skin || sample.auramine.flesh ? 'VANG_O' : '', sample.cadmium.flesh ? 'CD' : ''].filter(Boolean).join(',');

// Phiếu 07 lấy sẵn từ Phiếu 1; trường nào Phiếu 1 không có thì để trống cho nhân viên nhập.
export const planSeed = (form) => ({ ...emptyPlan(), representative: form.contactPerson || '', representativePhone: form.customerPhone || '', growingArea: form.origin || '', sampleForms: 'WHOLE' });
// Phiếu 02 lấy sẵn từ Phiếu 07 và Phiếu 1; thông tin thực tế (niêm phong, số lượng, tình trạng) để trống.
export const fieldSeed = (plan, form) => {
  const seed = emptyField(); SHARED_KEYS.forEach((key) => { seed[key] = plan[key] ?? ''; });
  seed.items = form.samples.map((sample, index) => ({ ...emptyFieldItem(), sampleName: sample.label.trim() || `Mẫu ${index + 1}`, parameters: requestParameterCodes(sample) }));
  if (!seed.items.length) seed.items = [emptyFieldItem()];
  return seed;
};

// Mỗi tổ hợp (mẫu, chỉ tiêu, nền mẫu) được tick là một dòng test_request_items; schema không đổi.
export function buildItems(samples) {
  const items = [];
  samples.forEach((sample, index) => {
    const label = sample.label.trim() || `Mẫu ${index + 1}`;
    const add = (parameters, matrix, testMethod) => items.push({ sampleNameOrId: label, parameters, matrix, testMethod: testMethod.trim() });
    if (sample.cadmium.flesh) add(CADMIUM, FLESH, sample.cadmium.method);
    if (sample.auramine.skin) add(AURAMINE, SKIN, sample.auramine.method);
    if (sample.auramine.flesh) add(AURAMINE, FLESH, sample.auramine.method);
    sample.legacy.forEach((row) => items.push({ sampleNameOrId: label, parameters: row.parameters, matrix: row.matrix ?? '', testMethod: row.testMethod ?? '' }));
  });
  return items;
}
export function samplesFrom(items) {
  const groups = new Map();
  (items ?? []).forEach((item) => {
    if (!groups.has(item.sampleNameOrId)) groups.set(item.sampleNameOrId, { ...emptySample(), label: item.sampleNameOrId, cadmium: { flesh: false, method: '' }, auramine: { skin: false, flesh: false, method: '' }, legacy: [] });
    const sample = groups.get(item.sampleNameOrId); const method = item.testMethod ?? '';
    if (item.parameters === CADMIUM && item.matrix === FLESH) { sample.cadmium.flesh = true; sample.cadmium.method = method; }
    else if (item.parameters === AURAMINE && item.matrix === SKIN) { sample.auramine.skin = true; sample.auramine.method = method; }
    else if (item.parameters === AURAMINE && item.matrix === FLESH) { sample.auramine.flesh = true; sample.auramine.method = method; }
    else sample.legacy.push({ parameters: item.parameters, matrix: item.matrix ?? '', testMethod: method });
  });
  const samples = [...groups.values()];
  return samples.length ? samples : [emptySample()];
}
export function requestPayload(form) { const { samples, ...rest } = form; return { ...rest, harvestDate: form.harvestDate || null, packingDate: form.packingDate || null, expectedDate: form.expectedDate || null, items: buildItems(samples) }; }
export function timePayload(data, requestNumber) { return { ...data, formNumber: requestNumber, samplingStart: data.samplingStart ? new Date(data.samplingStart).toISOString() : null, samplingEnd: data.samplingEnd ? new Date(data.samplingEnd).toISOString() : null }; }
export function fieldPayload(field, requestNumber) {
  const items = field.items.filter((item) => item.sampleName.trim() || item.samplingMethod.trim() || item.quantity.trim() || item.parameters || item.parametersOther.trim()).map((item, index) => ({ ...item, sampleName: item.sampleName.trim() || `Mẫu ${index + 1}` }));
  const join = (key, max) => [...new Set(items.map((item) => item[key].trim()).filter(Boolean))].join('; ').slice(0, max);
  // samplingMethod/sampleQuantity cũ vẫn được điền để Phiếu 3 và các màn đang dùng không mất dữ liệu.
  return { ...timePayload(field, requestNumber), items, samplingMethod: join('samplingMethod', 255), sampleQuantity: join('quantity', 120) };
}
export const blanks = (data) => Object.fromEntries(Object.entries(data).map(([key, value]) => [key, value ?? '']));
export function fieldFrom(data) { const items = (data.items ?? []).map((item) => blanks(item)); return { ...emptyField(), ...blanks(formFrom(data)), sealPresent: data.sealPresent, items: items.length ? items : [emptyFieldItem()] }; }
export function localDateTime(value) { if (!value) return ''; const d = new Date(value); const pad = (n) => String(n).padStart(2, '0'); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`; }
export function formFrom(data) { return { ...emptyPlan(), ...blanks(data), samplingStart: localDateTime(data.samplingStart), samplingEnd: localDateTime(data.samplingEnd) }; }
export function requestFrom(data) { const { items, samplingPlan, fieldSampling, ...rest } = data; return { ...emptyRequest(), ...rest, harvestDate: data.harvestDate ?? '', packingDate: data.packingDate ?? '', expectedDate: data.expectedDate ?? '', completed: data.status === 'COMPLETED', samples: samplesFrom(items) }; }

// Danh sách tick của Phiếu 1 (mục đích, thông báo khi chậm) lưu thành chuỗi "A; B; Khác: …" nên không cần đổi schema.
export const PURPOSES = ['Kiểm soát nội bộ', 'Đánh giá lô hàng', 'Yêu cầu của khách hàng'];
export const DELAY_CHANNELS = ['Điện thoại', 'Email'];
export function parseChecklist(value, options) {
  const tokens = (value || '').split(';').map((item) => item.trim()).filter(Boolean);
  const selected = tokens.filter((token) => options.includes(token));
  const rest = tokens.filter((token) => !options.includes(token));
  const other = rest.map((token) => token.replace(/^Khác:?\s*/, '')).join('; ');
  return { selected, otherEnabled: rest.length > 0, other };
}
export function buildChecklist(selected, otherEnabled, other) {
  return [...selected, ...(otherEnabled ? [other.trim() ? `Khác: ${other.trim()}` : 'Khác'] : [])].join('; ');
}
