/**
 * Số/No. dạng chuẩn CFC{YY}.{NNNNN}: gõ "cfc26.0005" hay "CFC26.5" đều thành "CFC26.00005"
 * (5 chữ số, chỉ thêm chữ số sau 99999). Chuỗi không phải số CFC được trả lại nguyên (đã bỏ khoảng trắng đầu/cuối).
 */
export function normalizeRequestNumber(value) {
  const text = (value ?? '').trim();
  const match = /^cfc(\d{2})\.(\d{1,9})$/i.exec(text.replace(/\s+/g, ''));
  if (!match) return text;
  const sequence = Number(match[2]);
  return sequence < 1 ? text : `CFC${match[1]}.${String(sequence).padStart(5, '0')}`;
}
