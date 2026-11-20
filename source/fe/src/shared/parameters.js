// Chỉ tiêu của một mẫu được lưu thành một chuỗi "Cadimi (Cd), Auramine O (Vỏ), Auramine O (Ruột)". Một mẫu có thể có nhiều chỉ tiêu
// (theo Phiếu KQKN của LAB: một mã mẫu, mỗi chỉ tiêu một hàng), nên các màn hình tách chuỗi này thành từng dòng.
function canonical(name, matrix) {
  const key = name.toLowerCase().normalize('NFC').replace(/\s+/g, ' ').trim();
  if (/^cadi?mi?/.test(key) || key.startsWith('cadmium')) return 'Cadimi (Cd)';
  if (/^(vàng o|auramin)/.test(key)) {
    const part = `${key} ${(matrix || '').toLowerCase()}`;
    if (/vỏ/.test(part)) return 'Auramine O (Vỏ)';
    if (/ruột|múi/.test(part)) return 'Auramine O (Ruột)';
    return 'Auramine O';
  }
  return name.trim();
}

export function splitParameters(value, matrix) {
  const seen = new Set();
  const result = [];
  (value || '').split(/[,;\n]/).forEach((raw) => {
    const item = raw.trim().replace(/^Khác:\s*/i, '');
    if (!item) return;
    const name = canonical(item, matrix);
    const key = name.toLowerCase();
    if (seen.has(key)) return;
    seen.add(key); result.push(name);
  });
  return result;
}

export function splitMethods(value) {
  return (value || '').split(/[,;\n]/).map((item) => item.trim()).filter(Boolean);
}
