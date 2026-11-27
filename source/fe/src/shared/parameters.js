// Chỉ tiêu của một mẫu được lưu thành một chuỗi "Cadimi (Cd), Auramine O (Vỏ), Auramine O (Ruột)". Một mẫu có thể có nhiều chỉ tiêu
// (theo Phiếu KQKN của LAB: một mã mẫu, mỗi chỉ tiêu một hàng), nên các màn hình tách chuỗi này thành từng dòng.
function canonical(name, matrix) {
  const key = name.toLowerCase().normalize('NFC').replace(/\s+/g, ' ').trim();
  if (/^cadi?mi?/.test(key) || key.startsWith('cadmium')) {
    // "Cadimi (Cd) Ruột + vỏ" là một chỉ tiêu đặc biệt (một dòng kết quả cho cả ruột và vỏ): giữ phần ghi thêm phía sau.
    const extra = name.trim().replace(/^cadi?mi?u?m?\s*(\(\s*cd\s*\))?/i, '').replace(/^[\s:\-–]+/, '').trim();
    return extra ? `Cadimi (Cd) ${extra}` : 'Cadimi (Cd)';
  }
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

// Phương pháp thử mặc định theo chỉ tiêu (danh mục phương pháp của LAB):
// Cadimi (Cd) → CFC-TN-17-2026; Auramine O (Vàng O, vỏ/ruột) → CFC-TN-16-2026.
export const CADMIUM_METHOD = 'CFC-TN-17-2026';
export const AURAMINE_METHOD = 'CFC-TN-16-2026';

/** Mã phương pháp gợi ý cho chuỗi chỉ tiêu của một mẫu, không trùng và theo thứ tự chỉ tiêu. Chỉ tiêu lạ không được gợi ý. */
export function methodCodesForParameters(value) {
  const codes = [];
  splitParameters(value).forEach((name) => {
    const code = name.startsWith('Cadimi') ? CADMIUM_METHOD : name.startsWith('Auramine') ? AURAMINE_METHOD : null;
    if (code && !codes.includes(code)) codes.push(code);
  });
  return codes;
}
