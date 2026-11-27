// Field forms store quantity as free text, sometimes including a unit. Preserve it without guessing a weight.
export function fieldQuantityForSample(field, name, sampleCount) {
  if (!field) return '';
  const items = field.items ?? [];
  const normalize = (value) => (value ?? '').trim().normalize('NFC').toLocaleLowerCase('vi-VN');
  const matches = items.filter((item) => normalize(item.sampleName) === normalize(name));
  if (matches.length === 1) return matches[0].quantity ?? '';
  if (matches.length > 1) return '';
  if (sampleCount === 1 && items.length === 1) return items[0].quantity ?? '';
  if (sampleCount === 1 && items.length === 0) return field.sampleQuantity ?? '';
  return '';
}

export const parameterOptions = ['Cadimi (Cd)', 'Auramine O (Vỏ)', 'Auramine O (Ruột)', 'Dư lượng thuốc BVTV'];
export const conditionOptions = ['Nguyên vẹn', 'Rò rỉ', 'Dập nát', 'Thiếu lượng'];
export const ABNORMAL = 'Dấu hiệu bất thường';
// Tình trạng mẫu là checklist theo mẫu Word, lưu thành chuỗi "Rò rỉ; Dập nát; Dấu hiệu bất thường: mốc".
export function parseCondition(value) {
  const tokens = (value || '').split(';').map((item) => item.trim()).filter(Boolean);
  const abnormal = tokens.find((item) => item.startsWith(ABNORMAL));
  return { picked: tokens.filter((item) => conditionOptions.includes(item)), abnormal: abnormal !== undefined, abnormalText: abnormal ? abnormal.slice(ABNORMAL.length).replace(/^:\s*/, '') : '' };
}
export function buildCondition({ picked, abnormal, abnormalText }) {
  return [...picked, ...(abnormal ? [`${ABNORMAL}:${abnormalText ? ` ${abnormalText}` : ''}`] : [])].join('; ');
}
