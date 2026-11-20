// Chỉ tiêu của một mẫu được lưu thành một chuỗi "Cadimi (Cd), Auramine O (Vàng ô)". Một mẫu có thể có nhiều chỉ tiêu
// (theo Phiếu KQKN của LAB: một mã mẫu, mỗi chỉ tiêu một hàng), nên các màn hình tách chuỗi này thành từng dòng.
function canonical(name) {
  const key = name.toLowerCase().normalize('NFC').replace(/\s+/g, ' ').trim();
  if (/^cadi?mi?/.test(key) || key.startsWith('cadmium')) return 'Cadimi (Cd)';
  if (/^(vàng o|auramin)/.test(key)) return 'Auramine O (Vàng ô)';
  return name.trim();
}

export function splitParameters(value) {
  const seen = new Set();
  const result = [];
  (value || '').split(/[,;\n]/).forEach((raw) => {
    const item = raw.trim().replace(/^Khác:\s*/i, '');
    if (!item) return;
    const name = canonical(item);
    const key = name.toLowerCase();
    if (seen.has(key)) return;
    seen.add(key); result.push(name);
  });
  return result;
}

export function splitMethods(value) {
  return (value || '').split(/[,;\n]/).map((item) => item.trim()).filter(Boolean);
}
