/**
 * Số/No. dạng chuẩn CFC{YY}.{NNNN}: gõ "cfc26.00001" hay "CFC26.1" đều thành "CFC26.0001"
 * (4 chữ số, chỉ thêm chữ số sau 9999). Chuỗi không phải số CFC được trả lại nguyên (đã bỏ khoảng trắng đầu/cuối).
 */
export function normalizeRequestNumber(value) {
  const text = (value ?? '').trim();
  const match = /^cfc(\d{2})\.(\d{1,9})$/i.exec(text.replace(/\s+/g, ''));
  if (!match) return text;
  const sequence = Number(match[2]);
  return sequence < 1 ? text : `CFC${match[1]}.${String(sequence).padStart(4, '0')}`;
}
