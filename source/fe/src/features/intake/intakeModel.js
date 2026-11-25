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
